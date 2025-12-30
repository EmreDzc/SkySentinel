package com.skysentinel.controller;

import com.skysentinel.dto.TelemetryResponseDTO;
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

    private final UavSimulatorService uavSimulatorService;

    // Endpoint 1: Get all flight logs (Good for checking history)
    @GetMapping(path = "/all")
    public List<TelemetryResponseDTO> getAllTelemetry() {
        return uavSimulatorService.getAllTelemetry();
    }

    // NEW: Get only the latest status (Live Dashboard)
    @GetMapping(path = "/latest")
    public ResponseEntity<TelemetryResponseDTO> getLatestTelemetry() {

        TelemetryResponseDTO telemetryResponseDTO = uavSimulatorService.getLatestTelemetry();
        if(telemetryResponseDTO != null){
            return ResponseEntity.ok(telemetryResponseDTO);
        }
       return ResponseEntity.notFound().build();
    }

}