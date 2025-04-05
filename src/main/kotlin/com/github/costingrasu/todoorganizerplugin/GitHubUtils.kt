package com.github.costingrasu.todoorganizerplugin

import okhttp3.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

// Function to load GitHub token from .env file
fun getGitHubToken(): String? {
    val token = "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"  // Replace with your GitHub token

    if (token.isEmpty()) {
        throw IllegalStateException("GitHub token is missing or empty")
    }

    return token
}

// Function to fetch issues from a GitHub repository
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

    // Parse the response and return a list of issues
    return parseIssuesResponse(response.body?.string() ?: "")
}

// Helper function to parse the issues response
fun parseIssuesResponse(responseBody: String): List<Issue> {
    val gson = Gson()
    val issuesArray = gson.fromJson(responseBody, Array<Issue>::class.java)  // Parse JSON to Issue array
    return issuesArray.toList()  // Convert array to list
}

data class Issue(val id: Long, val title: String, val body: String, val state: String)

// Update an existing issue by adding a comment
fun updateGitHubIssue(issueId: Long, repository: String, token: String, comment: String) {
    val client = OkHttpClient()
    val url = "https://api.github.com/repos/$repository/issues/$issueId/comments"
    val body = "{\"body\": \"$comment\"}"

    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "token $token")
        .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))  // Updated here
        .build()

    val response = client.newCall(request).execute()
    if (response.isSuccessful) {
        println("Successfully updated issue with comment: $comment")
    } else {
        println("Failed to update issue: ${response.message}")
    }
}

// Create a new GitHub issue if a TODO does not have a matching issue
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
        .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))  // Updated here
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
            // Compare TODO text with issue title or body
            issue.title.contains(todo.text, ignoreCase = true) || issue.body.contains(todo.text, ignoreCase = true)
        }

        if (matchingIssue != null) {
            println("Found matching GitHub issue: ${matchingIssue.title} (ID: ${matchingIssue.id})")
            // Update the issue on GitHub or add a comment based on the TODO
            updateGitHubIssue(matchingIssue.id, repository, token, "This issue is related to a TODO comment in the code.")
        } else {
            // No matching issue found, create a new issue
            println("No matching GitHub issue found for TODO: ${todo.text}")
            createGitHubIssue(repository, token, todo)
        }
    }

    // Check for issues in GitHub that don't have corresponding TODOs
    issues.forEach { issue ->
        val isTodoExist = todos.any { todo ->
            issue.title.contains(todo.text, ignoreCase = true) || issue.body.contains(todo.text, ignoreCase = true)
        }

        if (!isTodoExist) {
            println("GitHub issue ${issue.title} (ID: ${issue.id}) does not have a matching TODO.")
            // Handle issues without corresponding TODOs (e.g., close or delete)
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

