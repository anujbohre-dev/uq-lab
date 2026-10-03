package com.anujbohre.uqlab.job;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data generates the implementation at start-up from the method names (for example
 * {@code findByStatus} becomes "select ... where status = ?").
 */
public interface JobRepository extends JpaRepository<SimulationJob, String> {

    Optional<SimulationJob> findByIdAndOwnerId(String id, String ownerId);

    Page<SimulationJob> findByOwnerId(String ownerId, Pageable pageable);

    Page<SimulationJob> findByOwnerIdAndStatus(String ownerId, JobStatus status, Pageable pageable);

    List<SimulationJob> findByStatusIn(Collection<JobStatus> statuses);
}
