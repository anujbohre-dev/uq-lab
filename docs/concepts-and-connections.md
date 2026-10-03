# UQ Lab: Concepts, Tools and How the Phases Connect

This guide explains the project from the beginning. It defines each concept, says what each tool does, and shows how every phase builds on the one before. Read Part 1 first, then Part 3 phase by phase.

---

## Part 1. The whole system in one picture

### 1.1 What the application does
A **Monte Carlo simulation** repeats a calculation many times with randomly drawn inputs. Real engineering inputs are never exact: a steel rod's Young's modulus, a column's diameter and length all vary a little. Instead of one answer, the program produces a **distribution** of answers.

Example: the Euler formula gives the critical buckling load of a column, P = (pi squared x E x I) / (K x L) squared, where I = (pi x d to the fourth) / 64. Each of E, d, L and K is given a probability distribution. The server draws, for example, 10,000 sets of values, evaluates the formula for each, and reports the mean, the standard deviation, percentiles and a histogram.

### 1.2 The three tiers
- **Client (browser):** the React page. It shows forms and results and sends requests.
- **Server (backend):** the Java Spring Boot program. It checks requests, runs the simulation, enforces login and talks to the database.
- **Database:** PostgreSQL (or H2). It stores users and jobs permanently.

The client and server speak **HTTP** and exchange **JSON** (text with named fields). This style is called a **REST API**: each URL (such as `/api/jobs`) names a resource, and the HTTP method (GET, POST) says what to do with it.

### 1.3 The journey of one request: "Run simulation"
1. You press **Run simulation**. React builds a JSON body and sends `POST /api/jobs` with the header `Authorization: Bearer <token>`.
2. In development, the **Vite proxy** forwards `/api` calls to port 8080. In Docker, **nginx** does the same job. The browser therefore sees one address and no CORS setup is needed.
3. In the backend, the **security filter** verifies the token (signature, expiry, issuer). If invalid, it answers 401.
4. The **controller** (`JobController`) receives the JSON. **Bean Validation** checks it. The user id is read from the token.
5. The **service** (`JobService`) saves a new row with status QUEUED and the owner id, hands the work to a **thread pool**, and returns **202 Accepted** at once.
6. A **worker thread** loads the job, sets RUNNING, runs the **Monte Carlo engine**, saves the result as JSON, and sets DONE (or FAILED with a message).
7. The page **polls** `GET /api/jobs/{id}` every two seconds until DONE, then draws the histogram.

---

## Part 2. The tools: what each is and why it is here

### Java and the backend
- **Java 25 / JDK:** the language and its development kit. The JDK compiles and runs the code.
- **Maven (`pom.xml`):** a build tool. The `pom.xml` lists libraries (dependencies) and how to build. Maven downloads them into `C:\Users\<you>\.m2`.
- **Spring Boot:** a framework that wires a web application together automatically (embedded Tomcat web server, JSON handling, configuration). You write controllers and services; it handles the plumbing. **Dependency injection** is its core idea: a class declares what it needs in its constructor and Spring supplies it.
- **Spring Data JPA / Hibernate:** JPA is the Java standard for mapping classes to database tables. Hibernate implements it. An `@Entity` class becomes a table; a `Repository` interface gives save and find methods without SQL.
- **Spring Security:** the framework for login and access rules. Here it checks JWT tokens and hashes passwords with BCrypt.
- **springdoc / Swagger UI:** reads your controllers and builds a web page (`/swagger-ui.html`) for trying the API.
- **JUnit 5, MockMvc, Awaitility:** test tools. JUnit runs tests; MockMvc calls controllers without starting a server; Awaitility waits for asynchronous results.
- **IntelliJ IDEA:** the editor and runner for the backend.

### Frontend
- **Node.js and npm:** Node runs JavaScript outside a browser. npm downloads libraries into `node_modules` and runs scripts from `package.json`. `npm ci` installs exactly what `package-lock.json` records.
- **React:** a library for building user interfaces from **components** (functions that return markup). State changes redraw the page.
- **Vite:** the development server (hot reload on port 5173) and the production build tool (`npm run build` creates the `dist` folder).
- **Vitest + Testing Library + jsdom:** run frontend tests in a simulated browser.

### Database tools
- **H2:** a small Java database stored in a file. Needs no installation. Default for quick runs.
- **PostgreSQL:** a production-grade relational database. A **relational database** stores data in tables with rows and columns, linked by keys, and is queried with **SQL**.
- **pgAdmin:** a graphical program to look inside PostgreSQL.
- **Flyway:** applies numbered SQL files (`V1__...sql`, `V2__...sql`) in order, once each, and records them in `flyway_schema_history`.

### Delivery tools
- **Git and GitHub:** Git records versions of the project; GitHub stores them online. A **commit** is a saved version; a **tag** (`phase-4`) names a commit.
- **Docker:** packages a program with everything it needs into an **image**; a running image is a **container**. Images are built from a **Dockerfile**.
- **Docker Compose:** one file (`docker-compose.yml`) that starts several containers together and connects them.
- **nginx:** a web server. Here it serves the built page and forwards `/api` to the backend.
- **GitHub Actions:** runs a script (`ci.yml`) on GitHub's machines at every push. This is **continuous integration (CI)**.

---

## Part 3. Phase by phase

### Phase 1: the calculation core and a first endpoint
**Built:** the `domain` package (plain Java, no Spring): distributions (normal, log-normal, uniform, constant), the Euler buckling model, the seeded Monte Carlo engine, statistics (mean, standard deviation, percentiles, histogram), and a synchronous endpoint `POST /api/simulations`.

**Concepts:**
- **Probability distribution:** a rule for how likely each value is. Normal is the bell curve; log-normal never goes negative (good for stiffness); uniform means every value in a range is equally likely.
- **Seed:** the starting number of a random generator. Same seed, same numbers, so results are reproducible and testable.
- **Standard error:** standard deviation divided by the square root of the number of samples; it measures how precisely the mean is known.
- **Strategy pattern:** each model implements one interface, `SimulationModel`, so a new model is one new class.
- **Separation of concerns:** `domain` has no web or database code, so it can be tested alone.

**Limitation that motivates Phase 2:** the endpoint waits until the whole simulation finishes, and nothing is stored.

### Phase 2: asynchronous jobs and persistence
**Built:** `POST /api/jobs` (returns 202), `GET /api/jobs/{id}`, `GET /api/jobs` with paging; a thread pool; the `SimulationJob` entity; the `JobStore` interface with a JPA implementation; H2 file storage; two more models.

**Concepts:**
- **Asynchronous processing:** accept the request, do the work in the background, let the client ask later.
- **Thread pool:** a fixed number of worker threads (here 2) and a queue (100). A full queue gives **503 Service Unavailable**, which protects the server.
- **Job lifecycle:** QUEUED, RUNNING, DONE or FAILED. Jobs left unfinished by a restart are marked FAILED at start-up.
- **Port and adapter:** `JobStore` is the port (what the service needs); `JpaJobStore` is the adapter (how it is done). Tests use an in-memory adapter.
- **Pagination:** returning results in pages (`page`, `size`) so lists stay fast.

**Connection to Phase 1:** the same engine runs unchanged; Phase 2 only wraps it in a job.

### Phase 3: the React frontend
**Built:** the run form, the jobs list with status badges, the result view with an SVG histogram, polling, client-side validation, a fake backend for tests.

**Concepts:**
- **Component, state, effect:** a component draws part of the page; state is data that changes; effects run code such as fetching.
- **Polling:** asking the server repeatedly (`usePolled`). Simple, but wasteful compared with server push.
- **Proxy:** Vite forwards `/api` to port 8080, avoiding **CORS** (the browser rule that blocks calls to another origin).
- **Validation twice:** the page checks for convenience; the server checks for safety. The browser is never trusted.

**Connection to Phase 2:** the page uses exactly the Phase 2 endpoints. Nothing in the backend changed.

### Phase 4: authentication and ownership
**Built:** the `auth` package (`AppUser`, `UserService`, `TokenService`, `SecurityConfig`, `AuthController`); `ownerId` on jobs; owner-filtered queries; the sign-in page; token handling in the client.

**Concepts:**
- **Authentication:** proving who you are (login). **Authorization:** deciding what you may do (only your jobs).
- **Hashing vs encryption:** hashing is one way; encryption can be reversed. Passwords are hashed with **BCrypt** (slow and salted, so stolen hashes are costly to crack). Passwords are never stored or decrypted.
- **JWT (JSON Web Token):** three Base64 parts: header, claims (user id, name, issuer, expiry), and a signature made with a secret key. Anyone can read it; only the server can sign it. Never put secrets in it.
- **Stateless:** the server keeps no session; it re-verifies the token every time.
- **Bearer token:** sent as `Authorization: Bearer <token>`.
- **404 instead of 403** for someone else's job, so job ids are not revealed.
- **Environment variable `UQLAB_JWT_SECRET`:** keeps the key out of the code.

**Connection to Phases 1 to 3:** the calculation core is untouched. `JobService` and `JobController` gained an owner argument. The tests gained a signed-in user. The page gained a gate: no token, sign-in page.

### Phase 5: PostgreSQL and Flyway
**Built:** the `postgres` profile, `application-postgres.properties`, migrations `V1` and `V2`, and the migration guard test.

**Concepts:**
- **Migration:** a versioned change to the database schema. Flyway never re-runs an applied file and detects edits by checksum.
- **`ddl-auto=update` vs `validate`:** `update` lets Hibernate change tables silently (acceptable on a laptop, unsafe in production); `validate` only checks.
- **Profile:** a named set of configuration, chosen by `SPRING_PROFILES_ACTIVE`.
- **Index:** a lookup structure that speeds queries; `(owner_id, created_at DESC)` serves "my newest jobs".

**Connection to Phase 2 and 4:** the same entities and repositories run on either database. Only configuration differs.

### Phase 6: Docker Compose and CI
**Built:** two Dockerfiles, `nginx.conf`, `docker-compose.yml`, `.env.example`, `ci.yml`.

**Concepts:**
- **Multi-stage build:** build tools stay in a throw-away stage; the final image holds only the result.
- **Compose networking:** services find each other by name (`postgres`, `backend`).
- **Health check and `depends_on`:** the backend starts only when the database is ready.
- **Volume:** storage that outlives a container.
- **CI:** every push is built and tested automatically.

**Connection to Phase 3 and 5:** nginx replaces the Vite proxy; the `postgres` profile from Phase 5 is what the container uses.

### Phase 7: documentation and demonstration
**Built:** the README with screenshots and diagram, the demo script, the interview guide, this guide.

**Why it matters:** a project is judged by how clearly it is explained.

---

## Part 4. How the layers stay independent

- `domain` does not know Spring, HTTP or the database.
- `service` and `job` do not know whether storage is H2 or PostgreSQL (the `JobStore` port).
- `api` does not know how the simulation runs.
- The page does not know how the server is built; it knows only the URLs and JSON shapes.

This is why each phase could be added with small, local changes.

---

## Part 5. Glossary

- **API:** the agreed set of URLs and JSON shapes through which programs talk.
- **Bean:** an object managed by Spring.
- **Controller:** a class that receives HTTP requests.
- **DTO / record:** a plain data carrier, such as `SimulationRequest`.
- **Entity:** a class mapped to a database table.
- **Endpoint:** one URL plus method, such as `POST /api/jobs`.
- **HTTP status:** 200 OK, 201 Created, 202 Accepted, 400 invalid input, 401 not signed in, 404 not found, 409 conflict, 503 busy.
- **Mock / fake:** a stand-in used in tests.
- **Repository:** an interface for saving and finding entities.
- **Transaction:** a group of database changes that succeed or fail together.

---

## Part 6. Questions to test yourself

1. Trace "Run simulation" from the click to the histogram, naming each layer.
2. Why does the server answer 202 and not 200?
3. What would break if the thread pool had no queue limit?
4. What is in a JWT, and why is it safe to send?
5. Why BCrypt and not a plain hash?
6. What does Flyway do that `ddl-auto=update` does not?
7. What changed in `JobService` between Phase 2 and Phase 4, and why?
8. Why does nginx forward `/api`?
9. Which parts of the project need no change when switching from H2 to PostgreSQL, and why?
10. What are three weaknesses of this project, and how would you fix each?
