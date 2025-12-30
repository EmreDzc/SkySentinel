package com.skysentinel.service;

import com.skysentinel.dto.TelemetryResponseDTO;
import com.skysentinel.model.TelemetryData;
import com.skysentinel.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UavSimulatorService {

    private final TelemetryRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Random random = new Random();

    // Start coordinates (Approx. Ankara for realism)
    private double currentLat = 39.9334;
    private double currentLon = 32.8597;

    // Runs every 60000ms (1 min)
    @Scheduled(fixedRate = 5000)
    public void generateTelemetry() {
        TelemetryData data = new TelemetryData();
        
        // 1. Simulate Movement (adding small random variations)
        currentLat += (random.nextDouble() - 0.5) * 0.001; 
        currentLon += (random.nextDouble() - 0.5) * 0.001;

        // 2. Populate Data
        data.setMissionId("M-101"); // Fixed mission ID for now
        data.setLatitude(currentLat);
        data.setLongitude(currentLon);
        data.setAltitude(1000 + random.nextDouble() * 10); // Fluctuates around 1000m
        data.setSpeed(85 + random.nextDouble() * 5);       // Fluctuates around 85 km/h
        data.setBatteryLevel(random.nextInt(100));         // Random battery for now
        data.setEngineRunning(true);

        repository.save(data);
        messagingTemplate.convertAndSend("/topic/telemetry", data);

        log.info("Data Saved & Broadcasted! [Lat: {}]", data.getLatitude());
    }

    public List<TelemetryResponseDTO> getAllTelemetry() {
        List<TelemetryResponseDTO> responseList = new ArrayList<>();
        List<TelemetryData> entities = repository.findAll();

        for(TelemetryData entity : entities){
            TelemetryResponseDTO dto = new TelemetryResponseDTO();
            BeanUtils.copyProperties(entity,dto);
            responseList.add(dto);
        }
        return responseList;
    }

    public TelemetryResponseDTO getLatestTelemetry() {
        Optional<TelemetryData> latestDataOpt = repository.findTopByOrderByTimestampDesc();
        if(latestDataOpt.isPresent()){
            TelemetryData telemetryData = latestDataOpt.get();
            TelemetryResponseDTO dto = new TelemetryResponseDTO();
            BeanUtils.copyProperties(telemetryData,dto);
            return dto;
        }
        return null;
    }
}