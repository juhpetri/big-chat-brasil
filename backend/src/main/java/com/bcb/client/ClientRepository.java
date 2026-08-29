package com.bcb.client;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    boolean existsByDocumentId(DocumentId documentId);

    Optional<Client> findByDocumentId(DocumentId documentId);

    // Lock pessimista: serializa débitos concorrentes no saldo/uso do mesmo cliente
    // (SELECT ... FOR UPDATE), evitando lost update entre chargeForMessage concorrentes.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Client c where c.id = :id")
    Optional<Client> findByIdForUpdate(@Param("id") UUID id);
}
