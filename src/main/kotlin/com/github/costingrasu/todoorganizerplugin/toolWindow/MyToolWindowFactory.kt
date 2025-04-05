package com.github.costingrasu.todoorganizerplugin.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.costingrasu.todoorganizerplugin.TodoScanner
import com.github.costingrasu.todoorganizerplugin.TodoItem
import javax.swing.JButton
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow, project)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow, private val project: Project) {

        private val todoScanner = TodoScanner()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            // Fetch the TODOs using the TodoScanner
            val todos = todoScanner.findTodos(project)

            // Build the root node for the tree
            val rootNode = DefaultMutableTreeNode("TODOs")

            // Build the tree structure recursively
            buildTree(todos, rootNode)

            // Create the JTree and add it to the panel
            val tree = JTree(DefaultTreeModel(rootNode))

            // Optional: Make the tree more readable
            tree.isRootVisible = false  // Hide the root node
            tree.expandsSelectedPaths = true  // Automatically expand selected nodes

            add(tree)
        }

        // Helper function to build the tree recursively
        private fun buildTree(todoItems: List<TodoItem>, parentNode: DefaultMutableTreeNode) {
            todoItems.forEach { todo ->
                // Create a node for the TODO item
                val node = DefaultMutableTreeNode("${todo.tag}: ${todo.text}")
                parentNode.add(node)

                // If this TODO has children (e.g., BUG inside TODO), add them recursively
                buildTree(todo.children, node)
            }
        }
    }
}