# BugTrack

**Bug Tracking and Issue Management System**: a JavaFX desktop application for reporting, assigning, resolving and QA-verifying software defects.

Course project for *Software Construction and Development (SCD)*, FAST NUCES Lahore, by Team 12.
The full proposal is in [docs/BugTrack-Proposal.pdf](docs/BugTrack-Proposal.pdf). The Phase 1 design (requirements and UML diagrams) is in [docs/BugTrack-Phase1-Documentation.pdf](docs/BugTrack-Phase1-Documentation.pdf).

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

## Project structure

```
BugTrack/
├── config/
│   └── db.example.properties   copy to db.properties (ignored by Git) and add your DB password
├── docs/
│   ├── BugTrack-Proposal.pdf
│   ├── BugTrack-Phase1-Documentation.pdf   FRs, NFRs, use case, class, ER and package diagrams
│   ├── WORK_PLAN.md            who does what
│   ├── FRONTEND.md             how the UI connects to the backend
│   └── diagrams/               ER diagram, class diagram, wireframes
└── src/
    ├── main/
    │   ├── java/com/bugtrack/
    │   │   ├── ui/             Moiz   JavaFX app, controllers, UiBackend + FakeBackend
    │   │   ├── domain/         Arham  JPA entities and enums
    │   │   ├── infra/          Arham  HibernateUtil, ConnectionManager (Singleton)
    │   │   ├── dao/            Arham  repository/DAO interfaces
    │   │   │   ├── hibernate/  Arham  Hibernate repositories (CRUD)
    │   │   │   └── jdbc/       Arham  JDBC DAOs (transactions, batch, report queries)
    │   │   ├── report/         Arham  ReportService
    │   │   │   └── export/     Arham  PDF/CSV exporters (Strategy)
    │   │   ├── snapshot/       Arham  serialization: snapshots, user settings
    │   │   ├── service/        Ahsan  service interfaces
    │   │   │   └── impl/       Ahsan  service implementations
    │   │   ├── security/       Ahsan  BCrypt, session, permission checks
    │   │   ├── workflow/       Ahsan  bug lifecycle (State)
    │   │   ├── event/          Ahsan  notifications, activity log (Observer)
    │   │   ├── factory/        Ahsan  BugFactory, UserFactory (Factory)
    │   │   ├── command/        Ahsan  undo last edit (Command, optional)
    │   │   └── exception/      shared application exceptions
    │   └── resources/
    │       ├── com/bugtrack/ui/view/   FXML screens
    │       ├── com/bugtrack/ui/css/    themes
    │       └── db/                     schema.sql, seed data
    └── test/
        ├── java/com/bugtrack/  JUnit 5 + Mockito tests, same packages as main
        └── resources/
```

Each Java package has a `package-info.java` describing what belongs in it.

## Getting started

> Setup instructions (JDK, MySQL, configuration, build and run) will be added once the Maven project is in place.

## Team

| Member | Area |
|---|---|
| Moiz | Frontend (JavaFX UI) |
| Arham | Database, persistence and reporting |
| Ahsan | Business logic (service layer) |

The full task split is in [docs/WORK_PLAN.md](docs/WORK_PLAN.md). The frontend guide is in [docs/FRONTEND.md](docs/FRONTEND.md).

## Git workflow

| Branch | Purpose |
|---|---|
| `main` | Released code only. One merge from `develop` per milestone, tagged `v0.1`, `v0.2`, `v1.0`. |
| `develop` | The integration branch. All finished features are merged here. |
| `feature/<short-name>` | One branch per task, e.g. `feature/login-screen` or `feature/bug-entity` |

**Rules**

1. **Start each task from `develop`:**
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/<short-name>
   ```
2. **Commit small changes often** on your feature branch.
3. **Open a pull request into `develop`** when the task is done. At least one teammate reviews it before it is merged. Then delete the feature branch.
4. **Nobody pushes directly to `main` or `develop`.**
5. **At the end of each milestone,** when `develop` is stable, merge `develop` into `main` and tag the release.
6. **Use Conventional Commits.** Start each message with `feat:`, `fix:`, `test:`, `docs:`, `refactor:` or `chore:`.
