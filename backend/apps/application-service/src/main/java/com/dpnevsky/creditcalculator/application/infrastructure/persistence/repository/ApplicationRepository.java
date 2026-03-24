package com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository;

import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {
}