--liquibase formatted sql

--changeset bcb:003-create-conversation-table
CREATE TABLE conversation (
    id              UUID      NOT NULL,
    client_id       UUID      NOT NULL,
    recipient_id    TEXT,
    recipient_name  TEXT,
    last_message_at TIMESTAMP,
    created_at      TIMESTAMP,
    CONSTRAINT pk_conversation PRIMARY KEY (id),
    CONSTRAINT fk_conversation_client FOREIGN KEY (client_id) REFERENCES client (id)
);
--rollback DROP TABLE conversation;
