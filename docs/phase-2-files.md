# Phase 2: what changed

New and changed files compared with Phase 1. Everything not listed is unchanged.

## Changed
- `README.md` - status, stack, endpoints, models, limitations updated.
- `.gitignore` - ignores the local `data/` database folder.
- `backend/pom.xml` - adds `spring-boot-starter-data-jpa` and the H2 database.
- `backend/requests.http` - requests for jobs, listing and the two new models.
- `backend/src/main/resources/application.properties` - H2 file database and JPA settings.
- `.../SimulationConfig.java` - registers the two new models as beans.
- `.../service/SimulationService.java` - new `validate` method; checks input names against the model.
- `.../api/ApiExceptionHandler.java` - adds 404 for an unknown job and 503 for a full queue.
- `.../test/.../api/SimulationControllerTest.java` - expects three models.

## New: domain
- `domain/model/CantileverDeflectionModel.java` - tip deflection P L^3 / (3 E I).
- `domain/model/SpringMassFrequencyModel.java` - natural frequency sqrt(k/m) / (2 pi).

## New: job package
- `job/JobStatus.java` - QUEUED, RUNNING, DONE, FAILED.
- `job/SimulationJob.java` - the database entity (one row per job).
- `job/JobStore.java` - storage interface the service depends on.
- `job/JobRepository.java` - Spring Data interface; Spring writes the implementation.
- `job/JpaJobStore.java` - connects `JobStore` to `JobRepository`.
- `job/PageResult.java` - one page of results with totals.
- `job/JobExecutorConfig.java` - the thread pool (2 workers, queue of 100) and the clock.
- `job/JobService.java` - submit, get, list, run on a worker, fail interrupted jobs at start-up.
- `job/JobNotFoundException.java`, `job/JobQueueFullException.java` - error cases.

## New: API
- `api/JobController.java` - `POST /api/jobs`, `GET /api/jobs/{id}`, `GET /api/jobs`.
- `api/dto/JobResponse.java` - the job as JSON.

## New: tests
- `domain/model/CantileverDeflectionModelTest.java` (7), `SpringMassFrequencyModelTest.java` (5).
- `job/JobServiceTest.java` (11) and `job/InMemoryJobStore.java` - life cycle without a database.
- `api/JobControllerTest.java` (8) - the whole asynchronous API with a real thread pool and H2.
- `src/test/resources/application.properties` - in-memory database for tests.
