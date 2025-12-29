package com.skysentinel.repository;

import com.skysentinel.model.TelemetryData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional; // Import this

@Repository
public interface TelemetryRepository extends JpaRepository<TelemetryData, UUID> {

    // Custom query: Sort by timestamp (newest first) and give me the top 1
    Optional<TelemetryData> findTopByOrderByTimestampDesc();

    public void deleteByTimestampBefore(LocalDateTime threshold);
}