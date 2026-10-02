# Phase 1: what each file is for

## Project root
- `README.md` - what the project is, how to run and test it, the API, and how correctness is checked.
- `.gitignore` - tells Git which files never to commit (build output, IDE files, secrets).
- `docs/phase-1-files.md` - this file.

## backend/
- `pom.xml` - Maven build file: Java version, Spring Boot version, and the list of dependencies.
- `requests.http` - ready-made API requests you can run directly inside IntelliJ.

### backend/src/main/resources
- `application.properties` - settings: port 8080, health endpoint, Swagger UI path.

### backend/src/main/java/com/anujbohre/uqlab
- `UqLabApplication.java` - the entry point; `main` starts Spring and the embedded web server.
- `SimulationConfig.java` - registers the model and the engine as Spring beans.

### .../domain/distribution (plain Java, no Spring)
- `Distribution.java` - interface: draw a sample, report mean and standard deviation.
- `StandardNormal.java` - generates N(0,1) numbers (Box-Muller); shared by the normal and log-normal classes.
- `NormalDistribution.java` - Gaussian distribution.
- `UniformDistribution.java` - equal probability between a minimum and a maximum.
- `LogNormalDistribution.java` - always-positive distribution, defined by the variable's own mean and standard deviation.
- `ConstantDistribution.java` - a fixed value; used for inputs that are not uncertain.

### .../domain/model
- `SimulationModel.java` - interface every physical model implements (the Strategy pattern).
- `InputDescriptor.java` - name, unit and description of one model input.
- `EulerColumnBucklingModel.java` - the Euler critical-load formula for a solid circular column.

### .../domain/montecarlo
- `MonteCarloEngine.java` - the simulation loop: sample inputs, evaluate the model, summarise.
- `Statistics.java` - mean, standard deviation, percentiles and histogram calculations.
- `SimulationResult.java` - the result record the engine returns.
- `NoValidSamplesException.java` - raised when every sample was non-physical.

### .../service
- `SimulationService.java` - applies defaults, builds distributions, runs the engine, builds the response.
- `DistributionFactory.java` - turns the JSON distribution description into a domain distribution.
- `ModelRegistry.java` - finds a model by id; Spring injects all models into it.
- `ModelNotFoundException.java` - unknown model id (becomes HTTP 404).
- `InvalidSimulationRequestException.java` - sensible JSON but invalid content (becomes HTTP 400).

### .../api
- `SimulationController.java` - the two endpoints: list models, run a simulation.
- `ApiExceptionHandler.java` - converts exceptions into consistent JSON error responses.

### .../api/dto (shapes of the JSON)
- `SimulationRequest.java` - request body with validation rules.
- `DistributionSpec.java` - JSON form of one input's distribution.
- `DistributionType.java` - NORMAL, UNIFORM, LOGNORMAL, CONSTANT.
- `SimulationResponse.java` - response body: summary, percentiles, histogram.
- `ModelInfo.java` - description of a model for `GET /api/models`.

## backend/src/test/java/com/anujbohre/uqlab
- `domain/model/EulerColumnBucklingModelTest.java` - formula against hand calculations and scaling laws.
- `domain/distribution/DistributionTest.java` - sample statistics, bounds, reproducibility, invalid parameters.
- `domain/montecarlo/StatisticsTest.java` - mean, standard deviation, percentile and histogram helpers.
- `domain/montecarlo/MonteCarloEngineTest.java` - closed-form match, reproducibility, convergence, rejection handling.
- `api/SimulationControllerTest.java` - the HTTP API end to end, including error responses.
