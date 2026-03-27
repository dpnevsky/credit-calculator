package com.dpnevsky.creditcalculator.document.infrastructure.persistence.repository;

import com.dpnevsky.creditcalculator.document.infrastructure.persistence.entity.DocumentRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRequestRepository extends JpaRepository<DocumentRequestEntity, UUID> {
}