--liquibase formatted sql

--changeset bcb:004-create-message-table
CREATE TABLE message (
    id              UUID           NOT NULL,
    conversation_id UUID           NOT NULL,
    priority        TEXT,
    status          TEXT,
    sent_by_type    TEXT,
    cost            NUMERIC(10,2),
    created_at      TIMESTAMP,
    queued_at       TIMESTAMP,
    processed_at    TIMESTAMP,
    content         TEXT,
    CONSTRAINT pk_message PRIMARY KEY (id),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation (id)
);
--rollback DROP TABLE message;
