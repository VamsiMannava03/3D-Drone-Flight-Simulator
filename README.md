# 3D-Drone-Flight-Simulator

## Team

Vamsi Mannava @VamsiMannava03

## Project Structure

```
.
├── .claude/
│   ├── CLAUDE.md
│   └── docs/
│       ├── SRS.pdf
│       └── SRS.md
├── .github/
│   ├── CODEOWNERS
│   └── pull_request_template.md
├── pom.xml
├── src/
│   ├── main/java/dronesimulation/
│   │   ├── Main.java
│   │   ├── model/
│   │   │   └── Vector3D.java
│   │   ├── controller/
│   │   └── view/
│   └── test/java/dronesimulation/
│       ├── MainTest.java
│       └── model/
│           └── Vector3DTest.java
└── README.md
```

| Path | Description |
|------|-------------|
| `.claude/CLAUDE.md` | Project context loaded automatically by Claude Code |
| `.claude/docs/` | Software Requirements Specification (original PDF and text copy) |
| `.github/CODEOWNERS` | Code owners requested as reviewers on every PR |
| `.github/pull_request_template.md` | Default description for new pull requests |
| `pom.xml` | Maven build: JDK 25, JUnit 5, JaCoCo |
| `src/main/java/dronesimulation/Main.java` | Application entry point |
| `src/main/java/dronesimulation/model/` | Model: simulation state and logic with no UI code (physics, drones, world, weather) |
| `src/main/java/dronesimulation/model/Vector3D.java` | Immutable 3D vector used for positions, velocities and forces |
| `src/main/java/dronesimulation/controller/` | Controller: turns user input into commands on the model |
| `src/main/java/dronesimulation/view/` | Views: draw read-only snapshots of the model and never modify it |
| `src/test/java/dronesimulation/` | JUnit 5 tests, mirroring the `src/main/java/dronesimulation/` package layout |

## Setup and Run

Requires JDK 25 and Maven 3.9 or newer.

```bash
mvn test       # compile and run all JUnit tests
mvn compile
java -cp target/classes dronesimulation.Main
```

## Progress so Far

## Testing So Far

## Team Contributions

| Member | Contributions This Phase | Commits |
|--------|--------------------------|---------|
| Vamsi Mannava (@VamsiMannava03) | Repo setup (starter project, SRS, PR template, CODEOWNERS); Maven project layout; `Vector3D` class and unit tests (in progress) | 9 |
| @Attested-paper51 | | |
| @ozi14 | | |
| @Ulton321 | | |



