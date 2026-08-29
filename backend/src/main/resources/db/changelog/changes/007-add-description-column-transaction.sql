--liquibase formatted sql

--changeset bcb:007-add-description-column-transaction
ALTER TABLE transaction ADD COLUMN description TEXT;
--rollback ALTER TABLE transaction DROP COLUMN description;
