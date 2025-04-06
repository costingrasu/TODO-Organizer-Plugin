package com.github.costingrasu.todoorganizerplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.costingrasu.todoorganizerplugin.MyBundle

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {

    init {
    }

    fun getRandomNumber() = (1..100).random()
}
