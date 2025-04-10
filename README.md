# IssueTracker Plugin

## Project Overview
The **IssueTracker Plugin** is an IntelliJ plugin designed to efficiently manage and interact with TODOs, FIXMEs, and BUG comments in code. It provides functionality to sync these comments with GitHub issues, ensuring better task management and faster development processes.

## Our Idea
The core idea behind this project was to automate the process of managing TODOs and related tasks by syncing them with GitHub issues. The plugin allows developers to organize and track comments directly within the IDE, which are automatically mapped to corresponding GitHub issues. Additionally, if an issue is not present in GitHub, the plugin will create it, ensuring no task is left behind.

## Technologies Used

### 1. **UI (User Interface)**
- **Swing / JBPanel**: Used for creating the UI components such as the tool window that displays TODO comments.
- **JTree**: For displaying TODOs in a hierarchical structure.
- **Asynchronous Updates**: Ensuring smooth UI interactions while the plugin fetches data or performs tasks in the background.

### 2. **Core Logic**
- **Coroutines**: Utilized Kotlin Coroutines for handling asynchronous tasks such as scanning the file for TODOs and interacting with GitHub's API.
- **Channels**: To manage communication between different parts of the application, particularly for fetching and updating TODOs and GitHub issues in parallel.
- **Data Processing**: The plugin processes code comments and matches them with relevant GitHub issues using string matching, and updates them when necessary.

### 3. **PSI (Program Structure Interface)**
- **PSI Tree Traversal**: The plugin scans the opened files by traversing their PSI tree to find specific comment tags like TODO, FIXME, or BUG.
- **PSI Integration**: Ensures precise and efficient extraction of code comments without interfering with the code's structure, providing a clean approach to scanning files.

### 4. **GitHub API Handling**
- **GitHub REST API**: Interacts with the GitHub API to fetch, create, and update issues based on the TODOs in the code.
- **API Authentication**: Uses OAuth tokens for secure communication with GitHub.
- **Issue Management**: Automatically creates new issues when no matching issue exists, updates existing issues, and closes issues that no longer have corresponding TODOs.

## Future Ideas
- **Advanced Matching**: Enhance the logic for matching TODOs with issues, including more advanced matching algorithms.
- **Priority System**: Introduce a priority system for TODOs, allowing users to prioritize tasks and synchronize them with GitHub milestones.
- **Issue Commenting**: Extend the feature set to allow more detailed issue commenting directly from the plugin.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
