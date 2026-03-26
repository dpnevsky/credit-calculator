package com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository;

import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ScoringSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScoringSnapshotRepository extends JpaRepository<ScoringSnapshotEntity, UUID> {

    Optional<ScoringSnapshotEntity> findTopByApplicationIdOrderByScoredAtDesc(UUID applicationId);
}