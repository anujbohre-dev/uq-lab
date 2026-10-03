-- Phase 5, migration 1: the two tables, exactly as the JPA entities describe them.
-- Flyway runs this file once, records it in flyway_schema_history, and never lets it be changed
-- afterwards. A later change to the schema goes into a NEW file (V3, V4, ...).

CREATE TABLE app_user (
    id            VARCHAR(36)              NOT NULL,
    username      VARCHAR(30)              NOT NULL,
    password_hash VARCHAR(60)              NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uq_app_user_username UNIQUE (username)
);

CREATE TABLE simulation_job (
    id            VARCHAR(36)              NOT NULL,
    owner_id      VARCHAR(36),
    model_id      VARCHAR(100)             NOT NULL,
    status        VARCHAR(20)              NOT NULL,
    request_json  VARCHAR(4000)            NOT NULL,
    result_json   VARCHAR(20000),
    error_message VARCHAR(1000),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at    TIMESTAMP WITH TIME ZONE,
    finished_at   TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_simulation_job PRIMARY KEY (id)
);
