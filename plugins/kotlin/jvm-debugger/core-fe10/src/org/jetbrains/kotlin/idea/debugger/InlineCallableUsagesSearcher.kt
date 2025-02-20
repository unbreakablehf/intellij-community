// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.xdebugger.impl.XDebuggerManagerImpl
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.base.projectStructure.RootKindFilter
import org.jetbrains.kotlin.idea.base.projectStructure.matches
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.debugger.DebuggerClassNameProvider.Companion.getRelevantElement
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.ComputedClassNames
import org.jetbrains.kotlin.idea.search.isImportUsage
import org.jetbrains.kotlin.idea.base.projectStructure.scope.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class InlineCallableUsagesSearcher(val project: Project, val searchScope: GlobalSearchScope) {
    fun findInlinedCalls(
        declaration: KtDeclaration,
        alreadyVisited: Set<PsiElement>,
        transformer: (PsiElement, Set<PsiElement>) -> ComputedClassNames
    ): ComputedClassNames {
        if (!checkIfInline(declaration)) {
            return ComputedClassNames.EMPTY
        } else {
            val searchResult = hashSetOf<PsiElement>()
            val declarationName = runReadAction { declaration.name ?: "<error>" }

            val task = Runnable {
                for (reference in ReferencesSearch.search(declaration, getScopeForInlineDeclarationUsages(declaration))) {
                    ProgressManager.checkCanceled()
                    processReference(declaration, reference, alreadyVisited)?.let { searchResult += it }
                }
            }

            var isSuccess = true
            val applicationEx = ApplicationManagerEx.getApplicationEx()
            if (applicationEx.isDispatchThread) {
                isSuccess = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    task,
                    KotlinDebuggerCoreBundle.message("find.inline.calls.task.compute.names", declarationName),
                    true,
                    project
                )
            } else {
                try {
                    ProgressManager.getInstance().runProcess(task, EmptyProgressIndicator())
                } catch (e: InterruptedException) {
                    isSuccess = false
                }
            }

            if (!isSuccess) {
                XDebuggerManagerImpl.getNotificationGroup().createNotification(
                    KotlinDebuggerCoreBundle.message("find.inline.calls.task.cancelled", declarationName),
                    MessageType.WARNING
                ).notify(project)
            }

            val newAlreadyVisited = HashSet<PsiElement>().apply {
                addAll(alreadyVisited)
                addAll(searchResult)
                add(declaration)
            }

            val results = searchResult.map { transformer(it, newAlreadyVisited) }
            return ComputedClassNames(results.flatMap { it.classNames }, shouldBeCached = results.all { it.shouldBeCached })
        }
    }

    private fun processReference(declaration: KtDeclaration, reference: PsiReference, alreadyVisited: Set<PsiElement>): PsiElement? {
        if (runReadAction { reference.isImportUsage() }) {
            return null
        }

        val usage = (reference.element as? KtElement)?.let(::getRelevantElement) ?: return null
        val shouldAnalyze = runReadAction { !declaration.isAncestor(usage) && usage !in alreadyVisited }
        return if (shouldAnalyze) usage else null
    }

    private fun checkIfInline(declaration: KtDeclaration) =
        ReadAction.nonBlocking<Boolean> {
            ProgressManager.checkCanceled()
            checkIfInlineInReadAction(declaration)
        }.executeSynchronously()

    private fun checkIfInlineInReadAction(declaration: KtDeclaration): Boolean {
        if (DumbService.isDumb(project)) {
            return false
        }

        val bindingContext = declaration.analyze(BodyResolveMode.PARTIAL)
        val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration) ?: return false
        return when (descriptor) {
            is FunctionDescriptor -> InlineUtil.isInline(descriptor)
            is PropertyDescriptor -> InlineUtil.hasInlineAccessors(descriptor)
            else -> false
        }
    }

    private fun getScopeForInlineDeclarationUsages(inlineDeclaration: KtDeclaration): GlobalSearchScope {
        val virtualFile = runReadAction { inlineDeclaration.containingFile.virtualFile }
        return if (virtualFile != null && RootKindFilter.libraryFiles.matches(project, virtualFile)) {
            searchScope.uniteWith(
                KotlinSourceFilterScope.librarySources(GlobalSearchScope.allScope(project), project)
            )
        } else {
            searchScope
        }
    }
}