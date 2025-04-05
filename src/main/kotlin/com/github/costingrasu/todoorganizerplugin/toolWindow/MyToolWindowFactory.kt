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
import com.github.costingrasu.todoorganizerplugin.getGitHubToken
import com.github.costingrasu.todoorganizerplugin.fetchGitHubIssues
import com.github.costingrasu.todoorganizerplugin.compareTodosWithIssues
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.intellij.openapi.editor.ScrollType
import javax.swing.tree.DefaultTreeCellRenderer
import java.awt.Color
import java.awt.Component

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
                // Fetch the GitHub token
                val token = getGitHubToken()

                // Example GitHub repository (replace with the correct repo)
                val repository = "costingrasu/TODO-Organizer-Plugin"

                // Fetch GitHub issues and print them
                try {
                    val issues = fetchGitHubIssues(repository, token!!)
                    println("GitHub Issues: $issues")  // Print the fetched issues

                    // Compare TODOs with issues
                    val todos = TodoScanner.findTodosInOpenedFile(project, editor)
                    compareTodosWithIssues(todos, issues, repository, token!!)
                } catch (e: Exception) {
                    println("Error fetching GitHub issues: ${e.message}")
                }

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

                        // Set a custom renderer to color the tree nodes based on their tags
                        tree.cellRenderer = object : DefaultTreeCellRenderer() {
                            override fun getTreeCellRendererComponent(
                                tree: JTree?, value: Any?, selected: Boolean,
                                expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
                            ): Component {
                                val component = super.getTreeCellRendererComponent(
                                    tree, value, selected, expanded, leaf, row, hasFocus
                                )

                                val node = value as? DefaultMutableTreeNode
                                val todoText = node?.userObject as? String

                                if (todoText != null) {
                                    // Split the text into the first word (tag) and the rest
                                    val words = todoText.split(":", limit = 2)
                                    val tag = words.first().trim()  // The first word (e.g., TODO, FIXME, BUG)
                                    val remainingText = if (words.size > 1) words[1].trim() else ""

                                    // Apply color and make the tag bold
                                    val coloredAndBoldTag = when (tag) {
                                        "TODO" -> "<font color='green'><b>$tag:</b></font>"
                                        "FIXME" -> "<font color='orange'><b>$tag:</b></font>"
                                        "BUG" -> "<font color='red'><b>$tag:</b></font>"
                                        else -> "<font color='black'>$tag:</font>"
                                    }

                                    // Combine the colored and bold tag with the remaining text
                                    val formattedText = "$coloredAndBoldTag $remainingText"

                                    // Set the formatted HTML text for the component
                                    (component as? JLabel)?.text = "<html>$formattedText</html>"
                                }

                                return component
                            }
                        }

                        // Create the context menu (JPopupMenu)
                        val contextMenu = JPopupMenu()
                        val markAsCompleted = JMenuItem("Mark as Completed")

                        // Add action to "Mark as Completed"
                        markAsCompleted.addActionListener {
                            val selectedNode = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode
                            val selectedText = selectedNode?.userObject as? String
                            if (selectedText != null) {
                                // Check if the task has already been marked as completed
                                if (!selectedText.startsWith("Completed:")) {
                                    // Mark as completed: Change text color to gray and apply strikethrough
                                    val completedText =
                                        "<html><strike><font color='gray'>$selectedText</font></strike></html>"
                                    // Update the node's userObject with the completed text
                                    selectedNode.userObject = "Completed: $completedText"
                                    // Repaint the tree to reflect the changes
                                    tree.repaint()
                                }
                            }
                        }

                        // Add items to the context menu
                        contextMenu.add(markAsCompleted)

                        // Add a mouse listener to show the context menu when right-clicking on a tree node
                        tree.addMouseListener(object : java.awt.event.MouseAdapter() {
                            override fun mousePressed(e: java.awt.event.MouseEvent?) {
                                if (e?.button == java.awt.event.MouseEvent.BUTTON3) {
                                    // Show the context menu when right-clicking
                                    val path = tree.getPathForLocation(e.x, e.y)
                                    if (path != null) {
                                        tree.selectionPath = path
                                        contextMenu.show(e.component, e.x, e.y)
                                    }
                                }
                            }
                        })

                        val scrollPane = JScrollPane(tree)

                        // Set the vertical scrollbar policy to always show
                        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                        scrollPane.preferredSize = java.awt.Dimension(300, 400)

                        // Add the scrollable panel to the content
                        add(scrollPane)

                        // Force tool window repaint to ensure the list is displayed
                        toolWindow.contentManager.getContent(0)?.component?.revalidate()
                        toolWindow.contentManager.getContent(0)?.component?.repaint()

                        // Add Tree Selection Listener to handle clicking and navigating to code
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
                    }
                }
            } else {
                // If no file is opened, show a label
                add(JLabel("No file opened"))
            }
        }

        // Recursive function to build the tree
        private fun buildTree(
            todoItems: List<TodoItem>,
            parentNode: DefaultMutableTreeNode,
            addedItems: MutableSet<String>
        ) {
            todoItems.forEach { item ->
                // Only show the tag and text, removing characters before the first space in item.text
                val itemText = "${item.tag}: ${item.text.substringAfter(" ")}"

                // Prevent adding duplicate items
                if (!addedItems.contains(itemText)) {
                    val node = DefaultMutableTreeNode(itemText)
                    parentNode.add(node)
                    addedItems.add(itemText)  // Mark this item as added
                    // Recursively add children based on parent-child relationship
                    val children = todoItems.filter { isChildOf(it, item) }
                    buildTree(children, node, addedItems)
                }
            }
        }

        // Determines if the current item is a child of the parent
        private fun isChildOf(item: TodoItem, parentItem: TodoItem): Boolean {
            return item.lineNumber > parentItem.lineNumber && item.lineNumber < parentItem.lineNumber + 10
        }

        // Find a specific TodoItem based on the text (used when selecting a tree item)
        private fun findTodoItem(todoItems: List<TodoItem>, text: String): TodoItem? {
            return todoItems.find { "${it.tag}: ${it.text.substringAfter(" ")}" == text }
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
                println("TODO not found in the document.")
            }
        }
    }
}