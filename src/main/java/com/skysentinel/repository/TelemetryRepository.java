package com.skysentinel.repository;

import com.skysentinel.model.TelemetryData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

public interface TelemetryRepository extends JpaRepository<TelemetryData, UUID> {
}
