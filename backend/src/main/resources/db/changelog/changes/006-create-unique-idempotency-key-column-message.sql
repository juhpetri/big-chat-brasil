--liquibase formatted sql

--changeset bcb:005-create-transaction-table
-- client_id/message_id não viram relacionamento JPA na entidade Transaction (fica só como UUID
-- cru, sem @ManyToOne) — mas a FK no banco continua valendo: integridade referencial é
-- responsabilidade do schema, independe de como a entidade Java decide mapear a coluna.
CREATE TABLE transaction (
    id         UUID           NOT NULL,
    client_id  UUID           NOT NULL,
    message_id UUID,
    type       TEXT,
    amount     NUMERIC(10,2),
    timestamp  TIMESTAMP,
    CONSTRAINT pk_transaction PRIMARY KEY (id),
    CONSTRAINT fk_transaction_client FOREIGN KEY (client_id) REFERENCES client (id),
    CONSTRAINT fk_transaction_message FOREIGN KEY (message_id) REFERENCES message (id)
);
--rollback DROP TABLE transaction;
