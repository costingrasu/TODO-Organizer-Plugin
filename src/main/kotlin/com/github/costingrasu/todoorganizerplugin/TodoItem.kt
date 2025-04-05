package com.github.costingrasu.todoorganizerplugin

data class TodoItem(
    val tag: String,
    val text: String,
    val fileName: String,
    val offset: Int,
    val children: MutableList<TodoItem> = mutableListOf()
)
