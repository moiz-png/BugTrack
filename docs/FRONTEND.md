# Frontend Guide

The JavaFX frontend is in the `com.bugtrack.ui` package. It uses FXML for layouts, CSS for the theme, JavaFX Charts for the dashboards, and controller classes for screen logic. The controllers contain no business rules and no SQL (proposal section 7.1).

## How it talks to the backend

Every screen calls one interface: **`com.bugtrack.ui.backend.UiBackend`**. It lists everything the UI needs: login, users, projects, bugs, workflow actions, comments, notifications, reports, snapshots and settings.

- For now it is implemented by **`FakeBackend`**, which holds sample data in memory, so the UI works before the backend exists.
- **Arham and Ahsan:** when the services are ready, write one class (for example `ServiceBackend implements UiBackend`) that calls the real services (`AuthService`, `BugService`, `WorkflowService`, `ReportService`, ...) and converts their results into the `ui.model` classes. Then add one line at startup:
  ```java
  AppContext.useBackend(new ServiceBackend(...));
  ```
  No screen needs to change.
- If an action is not allowed, throw `IllegalStateException`. If something does not exist, throw `IllegalArgumentException`. The UI shows the exception message to the user, so write it for them.
- The screens call the backend on background threads through `util.Async`, using a JavaFX `Task`. Backend methods may block. They do not need to be thread-aware about the UI.

## Folder layout

```
src/main/java/com/bugtrack/ui/
  BugTrackApp.java     JavaFX Application (start here)
  Launcher.java        main() for the executable JAR
  Navigator.java       switches screens
  Session.java         logged-in user and their settings
  AppContext.java      which UiBackend is in use
  ThemeManager.java    light/dark CSS
  View.java            list of screens
  model/               data the screens display (UserView, BugView, BugFilter, ...)
  backend/             UiBackend interface + FakeBackend
  controller/          one controller per screen
  util/                Async, Dialogs, WorkflowDialogs, Permissions, BugTables, ...
src/main/resources/com/bugtrack/ui/
  view/*.fxml          screen layouts
  css/base.css         theme (colours + component styles)
  css/dark.css         dark theme colour overrides
```

## Screens

| Screen | Who sees it | What it does |
|---|---|---|
| Login | everyone | email and password sign-in |
| Dashboard | everyone | role-specific counters, status pie chart, priority bar chart, "needs your attention" list |
| Bugs / My Bugs | everyone | search, filters (project, module, status, priority, severity, assignee, dates), saved searches, pagination; Admin can bulk assign and bulk close |
| QA Queue | Admin, QA | bugs waiting in QA Verification |
| Report Bug | Admin, QA | report form with validation, attachments and a duplicate-detection pop-up |
| Bug details | everyone | all fields, lifecycle buttons for the user's role, threaded comments with @mentions, attachments, history |
| Projects | Admin | projects, versions, modules, team members |
| Users | Admin | create, edit, deactivate, change role, reset password |
| Reports | everyone | five reports as chart + table, CSV/PDF export, save/open `.ser` snapshots |
| Notifications | everyone | list with unread highlighting; the top-bar badge refreshes every 30 s |
| Settings | everyone | light/dark theme, default project, page size, saved searches |

Keyboard shortcuts: **Ctrl+D** dashboard, **Ctrl+B** bugs, **Ctrl+N** report a bug, **Ctrl+F** search (on the bug list).

## Demo accounts (FakeBackend)

The password for all of them is `password`.

| Email | Role |
|---|---|
| admin@bugtrack.dev | Admin / PM |
| dev@bugtrack.dev | Developer |
| qa@bugtrack.dev | QA Tester |

## Build notes for the Maven setup

- Dependencies needed by the UI: `org.openjfx:javafx-controls` and `org.openjfx:javafx-fxml` (version 21).
- Main class: `com.bugtrack.ui.Launcher`. Use this one for the JAR; for `javafx:run`, use `com.bugtrack.ui.BugTrackApp`.
- If a `module-info.java` is added, it must contain:
  ```java
  opens com.bugtrack.ui.controller to javafx.fxml;
  exports com.bugtrack.ui;
  ```
