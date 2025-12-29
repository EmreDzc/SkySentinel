package com.skysentinel.controller;

import com.skysentinel.dto.TelemetryResponseDTO;
import com.skysentinel.model.TelemetryData;
import com.skysentinel.repository.TelemetryRepository;
import com.skysentinel.service.UavSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryRepository repository;
    private final UavSimulatorService uavSimulatorService;

    // Endpoint 1: Get all flight logs (Good for checking history)
    @GetMapping(path = "/all")
    public List<TelemetryData> getAllTelemetry() {
        // In a real app, we would use pagination here, but this is fine for now.
        return repository.findAll();
    }

    // NEW: Get only the latest status (Live Dashboard)
    @GetMapping(path = "/latest")
    public ResponseEntity<TelemetryData> getLatestTelemetry() {
        return repository.findTopByOrderByTimestampDesc()
                .map(data -> ResponseEntity.ok(data))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(path = "/list")
    public List<TelemetryResponseDTO> getCleanLogs(){
        return uavSimulatorService.getAllTelemetry();
    }
}