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

## Team

| Member | Branch | Area |
|---|---|---|
| Moiz | `moiz` | Frontend (JavaFX UI) |
| Arham | `arham` | TBD |
| Ahsan | `ahsan` | TBD |

## Git workflow

| Branch | Purpose |
|---|---|
| `main` | Final, reviewed code. Nobody pushes to it directly. |
| `moiz`, `arham`, `ahsan` | One working branch per team member |

**Rules**

1. **Work on your own branch.** Commit small changes often.
2. **Merge into `main` often.** When a piece of work is finished (a screen, an entity, a service), open a pull request from your branch into `main`. At least one teammate reviews it before it is merged. Aim for every few days, not once a week.
3. **Pull `main` into your branch often.** Before starting new work, run:
   ```bash
   git checkout <your-branch>
   git pull origin main
   ```
   This brings in your teammates' merged work, so conflicts stay small.
4. **Use Conventional Commits.** Start each message with `feat:`, `fix:`, `test:`, `docs:`, `refactor:` or `chore:`.
5. **Tag each milestone on `main`:** `v0.1`, `v0.2`, `v1.0`.
