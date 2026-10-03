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

    /** Any job, whoever owns it. Only for the background worker, never for a request from a user. */
    Optional<SimulationJob> findById(String id);

    /** The job only if it belongs to {@code ownerId}; empty for a missing job and for someone else's. */
    Optional<SimulationJob> findByIdAndOwner(String id, String ownerId);

    /** One user's jobs, newest first. {@code status} may be null to list every status. */
    PageResult<SimulationJob> findPage(String ownerId, JobStatus status, int page, int size);

    List<SimulationJob> findByStatusIn(Collection<JobStatus> statuses);
}
