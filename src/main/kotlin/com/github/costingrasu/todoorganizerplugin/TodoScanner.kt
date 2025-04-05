package com.github.costingrasu.todoorganizerplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.openapi.editor.Editor

data class TodoItem(val tag: String, val text: String, val lineNumber: Int, val fileName: String, val offset: Int)

object TodoScanner {

    // Finds all TODO, FIXME, BUG comments in the opened file
    fun findTodosInOpenedFile(project: Project, editor: Editor): List<TodoItem> {
        val todoItems = mutableListOf<TodoItem>()
        val psiManager = PsiManager.getInstance(project)

        // Get the virtual file from the editor and retrieve the PsiFile
        val virtualFile = editor.virtualFile

        // Use Application.runReadAction to ensure PSI access happens within a read action
        ApplicationManager.getApplication().runReadAction {
            val psiFile = psiManager.findFile(virtualFile) ?: return@runReadAction

            // Traverse the PSI file using a visitor
            psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)

                    if (element is PsiComment) {
                        val text = element.text.trim()

                        // Only process valid comments: //TODO, //FIXME, //BUG or #TODO, #FIXME, #BUG
                        if (text.startsWith("//TODO") || text.startsWith("//FIXME") || text.startsWith("//BUG") ||
                            text.startsWith("#TODO") || text.startsWith("#FIXME") || text.startsWith("#BUG")) {

                            // Determine the tag
                            val tag = when {
                                text.startsWith("//TODO") || text.startsWith("#TODO") -> "TODO"
                                text.startsWith("//FIXME") || text.startsWith("#FIXME") -> "FIXME"
                                text.startsWith("//BUG") || text.startsWith("#BUG") -> "BUG"
                                else -> "UNKNOWN"  // Fallback in case of some other comment
                            }

                            // Calculate the line number using the text offset
                            val document = editor.document
                            val lineNumber = document.getLineNumber(element.textOffset)

                            // Create a new TodoItem
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

        // Debugging: Log the number of items found
        println("Found ${todoItems.size} TODO items.")

        return todoItems
    }
}
