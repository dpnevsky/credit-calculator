package com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository;

import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.OfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<OfferEntity, UUID> {

    List<OfferEntity> findAllByApplicationIdOrderByRateAsc(UUID applicationId);

    Optional<OfferEntity> findByIdAndApplicationId(UUID offerId, UUID applicationId);

    void deleteAllByApplicationId(UUID applicationId);
}
