# Phase 6: Docker Compose and GitHub Actions

## Files

- `backend/Dockerfile`: two stages. Stage 1 builds the jar with Maven; stage 2 copies only the jar into a small Java runtime image and runs as a non-root user.
- `frontend/Dockerfile` and `frontend/nginx.conf`: stage 1 builds the static files; stage 2 serves them with nginx, which forwards `/api` (and Swagger) to the backend container. This is the production counterpart of the Vite proxy used in development.
- `docker-compose.yml`: three services: `postgres` (with a health check and a named volume), `backend` (profile `postgres`, waits until the database is healthy) and `frontend` (port 3000).
- `.env.example`: the two values you must set (`POSTGRES_PASSWORD`, `UQLAB_JWT_SECRET`). Copy to `.env`; Git ignores `.env`. `.gitignore` was adjusted so `.env.example` itself is committed.
- `.github/workflows/ci.yml`: three jobs on every push and pull request: backend tests, frontend tests and build, and a Docker image build.
- `.dockerignore` files keep `node_modules`, `target` and local data out of the images.

## Honest note on verification

`docker compose config` accepted the compose file and the workflow parsed as valid YAML. The images could
not be built in the build environment (no running Docker engine, no Maven access). The first real build is on
your laptop; if it fails, send the last 30 lines of the output.

## Concepts to be able to explain

- Image versus container; layers and caching (why `pom.xml` is copied before the source).
- Multi-stage builds: build tools stay out of the final image, which is smaller and safer.
- Compose networking: services reach each other by name (`postgres`, `backend`).
- Health check plus `depends_on: service_healthy`: start order that means "ready", not just "started".
- Secrets in `.env`, never in the repository or the image.
- Continuous integration: every push is built and tested automatically, so a broken change is seen at once.
- Why nginx forwards `/api`: one origin for the browser, so no CORS configuration.
