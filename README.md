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
| 2 | Asynchronous jobs, job status, persistence (JPA + H2), pagination, two more models | done |
| 3 | React frontend: run form, jobs list with live status, result view with histogram | done |
| 4 | Authentication: register, sign in, JWT, each user sees only their own jobs | done |
| 5 | PostgreSQL and Flyway migrations (optional "postgres" profile; H2 stays the default) | done |
| 6 | Docker Compose and GitHub Actions CI | **this version** |
| 7 | Documentation and demonstration | planned |

## Tech stack (Phase 5)

Backend: Java 25, Spring Boot 3.5, Maven, Jakarta Bean Validation, Spring Data JPA (Hibernate) with an H2 file database by default or PostgreSQL with Flyway migrations (profile `postgres`),
Spring Security with JWT (HS256, via Spring's own Nimbus support) and BCrypt, springdoc-openapi (Swagger UI), JUnit 5, MockMvc and Awaitility.

Frontend: React 19, Vite 8 (development server and build), Vitest and Testing Library. The histogram is drawn as plain SVG, with no chart library.

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

## Running the web page (Phase 4)

1. Start the backend first (see above); it must be listening on port 8080.
2. Open PowerShell and run:

```
cd C:\Dev\uq-lab\frontend
npm install
npm run dev
```

3. Open <http://localhost:5173>. You see a sign-in page: choose **Create account**, pick a username and a password of at least 8 characters, and you are signed in. The first `npm install` needs internet and takes about a minute.
4. Choose a model, adjust the numbers if you wish, and press **Run simulation**. The job appears in the list, changes from Queued to Running to Done by itself, and the result view shows the statistics and the histogram.
5. Stop the page server with Ctrl+C.

While `npm run dev` runs, every URL that starts with `/api` is forwarded to port 8080 (see `frontend/vite.config.js`), so the browser talks to one address and the backend needs no CORS settings.

Frontend tests: `npm test` inside the `frontend` folder. Expected: 50 tests pass.

## Running the tests

Right-click `backend/src/test/java` and choose **Run 'All Tests'**. Expected: 98 tests pass
(36 on the calculation core and models, 14 on the job service, 21 on the job and simulation API, 23 on login and security, 4 guarding the database migration scripts).

## Running everything with Docker (Phase 6)

Requires Docker Desktop to be running. This starts PostgreSQL, the backend and the web page together.

1. In the project root, copy `.env.example` to `.env` and set both values in it (a database password and a login-token secret of at least 32 characters). `.env` is ignored by Git.
2. Run `docker compose up --build`. The first build downloads images and libraries and can take several minutes.
3. Open <http://localhost:3000>, create an account and run a simulation. Swagger UI is at <http://localhost:8080/swagger-ui.html>.
4. Stop with Ctrl+C, then `docker compose down`. The database is kept in a Docker volume (`docker compose down -v` also deletes it).

Do not run this at the same time as the IntelliJ backend: both use port 8080.

**Continuous integration:** `.github/workflows/ci.yml` runs on every push: backend tests (Maven), frontend tests and build (npm), and a build of the Docker images.

## Using PostgreSQL (Phase 5)

By default the application uses the H2 file database, so nothing needs installing. To use PostgreSQL instead:

1. Create an empty database: in **pgAdmin** right-click Databases, Create, Database, name it `uqlab`. (Or in `psql`: `CREATE DATABASE uqlab;`)
2. In IntelliJ open **Run, Edit Configurations**, select `UqLabApplication`, and under **Environment variables** enter (separated by semicolons):
   `SPRING_PROFILES_ACTIVE=postgres;UQLAB_DB_PASSWORD=<your postgres password>`
   Optional extras: `UQLAB_DB_HOST` (default localhost), `UQLAB_DB_PORT` (5432), `UQLAB_DB_NAME` (uqlab), `UQLAB_DB_USER` (postgres).
3. Run the application. Flyway creates the tables from `src/main/resources/db/migration` (V1 tables, V2 indexes) and records them in the table `flyway_schema_history`. Hibernate then only validates that the tables match the entities.
4. The new database is empty: register a fresh account. Data in the H2 file is not copied over.

Never write the password into a file that is committed to Git.

## Signing in (Phase 4)

- `POST /api/auth/register` with `{"username","password"}` creates an account and returns a token (`201`); `409` if the name is taken.
- `POST /api/auth/login` returns a token (`200`); `401 Invalid username or password` for a wrong password or unknown user (the same message for both).
- `GET /api/auth/me` returns the signed-in user.
- Every other `/api` call needs the header `Authorization: Bearer <token>`. In Swagger UI use the **Authorize** button and paste the `accessToken`.
- Each user sees only their own jobs; another user's job answers `404`. Jobs created before Phase 4 have no owner and are visible to nobody.
- Tokens last 60 minutes (`uqlab.security.token-ttl-minutes`). There are no refresh tokens: after expiry the user signs in again.
- **Signing secret.** Set the environment variable `UQLAB_JWT_SECRET` (at least 32 characters) to keep logins valid across restarts. If it is not set, a random key is used and everyone is signed out whenever the server restarts; this is fine for local use. Never commit the secret to Git.

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
frontend/src/
  api/client.js        the only file that talks to the backend; turns errors into ApiError
  lib/                 pure helpers: number formatting, request building and checking, form presets
  hooks/usePolled.js   loads data and keeps reloading it while a job is unfinished
  components/          RunForm, DistributionField, JobsPanel, JobDetail, Histogram, StatusBadge
  App.jsx              the page: loads the models, lays out the three areas
```

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
- Tokens are kept in `sessionStorage`, which scripts on the page can read; an httpOnly cookie would resist cross-site scripting better. There are no refresh tokens, no password reset, no e-mail check and no login rate limit.
- The page checks values before sending them, but the backend repeats every check; the browser is never trusted.
- The page asks the server for news every two seconds while a job is unfinished (polling). Push updates (WebSocket or server-sent events) would remove the repeated requests.
- Inputs are sampled independently; correlated inputs are not supported.
