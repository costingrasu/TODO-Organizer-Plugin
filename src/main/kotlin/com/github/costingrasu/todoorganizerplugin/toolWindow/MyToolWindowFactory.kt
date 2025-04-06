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
                val token = getGitHubToken()

                val repository = "costingrasu/TODO-Organizer-Plugin"

                try {
                    val issues = fetchGitHubIssues(repository, token!!)
                    println("GitHub Issues: $issues")

                    val todos = TodoScanner.findTodosInOpenedFile(project, editor)
                    compareTodosWithIssues(todos, issues, repository, token!!)
                } catch (e: Exception) {
                    println("Error fetching GitHub issues: ${e.message}")
                }

                GlobalScope.launch(Dispatchers.IO) {
                    val todos = TodoScanner.findTodosInOpenedFile(project, editor)

                    withContext(Dispatchers.Main) {
                        val root = DefaultMutableTreeNode("Root")
                        val addedItems = mutableSetOf<String>()

                        buildTree(todos, root, addedItems)

                        val tree = JTree(DefaultTreeModel(root))

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
                                    val words = todoText.split(":", limit = 2)
                                    val tag = words.first().trim()
                                    val remainingText = if (words.size > 1) words[1].trim() else ""

                                    val coloredAndBoldTag = when (tag) {
                                        "TODO" -> "<font color='green'><b>$tag:</b></font>"
                                        "FIXME" -> "<font color='orange'><b>$tag:</b></font>"
                                        "BUG" -> "<font color='red'><b>$tag:</b></font>"
                                        else -> "<font color='black'>$tag:</font>"
                                    }

                                    val formattedText = "$coloredAndBoldTag $remainingText"

                                    (component as? JLabel)?.text = "<html>$formattedText</html>"
                                }

                                return component
                            }
                        }

                        val contextMenu = JPopupMenu()
                        val markAsCompleted = JMenuItem("Mark as Completed")

                        markAsCompleted.addActionListener {
                            val selectedNode = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode
                            val selectedText = selectedNode?.userObject as? String
                            if (selectedText != null) {
                                if (!selectedText.startsWith("Completed:")) {
                                    val completedText =
                                        "<html><strike><font color='gray'>$selectedText</font></strike></html>"
                                    selectedNode.userObject = "Completed: $completedText"
                                    tree.repaint()
                                }
                            }
                        }

                        contextMenu.add(markAsCompleted)

                        tree.addMouseListener(object : java.awt.event.MouseAdapter() {
                            override fun mousePressed(e: java.awt.event.MouseEvent?) {
                                if (e?.button == java.awt.event.MouseEvent.BUTTON3) {
                                    val path = tree.getPathForLocation(e.x, e.y)
                                    if (path != null) {
                                        tree.selectionPath = path
                                        contextMenu.show(e.component, e.x, e.y)
                                    }
                                }
                            }
                        })

                        val scrollPane = JScrollPane(tree)

                        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                        scrollPane.preferredSize = java.awt.Dimension(300, 400)

                        add(scrollPane)

                        toolWindow.contentManager.getContent(0)?.component?.revalidate()
                        toolWindow.contentManager.getContent(0)?.component?.repaint()

                        tree.addTreeSelectionListener { event ->
                            val selectedNode = event.path.lastPathComponent as? DefaultMutableTreeNode
                            val selectedText = selectedNode?.userObject as? String
                            if (selectedText != null) {
                                val selectedItem = findTodoItem(todos, selectedText)
                                if (selectedItem != null) {
                                    navigateToTodoInCode(selectedItem, editor)
                                }
                            }
                        }
                    }
                }
            } else {
                add(JLabel("No file opened"))
            }
        }

        private fun buildTree(
            todoItems: List<TodoItem>,
            parentNode: DefaultMutableTreeNode,
            addedItems: MutableSet<String>
        ) {
            todoItems.forEach { item ->
                val itemText = "${item.tag}: ${item.text.substringAfter(" ")}"

                if (!addedItems.contains(itemText)) {
                    val node = DefaultMutableTreeNode(itemText)
                    parentNode.add(node)
                    addedItems.add(itemText)
                    val children = todoItems.filter { isChildOf(it, item) }
                    buildTree(children, node, addedItems)
                }
            }
        }

        private fun isChildOf(item: TodoItem, parentItem: TodoItem): Boolean {
            return item.lineNumber > parentItem.lineNumber && item.lineNumber < parentItem.lineNumber + 10
        }

        private fun findTodoItem(todoItems: List<TodoItem>, text: String): TodoItem? {
            return todoItems.find { "${it.tag}: ${it.text.substringAfter(" ")}" == text }
        }

        private fun navigateToTodoInCode(todoItem: TodoItem, editor: Editor) {
            val line = todoItem.lineNumber
            if (line != -1) {
                val document = editor.document
                val scrollingModel = editor.scrollingModel

                val startOffset = document.getLineStartOffset(line)
                editor.caretModel.moveToOffset(startOffset)
                scrollingModel.scrollToCaret(ScrollType.CENTER)
            } else {
                println("TODO not found in the document.")
            }
        }
    }
}