-- Phase 5, migration 2: speed up "my jobs, newest first" and "my jobs with this status".
CREATE INDEX idx_simulation_job_owner_created ON simulation_job (owner_id, created_at DESC);
CREATE INDEX idx_simulation_job_owner_status ON simulation_job (owner_id, status);
