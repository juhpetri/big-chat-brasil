--liquibase formatted sql

--changeset bcb:001-create-client-table
CREATE TABLE client (
    id            UUID           NOT NULL,
    name          TEXT,
    document      TEXT,
    document_type TEXT,
    plan_type     TEXT,
    active        BOOLEAN        DEFAULT TRUE,
    balance       NUMERIC(10,2),
    monthly_limit NUMERIC(10,2),
    monthly_usage NUMERIC(10,2),
    created_at    TIMESTAMP,
    CONSTRAINT pk_client PRIMARY KEY (id),
    CONSTRAINT uk_client_document UNIQUE (document)
);
--rollback DROP TABLE client;
