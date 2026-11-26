# BugTrack

**Bug Tracking and Issue Management System**: a JavaFX desktop application for reporting, assigning, resolving and QA-verifying software defects.

Course project for *Software Construction and Development (SCD)*, FAST NUCES Lahore, by Team 12.
The full proposal is in [docs/BugTrack-Proposal.pdf](docs/BugTrack-Proposal.pdf).

## Tech stack

| Concern | Technology |
|---|---|
| Language | Java 17 |
| UI | JavaFX 21 (FXML + CSS) |
| Database | MySQL 8 |
| Persistence | Hibernate 6 (entity mapping) + JDBC `PreparedStatement` (transactions, bulk updates, reports) |
| Connection pool | HikariCP |
| Build | Maven |
| Testing | JUnit 5, Mockito, JaCoCo |
| Security | jBCrypt |
| Reporting | OpenPDF, OpenCSV |

## Architecture

Strict layers, each depending only on the one below it through interfaces:

```
Presentation (JavaFX controllers, FXML)
      ↓
Service (business rules, workflow, permissions)
      ↓
Data Access (Hibernate repositories + JDBC DAOs)
      ↓
MySQL 8
```

The domain model (`User`, `Project`, `Bug`, `Comment`, ...) is shared by all layers.

## Bug lifecycle

`New → Assigned → In Progress → Resolved → QA Verification → Closed`

A bug that fails QA verification is **Reopened** and goes back to the developer. An admin can mark an invalid report **Rejected / Duplicate**.

## Getting started

> Setup instructions (JDK, MySQL, configuration, build and run) will be added once the Maven project is in place.

## Git workflow

- `main` holds released code only. Changes arrive by pull request.
- `develop` is the integration branch.
- Each task gets its own `feature/<issue-id>-<short-name>` branch, opened as a PR into `develop` and reviewed by at least one teammate.
- Commit messages follow Conventional Commits: `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`.
- Each milestone is tagged as a release: `v0.1`, `v0.2`, `v1.0`.
