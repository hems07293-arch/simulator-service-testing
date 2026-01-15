package com.project.hems.simulator_service_testing.repository;

import com.project.hems.simulator_service_testing.domain.MeterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeterRepository extends JpaRepository<MeterEntity, Long> {
    Optional<MeterEntity> findBySiteId(Long siteId);
}
