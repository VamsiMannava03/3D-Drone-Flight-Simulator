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
├── .mvn/wrapper/
├── mvnw, mvnw.cmd
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
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/` | Maven Wrapper: runs Maven 3.9.11 without installing it |
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

Requires **JDK 25**. Maven does not need to be installed: the Maven Wrapper (`mvnw` / `mvnw.cmd`) downloads Maven 3.9.11 on first run.

### 1. Install JDK 25

| OS | Command |
|----|---------|
| Windows | `winget install EclipseAdoptium.Temurin.25.JDK` |
| macOS | `brew install --cask temurin@25` |

### 2. Point `JAVA_HOME` at JDK 25

`pom.xml` compiles with `release 25`, so an older JDK (e.g. 19 or 21) fails with `release version 25 not supported`.

Windows (PowerShell). Change the path if your install folder is different:

```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot', 'User')
```

macOS (add to `~/.zshrc`):

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
```

**Restart your terminal and IDE** after changing environment variables, then check:

```bash
java -version   # should print 25
```

If it still prints an older version on Windows, remove the old JDK from your `Path` or move it below JDK 25 (System Properties → Environment Variables).

### 3. Build, test and run

macOS / Linux:

```bash
./mvnw test            # compile and run all JUnit tests (coverage report: target/site/jacoco/index.html)
./mvnw javafx:run      # open the Hover window (3D view, top/side views, panels)
./mvnw javafx:run -Djavafx.args="--scenario=empty --mission=missions/usecase.txt"
./mvnw compile && java -cp target/classes dronesimulation.Main text   # console mode only
```

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd javafx:run
.\mvnw.cmd javafx:run "-Djavafx.args=--scenario=empty --mission=missions/usecase.txt"
.\mvnw.cmd compile; java -cp target/classes dronesimulation.Main text
```

Options (put them in `-Djavafx.args="..."` for the window, or after `Main` for the console):

| Option | Default | Meaning |
|---|---|---|
| `gui` / `text` | `gui` (`text` without a display) | The JavaFX window, or the terminal only |
| `--scenario=city\|empty` | `city` | `empty` is a bare ground plane, the start of the SRS use case |
| `--mission=FILE` | `missions/demo.txt` for the city | Commands to run at start, and again after every reset |
| `--seed=N` | `42` | Seed for gusts and sensor noise; the same seed gives the same run |
| `--paused` | off | Start paused; press Start or type `start` |
| `--screenshot=FILE.png` | off | Save `FILE-3d.png`, `FILE-2d.png` and `FILE-follow.png` of the window, then close |
| `--screenshot-at=S` | `8` | Simulated seconds to wait before the screenshots |

In the window, the terminal still takes commands (type `help`). In the 3D view, drag to turn the
camera, right-drag to move it and scroll to zoom; *Follow selected drone* keeps the drone picked in
the status panel in the middle.

### IDE setup

- **IntelliJ IDEA:** open the folder as a Maven project, then File → Project Structure → SDK → JDK 25.
- **VS Code:** install *Extension Pack for Java*, then add to your user `settings.json`:

  ```json
  "java.configuration.runtimes": [
    { "name": "JavaSE-25", "path": "C:\\Program Files\\Eclipse Adoptium\\jdk-25.0.5.8-hotspot", "default": true }
  ]
  ```

  Run `Main` with the **Run** link above `main`, and tests from the **Testing** panel.

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



