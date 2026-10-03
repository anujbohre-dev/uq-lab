package com.anujbohre.uqlab.job;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** {@link JobStore} backed by the JPA repository (H2 database in this phase). */
@Component
class JpaJobStore implements JobStore {

    private final JobRepository repository;

    JpaJobStore(JobRepository repository) {
        this.repository = repository;
    }

    @Override
    public SimulationJob save(SimulationJob job) {
        return repository.save(job);
    }

    @Override
    public Optional<SimulationJob> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<SimulationJob> findByIdAndOwner(String id, String ownerId) {
        return repository.findByIdAndOwnerId(id, ownerId);
    }

    @Override
    public PageResult<SimulationJob> findPage(String ownerId, JobStatus status, int page, int size) {
        // The id is a tie-breaker so that paging is stable when two jobs share a timestamp.
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<SimulationJob> result = status == null
                ? repository.findByOwnerId(ownerId, pageable)
                : repository.findByOwnerIdAndStatus(ownerId, status, pageable);
        return new PageResult<>(
                result.getContent(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public List<SimulationJob> findByStatusIn(Collection<JobStatus> statuses) {
        return repository.findByStatusIn(statuses);
    }
}
