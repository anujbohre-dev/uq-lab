# Monte Carlo Uncertainty Lab (UQ Lab)

A full-stack Java project that propagates uncertain engineering inputs through a physical model
by Monte Carlo simulation and reports the statistics of the result.

**Example:** the critical buckling load of a steel column depends on Young's modulus, diameter,
length and end conditions, none of which is known exactly. Give each a probability distribution;
the server draws many random samples, evaluates the Euler formula for each, and returns the mean,
standard deviation, percentiles and a histogram of the load.

## Status

| Phase | Content | State |
|-------|---------|-------|
| 1 | Backend core: Euler model, distributions, seeded Monte Carlo, REST endpoint, tests | done |
| 2 | Asynchronous jobs, job status, persistence (JPA + H2), pagination, two more models | **this version** |
| 3 | React frontend (form, job list, histogram) | planned |
| 4 | Authentication (Spring Security, JWT) | planned |
| 5 | PostgreSQL and Flyway migrations | planned |
| 6 | Docker Compose and GitHub Actions CI | planned |
| 7 | Documentation and demonstration | planned |

## Tech stack (Phase 2)

Java 25, Spring Boot 3.5, Maven, Jakarta Bean Validation, Spring Data JPA (Hibernate) with an H2 file database,
springdoc-openapi (Swagger UI), JUnit 5, MockMvc and Awaitility.

## Running it (IntelliJ IDEA)

1. Keep the project **outside OneDrive**, for example `C:\Dev\uq-lab`.
2. In IntelliJ choose **File, Open** and select the `backend` folder (the one containing `pom.xml`). Click **Trust Project**.
3. Wait for the Maven import to finish (progress bar at the bottom). The first import downloads libraries and needs internet.
4. **File, Project Structure, Project, SDK**: choose JDK 25.
5. Open `UqLabApplication.java` and click the green triangle next to `main`. The console should end with `Started UqLabApplication`.
6. Open <http://localhost:8080/swagger-ui.html>, expand **POST /api/simulations**, click **Try it out**, paste a body from `backend/requests.http`, and click **Execute**.
7. Health check: <http://localhost:8080/actuator/health> should show `{"status":"UP"}`.
8. Stop the server with the red square in IntelliJ.

If Maven reports that Java 25 is not supported by this Spring Boot version, change `<java.version>25</java.version>` to `21` in `backend/pom.xml` and reload Maven. Nothing else needs to change.

## Running the tests

Right-click `backend/src/test/java` and choose **Run 'All Tests'**. Expected: 66 tests pass
(36 on the calculation core and models, 11 on the job service, 19 on the HTTP API).

## API

- `GET /api/models` lists models and their inputs.
- `POST /api/simulations` runs a simulation and waits for the answer (synchronous).
- `POST /api/jobs` queues the same simulation and returns `202 Accepted` at once, with a `Location` header.
- `GET /api/jobs/{id}` returns the job: `status` is `QUEUED`, `RUNNING`, `DONE` or `FAILED`; `result` appears when `DONE`, `errorMessage` when `FAILED`.
- `GET /api/jobs?status=DONE&page=0&size=20` lists jobs, newest first, with `items`, `page`, `size`, `totalItems` and `totalPages`. `status` is optional; `size` is 1 to 100.

Available models (each lists its inputs at `GET /api/models`):

- `euler-column-buckling`: critical buckling load, N.
- `cantilever-tip-deflection`: tip deflection of a cantilever with an end load, m.
- `spring-mass-natural-frequency`: natural frequency of a spring-mass system, Hz.

Jobs are stored in an H2 database file under `data/` (ignored by Git), so they survive a restart.
A job that was still `QUEUED` or `RUNNING` when the server stopped is marked `FAILED` at the next start.

Request body (all four inputs of the model are required):

```json
{
  "modelId": "euler-column-buckling",
  "samples": 100000,
  "seed": 2026,
  "bins": 20,
  "inputs": {
    "youngsModulus":         { "type": "LOGNORMAL", "mean": 200000000000, "stdDev": 10000000000 },
    "diameter":              { "type": "NORMAL", "mean": 0.05, "stdDev": 0.0005 },
    "length":                { "type": "NORMAL", "mean": 2.0, "stdDev": 0.01 },
    "effectiveLengthFactor": { "type": "UNIFORM", "min": 0.9, "max": 1.1 }
  }
}
```

- Distribution types: `NORMAL` and `LOGNORMAL` (mean, stdDev), `UNIFORM` (min, max), `CONSTANT` (value).
- `samples` is optional (default 10 000, range 100 to 1 000 000). `bins` is optional (default 20, range 2 to 200).
- `seed` is optional. If omitted, one is chosen and returned. The same seed and inputs always give identical results.
- Samples outside the model's physical domain (for example a negative length) are rejected and counted in `samplesRejected`.
- Errors use the problem-details format: 400 for invalid input, 404 for an unknown model, 422 when every sample was rejected.

## How correctness is checked

- With zero spread (all `CONSTANT`) the simulation must equal the closed-form Euler load, 151 397.835 N for E = 200 GPa, d = 50 mm, L = 2 m, K = 1. This is tested.
- The same seed must reproduce a run exactly; different seeds must differ.
- The standard error of the mean must shrink like 1/sqrt(N).
- Distribution moments, percentile interpolation and histogram counts have their own unit tests.
- The engine's statistics were also compared once against an independent NumPy implementation and agreed within sampling error.

## Design

```
api/        HTTP layer: controllers, request/response records, error handling
service/    application logic: validation, building distributions, calling the engine
job/        asynchronous jobs: entity, storage port (JobStore), JPA adapter, thread pool, JobService
domain/     plain Java, no Spring: distributions, models, Monte Carlo engine, statistics
```

The `domain` package has no framework dependency, so it can be tested and reused on its own.
Each physical model implements `SimulationModel` (Strategy pattern); adding a model means adding one class
and one `@Bean` in `SimulationConfig`.

## Limitations

- The Euler formula is valid only for slender columns; the model does not check slenderness.
- The job queue lives in memory: a restart fails unfinished jobs rather than resuming them. A multi-server deployment would need a shared queue.
- Two jobs run at a time and up to 100 wait; beyond that the API answers `503`.
- Anyone can read every job: there is no authentication until Phase 4.
- Inputs are sampled independently; correlated inputs are not supported.
