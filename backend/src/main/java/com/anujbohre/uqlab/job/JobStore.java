package com.anujbohre.uqlab.job;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Storage port for jobs. The service depends on this interface only, so it can be tested with an
 * in-memory fake and the database technology can change without touching the service.
 */
public interface JobStore {

    SimulationJob save(SimulationJob job);

    Optional<SimulationJob> findById(String id);

    /** Newest first. {@code status} may be null to list every job. */
    PageResult<SimulationJob> findPage(JobStatus status, int page, int size);

    List<SimulationJob> findByStatusIn(Collection<JobStatus> statuses);
}
