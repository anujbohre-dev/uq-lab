package com.anujbohre.uqlab.api;

import com.anujbohre.uqlab.api.dto.JobResponse;
import com.anujbohre.uqlab.api.dto.SimulationRequest;
import com.anujbohre.uqlab.job.JobService;
import com.anujbohre.uqlab.job.JobStatus;
import com.anujbohre.uqlab.job.PageResult;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Asynchronous simulations: submit, poll for status and result, list with pagination. */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /** Queues a simulation. Returns 202 Accepted with the job; poll {@code Location} for the result. */
    @PostMapping
    public ResponseEntity<JobResponse> submit(@Valid @RequestBody SimulationRequest request) {
        JobResponse job = jobService.submit(request);
        return ResponseEntity.accepted().location(URI.create("/api/jobs/" + job.id())).body(job);
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable String id) {
        return jobService.get(id);
    }

    /** Newest first. {@code status} filters; {@code page} starts at 0; {@code size} is 1 to 100. */
    @GetMapping
    public PageResult<JobResponse> list(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return jobService.list(status, page, size);
    }
}
