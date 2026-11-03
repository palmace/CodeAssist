package com.tyron.kotlin.completion

import com.tyron.kotlin_completion.util.PsiUtils
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import java.io.File

/**
 * KotlinDefinitionResolver v1.2: "The Surveyor".
 * v1.2: Ahora calcula línea y columna de la definición para saltos de precisión.
 */
class KotlinDefinitionResolver(private val environment: KotlinEnvironment) {

    data class DefinitionLocation(val file: File, val line: Int, val column: Int)

    fun resolveDefinition(file: KotlinFile, line: Int, character: Int): DefinitionLocation? {
        val element = file.elementAt(line, character) ?: return null
        
        val ktFiles = listOf(file.kotlinFile)
        val analysis = environment.analysisOf(ktFiles)
        val bindingContext = analysis.analysisResult.bindingContext
        
        val ktReference = element as? KtReferenceExpression 
            ?: PsiUtils.findParent(element, KtReferenceExpression::class.java)
            
        val descriptor = if (ktReference != null) {
            bindingContext.get(BindingContext.REFERENCE_TARGET, ktReference)
        } else {
            val ktElement = element as? KtElement 
                ?: PsiUtils.findParent(element, KtElement::class.java)
            ktElement?.let { bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, it) }
        }
            
        if (descriptor != null) {
            val sourceElement = descriptor.findPsi()
            if (sourceElement != null) {
                val containingFile = sourceElement.containingFile
                val virtualFile = containingFile.virtualFile
                if (virtualFile != null) {
                    // Calculamos coordenadas
                    val project = containingFile.project
                    val document = PsiDocumentManager.getInstance(project).getDocument(containingFile)
                    
                    var targetLine = 0
                    var targetCol = 0
                    
                    if (document != null) {
                        targetLine = document.getLineNumber(sourceElement.textOffset)
                        targetCol = sourceElement.textOffset - document.getLineStartOffset(targetLine)
                    }

                    return DefinitionLocation(
                        File(virtualFile.path),
                        targetLine,
                        targetCol
                    )
                }
            }
        }
        
        return null
    }
}
