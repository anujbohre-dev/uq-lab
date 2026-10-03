package com.anujbohre.uqlab.job;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data generates the implementation at start-up from the method names (for example
 * {@code findByStatus} becomes "select ... where status = ?").
 */
public interface JobRepository extends JpaRepository<SimulationJob, String> {

    Page<SimulationJob> findByStatus(JobStatus status, Pageable pageable);

    List<SimulationJob> findByStatusIn(Collection<JobStatus> statuses);
}
