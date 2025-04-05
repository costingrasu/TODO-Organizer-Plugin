package com.github.costingrasu.todoorganizerplugin

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.FileTypeIndex
import com.intellij.openapi.vfs.VirtualFile

class TodoScanner {

    fun findTodos(project: Project): List<TodoItem> {
        val psiManager = PsiManager.getInstance(project)
        val todoItems = mutableListOf<TodoItem>()
        var currentParent: TodoItem? = null  // Track the current parent comment (TODO, FIXME, BUG)

        // Search through all files in the project
        FileTypeIndex.getFiles(PlainTextFileType.INSTANCE, GlobalSearchScope.projectScope(project)).forEach { virtualFile ->
            val psiFile = psiManager.findFile(virtualFile) ?: return@forEach
            psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)
                    if (element is PsiComment) {
                        val text = element.text

                        // Only process valid comments: //TODO, //FIXME, //BUG
                        if (text.startsWith("//TODO") || text.startsWith("//FIXME") || text.startsWith("//BUG")) {
                            // Determine the tag
                            val tag = when {
                                text.startsWith("//TODO") -> "TODO"
                                text.startsWith("//FIXME") -> "FIXME"
                                text.startsWith("//BUG") -> "BUG"
                                else -> "UNKNOWN"  // Fallback in case of some other comment
                            }

                            // Create a new TodoItem
                            val item = TodoItem(
                                tag = tag,
                                text = element.text,
                                fileName = psiFile.name,
                                offset = element.textOffset,
                                children = mutableListOf()
                            )

                            // If we are inside another TODO, FIXME, or BUG, make it a child
                            if (currentParent != null && tag != "BUG") {
                                currentParent?.children?.add(item)  // Safe to use 'currentParent'
                            } else {
                                todoItems.add(item)  // Add the item to the main list
                            }

                            // Set the current parent to the current TODO, FIXME, or BUG
                            if (tag == "TODO" || tag == "FIXME") {
                                currentParent = item  // Set as current parent
                            }

                            // After processing a comment, if it’s a BUG, reset the parent to null
                            if (tag == "BUG") {
                                currentParent?.children?.add(item) // Add BUG as a child of the currentParent
                            } else {
                                // For TODO and FIXME, reset the parent if it's not a BUG
                                currentParent = null
                            }
                        }
                    }
                }
            })
        }

        return todoItems
    }
}
