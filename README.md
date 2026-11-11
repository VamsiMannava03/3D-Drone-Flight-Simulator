# 3D-Drone-Flight-Simulator

Hover is a Java desktop app that simulates drones flying in a 3D world under real physics. Users add drones, plan missions such as take off, fly to a point, hover and land, and watch the results in 2D and 3D views. The simulation models gravity, thrust, drag, wind, battery drain and collisions, so students can test drone behaviour without the cost and risk of a real drone. It is built with MVC and a custom physics engine so new drone types and features can be added without rewriting the system.

## Team

Vamsi Mannava @VamsiMannava03

Muhammed Bilal @Attested-paper51

Taha Oguzhan Ucar @ozi14

Sophakotra Son @Ulton321

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
│   │   │   └── obstacles/
│   │   │   └── physics/
│   │   ├── controller/
│   │   └── view/
│   └── test/java/dronesimulation/
│       ├── MainTest.java
│       └── model/
│           ├── Vector3DTest.java
│           └── obstacles/
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
| `src/main/java/dronesimulation/model/obstacles/` | Obstacles (boxes and spheres) and collision checks between drones and obstacles |
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

| Functional Requirement (from SRS) | Status | Notes |
|---|---|---|
| Maven MVC project layout | Complete | Model, controller and view packages |
| `Vector3D` math class | Complete | Immutable, rejects NaN and infinite values |
| FR3: Physics engine (gravity, thrust, drag) | In progress | Being built this phase |
| FR13: Collision detection with obstacles | Partial | Box and sphere obstacles with collision checks done, drone to drone collisions not started |
| FR1, FR9, FR10: Drones, flight controllers, missions | Not started | Planned for Phase 3 |
| FR17, FR18: JavaFX 2D and 3D views, controls | Not started | Planned for Phase 3 |

## Testing So Far

- JUnit 5 unit tests, run with `mvn test`
- 46 tests, all passing: 15 for `Vector3D`, 30 for obstacles, 1 for `Main`

## Next Steps (Phase 3)

- Finish the physics engine and connect obstacle collisions to it so a drone crashes on contact
- Add drones, flight controllers and missions
- Build the JavaFX views and a console run of one full flight

## Team Contributions

| Member | Contributions This Phase | Commits |
|--------|--------------------------|---------|
| Vamsi Mannava (@VamsiMannava03) | Repo setup (starter project, SRS, PR template, CODEOWNERS); Maven project layout; `Vector3D` class and unit tests (in progress) | 9 |
| Muhammed Bilal (@Attested-paper51) | Obstacles for collision detection (FR13): `Obstacle`, `BoxObstacle`, `SphereObstacle`, `ObstacleField` in `model/obstacles/`, with 30 unit tests; reviewed PR #3 | 2 |
| Sophakotra Son (@Ulton321) | Built an early prototype demo outside this repo to show the team the target look and features of the simulator | 0 |
| Taha Oguzhan Ucar (@ozi14) | Physics v1 (`model/physics/`): `PhysicsEngine` interface and `OwnPhysicsEngine` with semi-implicit Euler integration; `PhysicsBody` and `PointMass`; `Force` strategy with `Gravity` and `QuadraticDrag` (drag uses airspeed relative to the wind); `WindField` with the `NoWind` null object; `PhysicsConstants`; `Vector3D.UP`. Unit tests for every physics class, plus accuracy tests for free fall, hover drift and terminal velocity, and a determinism test | 1 |



