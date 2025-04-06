package com.github.costingrasu.todoorganizerplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.openapi.editor.Editor

data class TodoItem(val tag: String, val text: String, val lineNumber: Int, val fileName: String, val offset: Int)

object TodoScanner {

    fun findTodosInOpenedFile(project: Project, editor: Editor): List<TodoItem> {
        val todoItems = mutableListOf<TodoItem>()
        val psiManager = PsiManager.getInstance(project)

        val virtualFile = editor.virtualFile

        ApplicationManager.getApplication().runReadAction {
            val psiFile = psiManager.findFile(virtualFile) ?: return@runReadAction

            psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)

                    if (element is PsiComment) {
                        val text = element.text.trim()

                        if (text.startsWith("//TODO") || text.startsWith("//FIXME") || text.startsWith("//BUG") ||
                            text.startsWith("#TODO") || text.startsWith("#FIXME") || text.startsWith("#BUG")) {

                            val tag = when {
                                text.startsWith("//TODO") || text.startsWith("#TODO") -> "TODO"
                                text.startsWith("//FIXME") || text.startsWith("#FIXME") -> "FIXME"
                                text.startsWith("//BUG") || text.startsWith("#BUG") -> "BUG"
                                else -> "UNKNOWN"
                            }

                            val document = editor.document
                            val lineNumber = document.getLineNumber(element.textOffset)

                            val item = TodoItem(
                                tag = tag,
                                text = element.text,
                                lineNumber = lineNumber,
                                fileName = psiFile.name,
                                offset = element.textOffset
                            )

                            todoItems.add(item)
                        }
                    }
                }
            })
        }

        return todoItems
    }
}
