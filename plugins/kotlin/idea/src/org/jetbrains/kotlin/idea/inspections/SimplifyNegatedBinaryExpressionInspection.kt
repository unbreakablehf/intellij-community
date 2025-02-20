// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class SimplifyNegatedBinaryExpressionInspection : AbstractApplicabilityBasedInspection<KtPrefixExpression>(KtPrefixExpression::class.java) {

    override fun inspectionHighlightRangeInElement(element: KtPrefixExpression) = element.operationReference.textRangeIn(element)

    override fun inspectionText(element: KtPrefixExpression) = KotlinBundle.message("negated.operation.should.be.simplified")

    override val defaultFixText get() = KotlinBundle.message("simplify.negated.operation")

    override fun fixText(element: KtPrefixExpression): String {
        val expression = KtPsiUtil.deparenthesize(element.baseExpression) as? KtOperationExpression ?: return defaultFixText
        val operation = expression.operationReference.getReferencedNameElementType() as? KtSingleValueToken ?: return defaultFixText
        val negatedOperation = operation.negate() ?: return defaultFixText
        return KotlinBundle.message("replace.negated.0.operation.with.1", operation.value, negatedOperation.value)
    }

    override fun isApplicable(element: KtPrefixExpression): Boolean {
        return element.canBeSimplified()
    }

    override fun applyTo(element: KtPrefixExpression, project: Project, editor: Editor?) {
        element.simplify()
    }

    companion object {
        fun simplifyNegatedBinaryExpressionIfNeeded(expression: KtPrefixExpression) {
            if (expression.canBeSimplified()) expression.simplify()
        }

        private fun KtPrefixExpression.canBeSimplified(): Boolean {
            if (operationToken != KtTokens.EXCL) return false

            val expression = KtPsiUtil.deparenthesize(baseExpression) as? KtOperationExpression ?: return false
            when (expression) {
                is KtIsExpression -> if (expression.typeReference == null) return false
                is KtBinaryExpression -> if (expression.left == null || expression.right == null) return false
                else -> return false
            }

            return (expression.operationReference.getReferencedNameElementType() as? KtSingleValueToken)?.negate() != null
        }

        private fun KtPrefixExpression.simplify() {
            val expression = KtPsiUtil.deparenthesize(baseExpression) ?: return
            val operation =
                (expression as KtOperationExpression).operationReference.getReferencedNameElementType().negate()?.value ?: return

            val psiFactory = KtPsiFactory(expression)
            val newExpression = when (expression) {
                is KtIsExpression ->
                    psiFactory.createExpressionByPattern("$0 $1 $2", expression.leftHandSide, operation, expression.typeReference!!)
                is KtBinaryExpression ->
                    psiFactory.createExpressionByPattern("$0 $1 $2", expression.left ?: return, operation, expression.right ?: return)
                else ->
                    throw IllegalArgumentException()
            }
            replace(newExpression)
        }

        private fun IElementType.negate(): KtSingleValueToken? = when (this) {
            KtTokens.IN_KEYWORD -> KtTokens.NOT_IN
            KtTokens.NOT_IN -> KtTokens.IN_KEYWORD

            KtTokens.IS_KEYWORD -> KtTokens.NOT_IS
            KtTokens.NOT_IS -> KtTokens.IS_KEYWORD

            KtTokens.EQEQ -> KtTokens.EXCLEQ
            KtTokens.EXCLEQ -> KtTokens.EQEQ

            KtTokens.LT -> KtTokens.GTEQ
            KtTokens.GTEQ -> KtTokens.LT

            KtTokens.GT -> KtTokens.LTEQ
            KtTokens.LTEQ -> KtTokens.GT

            else -> null
        }
    }
}