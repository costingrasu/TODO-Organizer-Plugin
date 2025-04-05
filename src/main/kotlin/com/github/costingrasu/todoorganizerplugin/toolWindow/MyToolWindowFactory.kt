package com.github.costingrasu.todoorganizerplugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.github.costingrasu.todoorganizerplugin.TodoScanner
import com.github.costingrasu.todoorganizerplugin.TodoItem
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.intellij.openapi.editor.ScrollType

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow, project)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(private val toolWindow: ToolWindow, private val project: Project) {

        fun getContent(): JPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            val editor = FileEditorManager.getInstance(project).selectedTextEditor

            if (editor != null) {
                // Fetch the TODOs using the TodoScanner for the opened file
                GlobalScope.launch(Dispatchers.IO) {
                    val todos = TodoScanner.findTodosInOpenedFile(project, editor)

                    withContext(Dispatchers.Main) {
                        // Create a tree structure to group the TODOs, FIXMEs, and BUGs hierarchically
                        val root = DefaultMutableTreeNode("Root")
                        val addedItems = mutableSetOf<String>()  // To avoid duplicates

                        // Add the root node with each grouped category (TODO, FIXME, BUG)
                        buildTree(todos, root, addedItems)

                        // Create a JTree and add it to a JScrollPane
                        val tree = JTree(DefaultTreeModel(root))
                        val scrollPane = JScrollPane(tree)

                        // Set the vertical scrollbar policy to always show
                        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                        scrollPane.preferredSize = java.awt.Dimension(300, 400)

                        // Add ListSelectionListener to handle click events
                        tree.addTreeSelectionListener { event ->
                            val selectedNode = event.path.lastPathComponent as? DefaultMutableTreeNode
                            val selectedText = selectedNode?.userObject as? String
                            if (selectedText != null) {
                                // Find the corresponding TodoItem based on the selected text
                                val selectedItem = findTodoItem(todos, selectedText)
                                if (selectedItem != null) {
                                    // Navigate to the corresponding line in the code
                                    navigateToTodoInCode(selectedItem, editor)
                                }
                            }
                        }

                        // Add the scrollable panel to the content
                        add(scrollPane)

                        // Force tool window repaint to ensure the list is displayed
                        toolWindow.contentManager.getContent(0)?.component?.revalidate()
                        toolWindow.contentManager.getContent(0)?.component?.repaint()
                    }
                }
            } else {
                // If no file is opened, show a label
                add(JLabel("No file opened"))
            }
        }

        // Recursive function to build the tree
        private fun buildTree(todoItems: List<TodoItem>, parentNode: DefaultMutableTreeNode, addedItems: MutableSet<String>) {
            val todoMap = mutableMapOf<String, TodoItem>()
            todoItems.forEach { item ->
                val itemText = "${item.tag}: ${item.text} (Line: ${item.lineNumber})"

                // Prevent adding duplicate items
                if (!addedItems.contains(itemText)) {
                    val node = DefaultMutableTreeNode(itemText)
                    parentNode.add(node)
                    addedItems.add(itemText)  // Mark this item as added
                    todoMap[itemText] = item
                    // Recursively add children based on parent-child relationship
                    val children = todoItems.filter { isChildOf(it, item) }
                    buildTree(children, node, addedItems)
                }
            }
        }

        // Determines if the current item is a child of the parent
        private fun isChildOf(item: TodoItem, parentItem: TodoItem): Boolean {
            // Logic to determine if the item is inside the parent (based on line numbers)
            return item.lineNumber > parentItem.lineNumber && item.lineNumber < parentItem.lineNumber + 10
        }

        // Find a specific TodoItem based on the text (used when selecting a tree item)
        private fun findTodoItem(todoItems: List<TodoItem>, text: String): TodoItem? {
            return todoItems.find { "${it.tag}: ${it.text} (Line: ${it.lineNumber})" == text }
        }

        // Navigates to the line containing the TODO
        private fun navigateToTodoInCode(todoItem: TodoItem, editor: Editor) {
            val line = todoItem.lineNumber
            if (line != -1) {
                val document = editor.document
                val scrollingModel = editor.scrollingModel

                // Move the caret to the specific line and scroll to it
                val startOffset = document.getLineStartOffset(line)
                editor.caretModel.moveToOffset(startOffset)  // Move the caret to the start of the line
                scrollingModel.scrollToCaret(ScrollType.CENTER)  // Scroll the editor to the caret position
            } else {
                // If the line could not be found, log an error or show a message
                println("TODO not found in the document.")
            }
        }
    }
}
