/*******************************************************************************
 * Copyright 2000-2022 JetBrains s.r.o. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.jetbrains.packagesearch.intellij.plugin.extensibility

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtil
import com.jetbrains.packagesearch.intellij.plugin.PackageSearchBundle
import com.jetbrains.packagesearch.intellij.plugin.util.packageSearchProjectService

abstract class PackageVersionRangeInspection : AbstractPackageUpdateInspectionCheck() {

    override fun ProblemsHolder.checkFile(file: PsiFile, fileModule: Module) {
        file.project.packageSearchProjectService.dependenciesByModuleStateFlow.value
            .entries
            .find { it.key.nativeModule == fileModule }
            ?.value
            ?.filter { it.dependency.coordinates.version?.let { isIvyRange(it) } ?: false }
            ?.mapNotNull { coordinates ->
                runCatching {
                    coordinates.declarationIndexes
                        ?.let { selectPsiElementIndex(it) }
                        ?.let { PsiUtil.getElementAtOffset(file, it) }
                }.getOrNull()
                    ?.let { coordinates to it }
            }
            ?.forEach { (coordinatesWithResolvedVersion, psiElement) ->

                val message = coordinatesWithResolvedVersion.dependency.coordinates.version
                    ?.let { PackageSearchBundle.message("packagesearch.inspection.upgrade.range.withVersion", it) }
                    ?: PackageSearchBundle.message("packagesearch.inspection.upgrade.range")

                registerProblem(psiElement, message, ProblemHighlightType.WEAK_WARNING)
            }
    }

    private fun isIvyRange(version: String): Boolean {
        // See https://ant.apache.org/ivy/history/2.1.0/ivyfile/dependency.html
        val normalizedVersion = version.trimEnd()
        if (normalizedVersion.endsWith('+')) return true

        if (normalizedVersion.startsWith("latest.")) return true

        val startsWithParenthesisOrBrackets = normalizedVersion.startsWith('(') || normalizedVersion.startsWith('[')
        val endsWithParenthesisOrBrackets = normalizedVersion.endsWith(')') || normalizedVersion.endsWith(']')
        if (startsWithParenthesisOrBrackets && endsWithParenthesisOrBrackets) return true

        return false
    }
}
