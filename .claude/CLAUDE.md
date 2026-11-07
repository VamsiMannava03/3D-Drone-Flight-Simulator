# Hover — 3D Drone Flight Simulator

Java desktop app (Team 1, Northeastern Programming Design Paradigms) that simulates drones in a 3D world with real physics.

Full requirements: `.claude/docs/SRS.md` (text) / `.claude/docs/SRS.pdf` (original). Read the SRS before designing a feature.

## Key constraints from the SRS

- **Stack:** Java (JDK 25), JavaFX 25, Maven + Maven Wrapper, JUnit 5, JaCoCo (>= 0.8.14), IntelliJ IDEA.
- **Architecture:** strict MVC. The model has no UI code; views get read-only snapshots and never modify the model.
- **Patterns in use:** Strategy, State, Command, Observer, Template Method, Builder, Decorator, Facade, Null Object.
- **Physics:** custom engine behind a `PhysicsEngine` interface; semi-implicit Euler, fixed 0.01 s step; custom immutable `Vector3D`.
- **Determinism:** same scenario gives the same result; all randomness from a seeded generator.
- **Events:** Observer pattern via `SimulationListener`, immutable event records, delivered at end of step; one failing listener must not stop the simulation.
- **Quality bar:** >= 80% line coverage on the model, every model class unit tested, Javadoc on every public class and method.
- **Input validation:** reject invalid input (negative mass, throttle > 100%) with clear errors.
- **Workflow:** branch per task, protected `main`, PRs need 2 code owner approvals (see `.github/CODEOWNERS`), CI on macOS and Windows. The SRS says one review; the team raised it to 2.

## Out of scope

Collision bouncing (collisions = crashes), full rigid-body rotation, turbulence/ground effect/terrain, networking, real hardware.
