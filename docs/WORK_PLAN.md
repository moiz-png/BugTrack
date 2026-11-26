# BugTrack Work Plan

Each member owns one layer of the application. This keeps everyone in different files and keeps merge conflicts small.

## Moiz: Frontend (JavaFX)

- App shell: main window, navigation, a sidebar that changes by role, and a CSS theme
- Login screen
- Bug list: a table with search, filters, pagination and saved searches
- Report-bug form, including the "similar bugs" pop-up that flags possible duplicates
- Bug details screen: comments, attachments, activity history, resolve form, assign dialog, and QA verify/reopen buttons
- Admin screens: users, projects, modules and team members
- Notifications panel
- Dashboards with charts, one per role
- Reports screen: preview, export to PDF/CSV, save and load snapshots
- Settings screen: theme, default project, table layout
- Background threads (JavaFX `Task`) for every backend call, so the UI never freezes

## Arham: Database, persistence and reporting

- Maven project setup (`pom.xml`, Maven Wrapper, folder structure)
- MySQL schema script, indexes, ER diagram and seed data
- `ConnectionManager` (HikariCP) and `HibernateUtil`: the **Singleton** pattern
- All 10 Hibernate entities, plus the Role, Status, Priority and Severity enums
- Repositories for Hibernate CRUD: `BugRepository`, `UserRepository`, `ProjectRepository`, ...
- JDBC DAOs with transactions: bug assignment (commit/rollback), bulk status updates, report queries
- Reporting: `ReportService` plus PDF/CSV exporters, using the **Strategy** pattern
- Serialization: report snapshots, user settings and offline bug export
- Integration tests: Hibernate mappings, JDBC rollback, serialization round-trip

## Ahsan: Business logic

- Service interfaces: the methods the frontend screens call
- Login and security: BCrypt hashing, sessions, password reset, and role permission checks
- Bug lifecycle with the **State** pattern: 8 states, which moves are allowed, and which role can make each move
- `BugService` / `WorkflowService`: report, assign, resolve, QA verify, reopen, reject
- User, project and module services, with the **Factory** pattern
- Notifications and activity log with the **Observer** pattern
- Comments, and duplicate detection by keyword matching
- Undo last edit with the **Command** pattern (optional)
- Unit tests with JUnit 5 and Mockito, reaching at least 70% JaCoCo coverage on the services

## Everyone

- Write JavaDoc for your own public classes and methods
- Review each other's pull requests before they are merged into `main`
- Write the final report and prepare the demo together

## What has to happen first

1. **Arham** merges the Maven setup into `main`, because nobody can write code without it.
2. **Ahsan** merges the service interfaces, and **Arham** merges the entities and enums. These are just method signatures and fields, with no logic yet.
3. **Moiz** builds the screens against fake services that return sample data, then switches to the real ones when they are ready.
