## Workload Split

| Member | Owns (model + tests) | Also owns (UI / infra) | SRS FRs |
|---|---|---|---|
| Sophakotra Son (@Ulton321) | Simulation engine: fixed-step loop, pause/speed/reset, `WorldSnapshot`/`DroneSnapshot`, step order (documented in Javadoc), determinism test | 3D view (orbit camera, lighting, town, drone models, trails, labels), view switching, main window, `HoverFacade`, demo scenario and demo video, end-to-end FR scenario tests | 2, 17 (3D), 18, 19 |
| Taha Oguzhan Ucar (@ozi14) | Physics: `PhysicsEngine`, `OwnPhysicsEngine`, gravity, drag, air density by altitude, seeded gusty wind, ground contact (reports touchdown speed only), accuracy tests | Flight controllers: hover, altitude hold, waypoint, return home, fixed-wing (`LiftForce`, stall); design document; stretch goal (RK4 or ode4j adapter) | 3, 4, 5, 6, 9 |
| Muhammed Bilal (@Attested-paper51) | World: obstacles, no-fly zones, collision detection (collision = crash); events: `EventBus`, `SimulationListener`, immutable events delivered at end of step, listener isolation; auto-policies (return home on low battery) | Event log panel; control panel (start, pause, reset, speed, add/remove drones, wind settings); drone status panel; 2D top-down and side views; console mode; UML diagrams (Mermaid) | 13, 14, 15, 16, 17 (2D, status, log) |
| Vamsi Mannava (@VamsiMannava03) | Drones: `Vector3D`, drone types (quadcopter, hexacopter, `FixedWingDrone`), `DroneBuilder`/`DroneFactory`, battery drain, payload, sensor decorators, lifecycle states (State pattern; emits take-off, landing, crash, battery and mission events); missions: Command steps, `MissionPlan`, `MissionValidator`, mission file loader/writer, mission abort | Mission editor; Maven Wrapper, `.gitignore`, GitHub Actions CI (macOS + Windows, JaCoCo gate), README, coverage report | 1, 7, 8, 10, 11, 12 |

### Shared rules

- **Interfaces first (Week 1 stub PRs):** Vamsi: `Drone`, `DroneState`, `MissionStep`. Taha: `FlightController` (`PhysicsEngine` is done). Bilal: `SimulationListener` and event records. Sophakotra: snapshots and step order. Implementations follow, with Null Object stubs so nobody is blocked.
- **Event ownership:** drone lifecycle, battery and mission events come from Vamsi's drones. Collision and zone events come from Bilal's world. Taha's physics never publishes events or decides a crash.
- **Return home on low battery:** Bilal's policy sends a command, Vamsi's mission aborts, and Taha's controller flies the drone home.
- **Determinism:** all randomness comes from the seeded generator, with no unordered collections in the step path.
- **Javadoc:** each person documents their own packages.
- **Review:** `CODEOWNERS` covers the whole team, so "owns" means primary author, not sole reviewer. Every PR still needs 2 approvals.
