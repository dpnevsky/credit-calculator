package com.dpnevsky.creditcalculator.scoring.infrastructure.persistence.repository;

import com.dpnevsky.creditcalculator.scoring.infrastructure.persistence.entity.ScoringRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScoringRequestRepository extends JpaRepository<ScoringRequestEntity, UUID> {

    Optional<ScoringRequestEntity> findByRequestId(UUID requestId);
}