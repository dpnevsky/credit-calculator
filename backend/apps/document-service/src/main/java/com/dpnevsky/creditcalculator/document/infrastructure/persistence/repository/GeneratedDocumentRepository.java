package com.dpnevsky.creditcalculator.document.infrastructure.persistence.repository;

import com.dpnevsky.creditcalculator.document.infrastructure.persistence.entity.GeneratedDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocumentEntity, UUID> {

    List<GeneratedDocumentEntity> findAllByApplicationIdOrderByGeneratedAtDesc(UUID applicationId);
}