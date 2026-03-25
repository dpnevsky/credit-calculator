package com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository;

import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationSubmitDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplicationSubmitDataRepository extends JpaRepository<ApplicationSubmitDataEntity, UUID> {
}