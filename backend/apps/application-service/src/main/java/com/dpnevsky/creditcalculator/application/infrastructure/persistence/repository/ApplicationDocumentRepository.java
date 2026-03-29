package com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository;

import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocumentEntity, UUID> {

    Optional<ApplicationDocumentEntity> findByDocumentId(UUID documentId);

    List<ApplicationDocumentEntity> findAllByApplicationIdOrderByGeneratedAtDesc(UUID applicationId);
}