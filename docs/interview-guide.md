# Interview guide: UQ Lab

Use this only for parts you have understood. If you cannot explain a part, say so honestly and describe how you would find out.

## One-minute summary
"UQ Lab is a full-stack Java application for uncertainty quantification. The user gives probability
distributions for the inputs of an engineering formula. A Spring Boot backend draws many random samples,
evaluates the formula for each, and returns statistics and a histogram. Jobs run asynchronously on a
thread pool, are stored in PostgreSQL, and belong to users who log in with JWT. A React page shows
live status and the result. It runs with Docker Compose and is tested in CI."

## Likely questions and short answers

**What is Monte Carlo simulation?**
Repeating a calculation with randomly drawn inputs many times, so that the spread of the outputs
approximates the uncertainty of the result. The standard error of the mean falls as one over the square
root of the number of samples.

**Why a seed?** The same seed gives the same sequence, so a run is reproducible and a test can assert exact numbers.

**Why asynchronous jobs?** A large run can take seconds. The request returns `202 Accepted` with a
`Location` header at once, and the client polls. The request thread is never blocked.

**Why a bounded thread pool?** Two workers and a queue of 100 limit memory and CPU. When the queue is full
the API answers `503` instead of accepting unlimited work. Unfinished jobs are marked FAILED at start-up
because the queue lives in memory.

**What is the JobStore interface for?** It separates job logic from storage (a port with a JPA adapter).
Tests use an in-memory implementation, so the service is tested without a database.

**How does login work?** Register or login returns a JWT signed with HS256. The client sends it as
`Authorization: Bearer ...`. A filter verifies signature, expiry and issuer on every request. The server
keeps no session, so it is stateless.

**Why is CSRF disabled?** CSRF attacks rely on the browser attaching credentials automatically, as it
does with cookies. This API uses a header that the page sets itself, so there is nothing to ride on.
(If tokens moved to cookies, CSRF protection would be needed again.)

**Why BCrypt?** It is slow and salted by design, so stolen hashes are expensive to crack. Hashing is one
way; encryption can be reversed.

**What is stored in the token, and is it secret?** The user id, name, issuer and times. It is signed, not
encrypted, so anyone can read it; never put secrets in it.

**Why 404 and not 403 for another user's job?** 403 would confirm that the job exists. 404 reveals nothing.

**What are the weaknesses of your token storage?** `sessionStorage` is readable by injected scripts (XSS).
An httpOnly cookie, with CSRF protection and refresh tokens, would be stronger. This is listed in the README.

**Why Flyway instead of `ddl-auto=update`?** `update` changes the schema silently and cannot rename or drop.
Flyway applies reviewed, numbered SQL files once and checks their checksums. Hibernate only validates.

**How is the project tested?** Unit tests for distributions, models and statistics; service tests with an
in-memory store; MockMvc tests for the HTTP layer including security; a fake backend for the React tests;
a guard test for the migration scripts. CI runs them on every push.

**What does Docker Compose give you?** One command starts PostgreSQL, the backend and the page. The
backend waits for the database health check. Services reach each other by name.

**What would you do next?** Refresh tokens or httpOnly cookies, a login rate limit, correlated inputs,
more models, server-sent events instead of polling, and a real-database integration test.

## Be honest about
- Which parts you built with assistance, and which you can now explain.
- What was not verified: for example, the Docker build on other machines.
