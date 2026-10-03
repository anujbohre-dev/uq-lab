package com.anujbohre.uqlab.api;

import com.anujbohre.uqlab.auth.InvalidCredentialsException;
import com.anujbohre.uqlab.auth.InvalidRegistrationException;
import com.anujbohre.uqlab.auth.UsernameTakenException;
import com.anujbohre.uqlab.domain.montecarlo.NoValidSamplesException;
import com.anujbohre.uqlab.job.JobNotFoundException;
import com.anujbohre.uqlab.job.JobQueueFullException;
import com.anujbohre.uqlab.service.InvalidSimulationRequestException;
import com.anujbohre.uqlab.service.ModelNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns exceptions into consistent JSON error bodies (RFC 7807 problem details). */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Bean-validation failure, e.g. samples below 100. Lists every failing field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onValidationFailure(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Invalid request");
        problem.setProperty("errors", errors);
        return problem;
    }

    /** Body is not valid JSON, or contains a value of the wrong type or an unknown enum constant. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail onUnreadableBody(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request body is not valid JSON or contains an unsupported value");
        problem.setTitle("Malformed request");
        return problem;
    }

    @ExceptionHandler(InvalidSimulationRequestException.class)
    public ProblemDetail onInvalidRequest(InvalidSimulationRequestException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid simulation request");
        return problem;
    }

    @ExceptionHandler(ModelNotFoundException.class)
    public ProblemDetail onModelNotFound(ModelNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Model not found");
        return problem;
    }

    @ExceptionHandler(NoValidSamplesException.class)
    public ProblemDetail onNoValidSamples(NoValidSamplesException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("No valid samples");
        return problem;
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ProblemDetail onJobNotFound(JobNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Job not found");
        return problem;
    }

    @ExceptionHandler(JobQueueFullException.class)
    public ProblemDetail onQueueFull(JobQueueFullException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Job queue full");
        return problem;
    }

    /** Registration with a name that already exists. */
    @ExceptionHandler(UsernameTakenException.class)
    public ProblemDetail onUsernameTaken(UsernameTakenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Username already taken");
        return problem;
    }

    /** Wrong username or password; the same message for both, on purpose. */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail onInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Sign-in failed");
        return problem;
    }

    @ExceptionHandler(InvalidRegistrationException.class)
    public ProblemDetail onInvalidRegistration(InvalidRegistrationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid registration");
        return problem;
    }
}
