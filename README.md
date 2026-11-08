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
│   │   │   ├── Vector3D.java
│   │   │   └── physics/
│   │   ├── controller/
│   │   └── view/
│   └── test/java/dronesimulation/
│       ├── MainTest.java
│       └── model/
│           ├── Vector3DTest.java
│           └── physics/
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
| `src/main/java/dronesimulation/model/physics/` | Physics engine: `PhysicsEngine` interface, `OwnPhysicsEngine` (semi-implicit Euler), `PhysicsBody`/`PointMass`, forces (`Gravity`, `QuadraticDrag`), `WindField` with the `NoWind` null object |
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
| Taha Oguzhan Ucar (@ozi14) | Physics v1 (`model/physics/`): `PhysicsEngine` interface and `OwnPhysicsEngine` with semi-implicit Euler integration; `PhysicsBody` and `PointMass`; `Force` strategy with `Gravity` and `QuadraticDrag` (drag uses airspeed relative to the wind); `WindField` with the `NoWind` null object; `PhysicsConstants`; `Vector3D.UP`. Unit tests for every physics class, plus accuracy tests for free fall, hover drift and terminal velocity, and a determinism test | 1 |
| @Ulton321 | | |



