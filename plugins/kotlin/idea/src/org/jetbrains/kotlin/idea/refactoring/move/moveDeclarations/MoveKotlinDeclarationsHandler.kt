// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.*
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.MoveHandlerDelegate
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesImpl
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui.KotlinAwareMoveFilesOrDirectoriesDialog
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui.KotlinSelectNestedClassRefactoringDialog
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui.MoveKotlinNestedClassesDialog
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui.MoveKotlinTopLevelDeclarationsDialog
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.idea.util.isEffectivelyActual
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelInFileOrScript

private val defaultHandlerActions = object : MoveKotlinDeclarationsHandlerActions {

    override fun showErrorHint(
        project: Project,
        editor: Editor?,
        @NlsContexts.DialogMessage message: String,
        @NlsContexts.DialogTitle title: String,
        helpId: String?
    ) {
        CommonRefactoringUtil.showErrorHint(project, editor, message, title, helpId)
    }

    override fun invokeMoveKotlinNestedClassesRefactoring(
        project: Project,
        elementsToMove: List<KtClassOrObject>,
        originalClass: KtClassOrObject,
        targetClass: KtClassOrObject,
        moveCallback: MoveCallback?
    ) = MoveKotlinNestedClassesDialog(project, elementsToMove, originalClass, targetClass, null, moveCallback).show()

    override fun invokeMoveKotlinTopLevelDeclarationsRefactoring(
        project: Project,
        elementsToMove: Set<KtNamedDeclaration>,
        targetPackageName: String,
        targetDirectory: PsiDirectory?,
        targetFile: KtFile?,
        freezeTargets: Boolean,
        moveToPackage: Boolean,
        moveCallback: MoveCallback?
    ) = MoveKotlinTopLevelDeclarationsDialog(
        project,
        elementsToMove,
        targetPackageName,
        targetDirectory,
        targetFile,
        freezeTargets,
        moveToPackage,
        moveCallback
    ).show()

    override fun invokeKotlinSelectNestedClassChooser(nestedClass: KtClassOrObject, targetContainer: PsiElement?) =
        KotlinSelectNestedClassRefactoringDialog.chooseNestedClassRefactoring(nestedClass, targetContainer)

    override fun invokeKotlinAwareMoveFilesOrDirectoriesRefactoring(
        project: Project,
        initialDirectory: PsiDirectory?,
        elements: List<PsiFileSystemItem>,
        moveCallback: MoveCallback?
    ) = KotlinAwareMoveFilesOrDirectoriesDialog(project, initialDirectory, elements, moveCallback).show()
}

class MoveKotlinDeclarationsHandler internal constructor(private val handlerActions: MoveKotlinDeclarationsHandlerActions) :
    MoveHandlerDelegate() {

    private var freezeTargets: Boolean = true

    constructor() : this(defaultHandlerActions)

    constructor(freezeTargets: Boolean) : this() {
        this.freezeTargets = freezeTargets
    }

    private fun getUniqueContainer(elements: Array<out PsiElement>): PsiElement? {
        val allTopLevel = elements.all { isTopLevelInFileOrScript(it) }

        val getContainer: (PsiElement) -> PsiElement? =
            { e ->
                if (allTopLevel) {
                    e.containingFile?.parent
                } else {
                    when (e) {
                        is KtNamedDeclaration -> e.containingClassOrObject ?: e.parent
                        is KtFile -> e.parent
                        else -> null
                    }
                }
            }

        return elements.mapNotNullTo(LinkedHashSet(), getContainer).singleOrNull()
    }

    private fun KtNamedDeclaration.canMove() = if (this is KtClassOrObject) !isLocal else isTopLevelInFileOrScript(this)

    private fun doMoveWithCheck(
        project: Project, elements: Array<out PsiElement>, targetContainer: PsiElement?, callback: MoveCallback?, editor: Editor?
    ): Boolean {
        if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, elements.toList(), true)) return false

        val container = getUniqueContainer(elements)
        if (container == null) {
            handlerActions.showErrorHint(
                project,
                editor,
                KotlinBundle.message("text.all.declarations.must.belong.to.the.same.directory.or.class"),
                MOVE_DECLARATIONS,
                null
            )
            return false
        }

        val elementsToSearch = elements.flatMapTo(LinkedHashSet<KtNamedDeclaration>()) {
            when (it) {
                is KtNamedDeclaration -> listOf(it)
                is KtFile -> it.declarations.filterIsInstance<KtNamedDeclaration>()
                else -> emptyList()
            }
        }

        // todo: allow moving companion object
        if (elementsToSearch.any { it is KtObjectDeclaration && it.isCompanion() }) {
            val message = RefactoringBundle.getCannotRefactorMessage(
                KotlinBundle.message("text.move.declaration.no.support.for.companion.objects")
            )
            handlerActions.showErrorHint(project, editor, message, MOVE_DECLARATIONS, null)
            return true
        }

        if (elementsToSearch.any { !it.canMove() }) {
            val message =
                RefactoringBundle.getCannotRefactorMessage(KotlinBundle.message("text.move.declaration.supports.only.top.levels.and.nested.classes"))
            handlerActions.showErrorHint(project, editor, message, MOVE_DECLARATIONS, null)
            return true
        }

        if (elementsToSearch.any { it is KtEnumEntry }) {
            val message = RefactoringBundle.getCannotRefactorMessage(KotlinBundle.message("text.move.declaration.no.support.for.enums"))
            handlerActions.showErrorHint(project, editor, message, MOVE_DECLARATIONS, null)
            return true
        }

        if (elements.all { it is KtFile }) {
            val ktFileElements = elements.map { it as KtFile }
            val initialTargetElement = when {
                targetContainer is PsiPackage || targetContainer is PsiDirectory -> targetContainer
                container is PsiPackage || container is PsiDirectory -> container
                else -> null
            }
            val initialTargetDirectory = MoveFilesOrDirectoriesUtil.resolveToDirectory(project, initialTargetElement)

            if (!isUnitTestMode() &&
                elementsToSearch.any { it.isExpectDeclaration() || it.isEffectivelyActual() }
            ) {
                val message =
                    RefactoringBundle.getCannotRefactorMessage(KotlinBundle.message("text.move.declaration.proceed.move.without.mpp.counterparts.text"))
                val title =
                    KotlinBundle.message("text.move.declaration.proceed.move.without.mpp.counterparts.title")
                val proceedWithIncompleteRefactoring = Messages.showYesNoDialog(project, message, title, Messages.getWarningIcon())
                if (proceedWithIncompleteRefactoring != Messages.YES) return true
            }

            handlerActions.invokeKotlinAwareMoveFilesOrDirectoriesRefactoring(
                project, initialTargetDirectory, ktFileElements, callback
            )

            return true
        }

        when (container) {
            is PsiDirectory, is PsiPackage, is KtFile -> {
                val targetPackageName = MoveClassesOrPackagesImpl.getInitialTargetPackageName(targetContainer, elements)
                val targetDirectory = if (targetContainer != null) {
                    MoveClassesOrPackagesImpl.getInitialTargetDirectory(targetContainer, elements)
                } else null
                val targetFile = targetContainer as? KtFile
                val moveToPackage = targetContainer !is KtFile

                handlerActions.invokeMoveKotlinTopLevelDeclarationsRefactoring(
                    project,
                    elementsToSearch,
                    targetPackageName,
                    targetDirectory,
                    targetFile,
                    freezeTargets,
                    moveToPackage,
                    callback
                )
            }

            is KtClassOrObject -> {
                if (elementsToSearch.size > 1) {
                    // todo: allow moving multiple classes to upper level
                    if (targetContainer !is KtClassOrObject) {
                        val message =
                            RefactoringBundle.getCannotRefactorMessage(
                                KotlinBundle.message("text.moving.multiple.nested.classes.to.top.level.not.supported")
                            )
                        handlerActions.showErrorHint(project, editor, message, MOVE_DECLARATIONS, null)
                        return true
                    }
                  handlerActions.invokeMoveKotlinNestedClassesRefactoring(
                      project,
                      elementsToSearch.filterIsInstance<KtClassOrObject>(),
                      container,
                      targetContainer,
                      callback
                  )
                    return true
                }
                handlerActions.invokeKotlinSelectNestedClassChooser(
                    elementsToSearch.first() as KtClassOrObject,
                    targetContainer
                )
            }

            else -> throw AssertionError("Unexpected container: ${container.getElementTextWithContext()}")
        }

        return true
    }

    private fun canMove(elements: Array<out PsiElement>, targetContainer: PsiElement?, editorMode: Boolean): Boolean {
        if (targetContainer != null && !isValidTarget(targetContainer, elements)) return false

        val container = getUniqueContainer(elements) ?: return false

        if (container is KtClassOrObject && targetContainer != null && targetContainer !is KtClassOrObject && elements.size > 1) {
            return false
        }

        return elements.all { e ->
            when {
                e is KtClass || e is KtObjectDeclaration && !e
                    .isObjectLiteral() || e is KtNamedFunction || e is KtProperty || e is KtTypeAlias ->
                    (editorMode || (e as KtNamedDeclaration).canMove()) && e.canRefactor()
                e is KtFile ->
                    e.declarations.any { it is KtNamedDeclaration } && e.canRefactor()
                else ->
                    false
            }
        }
    }

    private fun tryToMoveImpl(
        element: PsiElement, project: Project, dataContext: DataContext?, reference: PsiReference?, editor: Editor?
    ): Boolean {
        val elementsToMove = element.unwrapped?.let { arrayOf(it) } ?: PsiElement.EMPTY_ARRAY
        val targetContainer = dataContext?.let { context -> LangDataKeys.TARGET_PSI_ELEMENT.getData(context) }
        return canMove(elementsToMove, targetContainer, true) && doMoveWithCheck(project, elementsToMove, targetContainer, null, editor)
    }

    private fun recursivelyTryToMove(
        element: PsiElement, project: Project, dataContext: DataContext?, reference: PsiReference?, editor: Editor?
    ): Boolean {
        return tryToMoveImpl(element, project, dataContext, reference, editor)
                || element.parent?.let { recursivelyTryToMove(it, project, dataContext, reference, editor) } ?: false
    }

    override fun canMove(elements: Array<out PsiElement>, targetContainer: PsiElement?, reference: PsiReference?): Boolean {
        return canMove(elements, targetContainer, false)
    }

    override fun isValidTarget(psiElement: PsiElement?, sources: Array<out PsiElement>): Boolean {
        return psiElement is PsiPackage
                || (psiElement is PsiDirectory && psiElement.getPackage() != null)
                || psiElement is KtFile
                || (psiElement is KtClassOrObject
                && !(psiElement.hasModifier(KtTokens.ANNOTATION_KEYWORD))
                && !sources.any { it.parent is KtFile })
    }

    override fun doMove(project: Project, elements: Array<out PsiElement>, targetContainer: PsiElement?, callback: MoveCallback?) {
        doMoveWithCheck(project, elements, targetContainer, callback, null)
    }

    override fun tryToMove(
        element: PsiElement, project: Project, dataContext: DataContext?, reference: PsiReference?, editor: Editor?
    ): Boolean {
        if (element is PsiWhiteSpace && element.textOffset > 0) {
            val prevElement = element.containingFile.findElementAt(element.textOffset - 1)
            return prevElement != null
                    && recursivelyTryToMove(prevElement, project, dataContext, reference, editor)
        }
        return tryToMoveImpl(element, project, dataContext, reference, editor)
    }
}

private val MOVE_DECLARATIONS: String
    @Nls
    get() = KotlinBundle.message("text.move.declarations")
