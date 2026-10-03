package com.anujbohre.uqlab.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anujbohre.uqlab.api.dto.DistributionSpec;
import com.anujbohre.uqlab.api.dto.DistributionType;
import com.anujbohre.uqlab.api.dto.JobResponse;
import com.anujbohre.uqlab.api.dto.SimulationRequest;
import com.anujbohre.uqlab.domain.model.EulerColumnBucklingModel;
import com.anujbohre.uqlab.domain.model.SpringMassFrequencyModel;
import com.anujbohre.uqlab.domain.montecarlo.MonteCarloEngine;
import com.anujbohre.uqlab.service.DistributionFactory;
import com.anujbohre.uqlab.service.InvalidSimulationRequestException;
import com.anujbohre.uqlab.service.ModelNotFoundException;
import com.anujbohre.uqlab.service.ModelRegistry;
import com.anujbohre.uqlab.service.SimulationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Tests the job life cycle without Spring or a database: the store is an in-memory fake and the
 * executor is a "manual" one that holds tasks until the test runs them, so every state can be
 * observed deterministically.
 */
class JobServiceTest {

    private static final double CLOSED_FORM = 151397.8353530265;
    private static final String OWNER = "user-1";

    /** Executor that queues tasks instead of running them; the test calls {@link #runAll()}. */
    static class ManualExecutor extends AbstractExecutorService {
        final List<Runnable> pending = new ArrayList<>();
        boolean rejecting;

        @Override
        public void execute(Runnable command) {
            if (rejecting) {
                throw new RejectedExecutionException("full");
            }
            pending.add(command);
        }

        void runAll() {
            List<Runnable> copy = new ArrayList<>(pending);
            pending.clear();
            copy.forEach(Runnable::run);
        }

        @Override
        public void shutdown() {}

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }

    /** Clock that moves forward one second every time it is read, so createdAt values differ. */
    static class TickingClock extends Clock {
        private long seconds = 0;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.EPOCH.plusSeconds(seconds++);
        }
    }

    private final InMemoryJobStore store = new InMemoryJobStore();
    private final ManualExecutor executor = new ManualExecutor();
    private final JobService jobs = new JobService(
            store,
            new SimulationService(
                    new ModelRegistry(List.of(new EulerColumnBucklingModel(), new SpringMassFrequencyModel())),
                    new DistributionFactory(),
                    new MonteCarloEngine()),
            new ObjectMapper(),
            executor,
            new TickingClock());

    private static SimulationRequest eulerRequest(double diameter) {
        Map<String, DistributionSpec> in = new LinkedHashMap<>();
        in.put("youngsModulus", constant(200e9));
        in.put("diameter", constant(diameter));
        in.put("length", constant(2.0));
        in.put("effectiveLengthFactor", constant(1.0));
        return new SimulationRequest("euler-column-buckling", in, 1000, 1L, null);
    }

    private static DistributionSpec constant(double value) {
        return new DistributionSpec(DistributionType.CONSTANT, null, null, null, null, value);
    }

    @Test
    void submitReturnsAQueuedJobBeforeAnythingRuns() {
        JobResponse job = jobs.submit(OWNER, eulerRequest(0.05));

        assertEquals(JobStatus.QUEUED, job.status());
        assertEquals("euler-column-buckling", job.modelId());
        assertTrue(job.result() == null);
        assertEquals(1, executor.pending.size());
        assertEquals(JobStatus.QUEUED, jobs.get(OWNER, job.id()).status());
    }

    @Test
    void jobCompletesWithTheClosedFormResult() {
        JobResponse queued = jobs.submit(OWNER, eulerRequest(0.05));
        executor.runAll();

        JobResponse done = jobs.get(OWNER, queued.id());
        assertEquals(JobStatus.DONE, done.status());
        assertEquals(CLOSED_FORM, done.result().summary().mean(), 1e-6);
        assertEquals(1000, done.result().samplesUsed());
        assertTrue(done.startedAt() != null);
        assertTrue(done.finishedAt() != null);
        assertTrue(done.errorMessage() == null);
        // The stored request is returned exactly as it was submitted.
        assertEquals(0.05, done.request().inputs().get("diameter").value(), 0.0);
    }

    @Test
    void jobWithNoValidSamplesEndsFailedWithAMessage() {
        // A negative diameter is accepted by the request check but is non-physical in the model,
        // so every sample is rejected and the engine reports it.
        JobResponse queued = jobs.submit(OWNER, eulerRequest(-0.05));
        executor.runAll();

        JobResponse failed = jobs.get(OWNER, queued.id());
        assertEquals(JobStatus.FAILED, failed.status());
        assertTrue(failed.errorMessage() != null && !failed.errorMessage().isBlank());
        assertTrue(failed.result() == null);
    }

    @Test
    void unknownModelIsRejectedAndNothingIsStored() {
        SimulationRequest request = new SimulationRequest("nope", eulerRequest(0.05).inputs(), null, null, null);
        assertThrows(ModelNotFoundException.class, () -> jobs.submit(OWNER, request));
        assertEquals(0, store.count());
        assertEquals(0, executor.pending.size());
    }

    @Test
    void missingAndUnknownInputsAreRejectedAndNothingIsStored() {
        Map<String, DistributionSpec> missing = new LinkedHashMap<>(eulerRequest(0.05).inputs());
        missing.remove("length");
        assertThrows(
                InvalidSimulationRequestException.class,
                () -> jobs.submit(OWNER, new SimulationRequest("euler-column-buckling", missing, null, null, null)));

        Map<String, DistributionSpec> extra = new LinkedHashMap<>(eulerRequest(0.05).inputs());
        extra.put("temperature", constant(20.0));
        assertThrows(
                InvalidSimulationRequestException.class,
                () -> jobs.submit(OWNER, new SimulationRequest("euler-column-buckling", extra, null, null, null)));

        assertEquals(0, store.count());
    }

    @Test
    void fullQueueMarksTheJobFailedAndReportsIt() {
        executor.rejecting = true;
        assertThrows(JobQueueFullException.class, () -> jobs.submit(OWNER, eulerRequest(0.05)));

        assertEquals(1, store.count());
        assertEquals(JobStatus.FAILED, store.findByStatusIn(List.of(JobStatus.FAILED)).get(0).getStatus());
    }

    @Test
    void unknownJobIdGivesNotFound() {
        assertThrows(JobNotFoundException.class, () -> jobs.get(OWNER, "does-not-exist"));
    }

    @Test
    void listingIsPaginatedNewestFirst() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ids.add(jobs.submit(OWNER, eulerRequest(0.05)).id());
        }

        PageResult<JobResponse> first = jobs.list(OWNER, null, 0, 2);
        assertEquals(2, first.items().size());
        assertEquals(5, first.totalItems());
        assertEquals(3, first.totalPages());
        assertEquals(ids.get(4), first.items().get(0).id()); // newest first
        assertEquals(ids.get(3), first.items().get(1).id());

        assertEquals(ids.get(2), jobs.list(OWNER, null, 1, 2).items().get(0).id());
        PageResult<JobResponse> last = jobs.list(OWNER, null, 2, 2);
        assertEquals(1, last.items().size());
        assertEquals(ids.get(0), last.items().get(0).id());
        assertEquals(0, jobs.list(OWNER, null, 3, 2).items().size()); // past the end: empty, not an error
    }

    @Test
    void listingCanFilterByStatus() {
        jobs.submit(OWNER, eulerRequest(0.05));
        jobs.submit(OWNER, eulerRequest(0.05));
        executor.runAll();
        jobs.submit(OWNER, eulerRequest(0.05)); // stays QUEUED

        assertEquals(2, jobs.list(OWNER, JobStatus.DONE, 0, 10).totalItems());
        assertEquals(1, jobs.list(OWNER, JobStatus.QUEUED, 0, 10).totalItems());
        assertEquals(0, jobs.list(OWNER, JobStatus.FAILED, 0, 10).totalItems());
        assertEquals(3, jobs.list(OWNER, null, 0, 10).totalItems());
    }

    @Test
    void badPagingParametersAreRejected() {
        assertThrows(InvalidSimulationRequestException.class, () -> jobs.list(OWNER, null, -1, 10));
        assertThrows(InvalidSimulationRequestException.class, () -> jobs.list(OWNER, null, 0, 0));
        assertThrows(InvalidSimulationRequestException.class, () -> jobs.list(OWNER, null, 0, 101));
    }

    @Test
    void interruptedJobsAreMarkedFailedAtStartUp() {
        JobResponse queued = jobs.submit(OWNER, eulerRequest(0.05)); // never executed: simulates a crash
        JobResponse finished = jobs.submit(OWNER, eulerRequest(0.05));
        // Run only the second job.
        executor.pending.get(1).run();
        executor.pending.clear();

        jobs.failInterruptedJobs();

        assertEquals(JobStatus.FAILED, jobs.get(OWNER, queued.id()).status());
        assertEquals("Interrupted by an application restart", jobs.get(OWNER, queued.id()).errorMessage());
        assertEquals(JobStatus.DONE, jobs.get(OWNER, finished.id()).status());
    }

    @Test
    void eachUserSeesOnlyTheirOwnJobs() {
        JobResponse alices = jobs.submit("alice", eulerRequest(0.05));
        JobResponse bobs = jobs.submit("bob", eulerRequest(0.05));

        assertEquals(1, jobs.list("alice", null, 0, 10).totalItems());
        assertEquals(alices.id(), jobs.list("alice", null, 0, 10).items().get(0).id());
        assertEquals(1, jobs.list("bob", null, 0, 10).totalItems());
        assertEquals(bobs.id(), jobs.get("bob", bobs.id()).id());
    }

    @Test
    void anotherUsersJobLooksLikeItDoesNotExist() {
        JobResponse alices = jobs.submit("alice", eulerRequest(0.05));

        // Same exception as for an id that does not exist: the caller learns nothing.
        assertThrows(JobNotFoundException.class, () -> jobs.get("bob", alices.id()));
    }

    @Test
    void jobsWithoutAnOwnerAreVisibleToNobody() {
        // A job saved before Phase 4 has no owner.
        store.save(new SimulationJob("old-job", null, "euler-column-buckling", "{}", Instant.EPOCH));

        assertThrows(JobNotFoundException.class, () -> jobs.get("alice", "old-job"));
        assertEquals(0, jobs.list("alice", null, 0, 10).totalItems());
    }
}
