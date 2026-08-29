--liquibase formatted sql

--changeset bcb:002-create-session-table
CREATE TABLE session (
    token      TEXT      NOT NULL,
    client_id  UUID      NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT pk_session PRIMARY KEY (token),
    CONSTRAINT fk_session_client FOREIGN KEY (client_id) REFERENCES client (id)
);
--rollback DROP TABLE session;
