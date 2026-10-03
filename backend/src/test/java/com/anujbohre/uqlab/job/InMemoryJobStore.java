package com.anujbohre.uqlab.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Test double for {@link JobStore}: keeps jobs in a list, newest last. No database needed. */
class InMemoryJobStore implements JobStore {

    private final List<SimulationJob> jobs = new ArrayList<>();

    @Override
    public SimulationJob save(SimulationJob job) {
        if (!jobs.contains(job)) {
            jobs.add(job);
        }
        return job;
    }

    @Override
    public Optional<SimulationJob> findById(String id) {
        return jobs.stream().filter(j -> j.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<SimulationJob> findByIdAndOwner(String id, String ownerId) {
        return findById(id).filter(j -> ownerId.equals(j.getOwnerId()));
    }

    @Override
    public PageResult<SimulationJob> findPage(String ownerId, JobStatus status, int page, int size) {
        List<SimulationJob> matching = new ArrayList<>();
        for (int i = jobs.size() - 1; i >= 0; i--) { // newest first
            boolean mine = ownerId.equals(jobs.get(i).getOwnerId());
            if (mine && (status == null || jobs.get(i).getStatus() == status)) {
                matching.add(jobs.get(i));
            }
        }
        int from = Math.min(page * size, matching.size());
        int to = Math.min(from + size, matching.size());
        int totalPages = (matching.size() + size - 1) / size;
        return new PageResult<>(matching.subList(from, to), page, size, matching.size(), totalPages);
    }

    @Override
    public List<SimulationJob> findByStatusIn(Collection<JobStatus> statuses) {
        return jobs.stream().filter(j -> statuses.contains(j.getStatus())).toList();
    }

    int count() {
        return jobs.size();
    }
}
