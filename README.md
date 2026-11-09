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
├── .mvn/wrapper/
├── mvnw, mvnw.cmd
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
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/` | Maven Wrapper: runs Maven 3.9.11 without installing it |
| `pom.xml` | Maven build: JDK 25, JUnit 5, JaCoCo |
| `src/main/java/dronesimulation/Main.java` | Application entry point |
| `src/main/java/dronesimulation/model/` | Model: simulation state and logic with no UI code (physics, drones, world, weather) |
| `src/main/java/dronesimulation/model/Vector3D.java` | Immutable 3D vector used for positions, velocities and forces |
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
./mvnw test        # compile and run all JUnit tests (coverage report: target/site/jacoco/index.html)
./mvnw compile
java -cp target/classes dronesimulation.Main
```

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd compile
java -cp target/classes dronesimulation.Main
```

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

## Testing So Far

## Team Contributions

| Member | Contributions This Phase | Commits |
|--------|--------------------------|---------|
| Vamsi Mannava (@VamsiMannava03) | Repo setup (starter project, SRS, PR template, CODEOWNERS); Maven project layout; `Vector3D` class and unit tests (in progress) | 9 |
| @Attested-paper51 | | |
| Taha Oguzhan Ucar (@ozi14) | Physics v1 (`model/physics/`): `PhysicsEngine` interface and `OwnPhysicsEngine` with semi-implicit Euler integration; `PhysicsBody` and `PointMass`; `Force` strategy with `Gravity` and `QuadraticDrag` (drag uses airspeed relative to the wind); `WindField` with the `NoWind` null object; `PhysicsConstants`; `Vector3D.UP`. Unit tests for every physics class, plus accuracy tests for free fall, hover drift and terminal velocity, and a determinism test | 1 |
| @Ulton321 | | |



