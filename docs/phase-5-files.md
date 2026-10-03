# Phase 5: PostgreSQL and Flyway

Phase 5 lets the same application run on PostgreSQL with a controlled, versioned database schema.
H2 remains the default so that tests and quick runs need nothing installed.

## New and changed files

- `backend/src/main/resources/db/migration/V1__create_tables.sql`: creates `app_user` and `simulation_job` exactly as the JPA entities describe them.
- `backend/src/main/resources/db/migration/V2__job_indexes.sql`: indexes for "my jobs, newest first" and "my jobs by status".
- `backend/src/main/resources/application-postgres.properties`: used only with the profile `postgres`. Connection from environment variables, Flyway on, `ddl-auto=validate`.
- `backend/src/main/resources/application.properties` and the test properties: Flyway explicitly off.
- `backend/pom.xml`: PostgreSQL driver, `flyway-core`, `flyway-database-postgresql`.
- `backend/src/test/java/.../db/MigrationFilesTest.java`: 4 tests that fail if an entity gains a column the migration lacks, or if lengths differ.

## Why this matters

- `ddl-auto=update` lets Hibernate change the schema silently at start-up. It cannot rename or drop, it is unreviewed, and two servers can race. It is acceptable for a laptop and unsafe for production.
- A migration is a numbered SQL file. Flyway applies each file once, in order, and stores a checksum. Editing an applied file makes start-up fail, so history cannot be rewritten; a change is a new file (V3, V4, ...).
- `ddl-auto=validate` turns Hibernate into a checker: it refuses to start if tables and entities disagree.
- Profiles (`SPRING_PROFILES_ACTIVE=postgres`) select configuration without code changes. Secrets come from environment variables.

## Honest note on verification

The migration guard tests and the rest of the code could not be run in the build environment (no access
to Maven libraries). The real check is: run the tests in IntelliJ (expect 98 passing), then start the
application with the `postgres` profile and confirm in pgAdmin that the tables and `flyway_schema_history` exist.
If the start fails, send the first red lines of the log.

## Concepts to be able to explain

- Schema migration, versioning and checksums; why not `update` in production.
- Profile-based configuration and environment variables for secrets.
- Why `TIMESTAMP WITH TIME ZONE` is used for instants.
- Composite indexes: `(owner_id, created_at DESC)` serves the exact query the jobs list issues.
