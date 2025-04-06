package com.github.costingrasu.todoorganizerplugin

import okhttp3.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

fun getGitHubToken(): String? {
    val token = "KEY"

    if (token.isEmpty()) {
        throw IllegalStateException("GitHub token is missing or empty")
    }

    return token
}

fun fetchGitHubIssues(repository: String, token: String): List<Issue> {
    val client = OkHttpClient()
    val url = "https://api.github.com/repos/$repository/issues?state=open"
    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "token $token")
        .build()

    val response = client.newCall(request).execute()

    if (!response.isSuccessful) {
        throw IOException("Unexpected code $response")
    }

    return parseIssuesResponse(response.body?.string() ?: "")
}

fun parseIssuesResponse(responseBody: String): List<Issue> {
    val gson = Gson()
    val issuesArray = gson.fromJson(responseBody, Array<Issue>::class.java)
    return issuesArray.toList()
}

data class Issue(val id: Long, val title: String, val body: String, val state: String)

fun updateGitHubIssue(issueId: Long, repository: String, token: String, comment: String) {
    val client = OkHttpClient()
    val url = "https://api.github.com/repos/$repository/issues/$issueId/comments"
    val body = "{\"body\": \"$comment\"}"

    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "token $token")
        .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

    val response = client.newCall(request).execute()
    if (response.isSuccessful) {
        println("Successfully updated issue with comment: $comment")
    } else {
        println("Failed to update issue: ${response.message}")
    }
}

fun createGitHubIssue(repository: String, token: String, todo: TodoItem) {
    val client = OkHttpClient()
    val url = "https://api.github.com/repos/$repository/issues"
    val body = """
        {
            "title": "${todo.text}",
            "body": "This issue corresponds to the TODO comment in the code. \n\nDetails: ${todo.text}",
            "labels": ["TODO"]
        }
    """

    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "token $token")
        .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

    val response = client.newCall(request).execute()
    if (response.isSuccessful) {
        println("Successfully created GitHub issue for TODO: ${todo.text}")
    } else {
        println("Failed to create GitHub issue: ${response.message}")
    }
}

fun compareTodosWithIssues(todos: List<TodoItem>, issues: List<Issue>, repository: String, token: String) {
    todos.forEach { todo ->
        val matchingIssue = issues.find { issue ->
            issue.title?.contains(todo.text, ignoreCase = true) == true ||
                    issue.body?.contains(todo.text, ignoreCase = true) == true
        }

        if (matchingIssue != null) {
            println("Found matching GitHub issue: ${matchingIssue.title} (ID: ${matchingIssue.id})")
            updateGitHubIssue(matchingIssue.id, repository, token, "This issue is related to a TODO comment in the code.")
        } else {
            println("No matching GitHub issue found for TODO: ${todo.text}")
            createGitHubIssue(repository, token, todo)
        }
    }

    issues.forEach { issue ->
        val isTodoExist = todos.any { todo ->
            issue.title?.contains(todo.text, ignoreCase = true) == true ||
                    issue.body?.contains(todo.text, ignoreCase = true) == true
        }

        if (!isTodoExist) {
            println("GitHub issue ${issue.title} (ID: ${issue.id}) does not have a matching TODO.")
            closeGitHubIssue(issue.id, repository, token)
        }
    }
}

fun closeGitHubIssue(issueId: Long, repository: String, token: String) {
    val client = OkHttpClient()
    val url = "https://api.github.com/repos/$repository/issues/$issueId"
    val body = """
        {
            "state": "closed"
        }
    """

    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "token $token")
        .patch(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

    val response = client.newCall(request).execute()
    if (response.isSuccessful) {
        println("Successfully closed GitHub issue with ID: $issueId")
    } else {
        println("Failed to close GitHub issue: ${response.message}")
    }
}

