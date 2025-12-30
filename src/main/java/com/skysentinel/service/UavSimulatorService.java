package com.skysentinel.service;

import com.skysentinel.dto.CommandRequestDTO;
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

    private final double HOME_LAT = 39.9334;
    private final double HOME_LON = 32.8597;

    private double currentLat = HOME_LAT;
    private double currentLon = HOME_LON;
    private double currentAlt = 0; // Yerde başlasın
    private double currentSpeed = 0;
    private boolean isEngineRunning = false;
    private int batteryLevel = 100;

    private double targetAltitude = 1000.0;
    private boolean isReturnToHome = false;
    private boolean isEmergencyLanding = false;

    public void executeCommand(CommandRequestDTO command) {
        log.info("Komut Alındı: {} - Değer: {}", command.getCommandType(), command.getValue());
        switch (command.getCommandType()) {
            case "TAKEOFF":
                if (!isEngineRunning) {
                    isEngineRunning = true;
                    targetAltitude = 1000.0;
                    isEmergencyLanding = false;
                    isReturnToHome = false;
                }
                break;
            case "LAND":
                isEmergencyLanding = true;
                isReturnToHome = false;
                break;
            case "RTH":
                isReturnToHome = true;
                isEmergencyLanding = false;
                targetAltitude = 1000.0;
                break;
            case "CHANGE_ALTITUDE":
                if (command.getValue() != null) {
                    targetAltitude = command.getValue();
                }
                break;
        }
    }

    @Scheduled(fixedRate = 10000)
    public void generateTelemetry() {
        if (!isEngineRunning && currentAlt <= 0) {
            broadcastTelemetry();
            return;
        }
        if (isEmergencyLanding) {
            targetAltitude = 0;
        }
        if (currentAlt < targetAltitude) {
            currentAlt += 10 + random.nextDouble() * 5;
        } else if (currentAlt > targetAltitude) {
            currentAlt -= 10 + random.nextDouble() * 5;
        }
        if (currentAlt <= 0) {
            currentAlt = 0;
            if (isEmergencyLanding) {
                isEngineRunning = false;
                currentSpeed = 0;
            }
        }
        if (isEngineRunning && currentAlt > 10) {
            if (isReturnToHome) {
                double latDiff = HOME_LAT - currentLat;
                double lonDiff = HOME_LON - currentLon;

                currentLat += latDiff * 0.05;
                currentLon += lonDiff * 0.05;
                currentSpeed = 60.0;
                if (Math.abs(latDiff) < 0.0001 && Math.abs(lonDiff) < 0.0001) {
                    isEmergencyLanding = true;
                }
            } else if (!isEmergencyLanding) {
                currentLat += (random.nextDouble() - 0.5) * 0.0005;
                currentLon += (random.nextDouble() - 0.5) * 0.0005;
                currentSpeed = 85 + random.nextDouble() * 10;
            } else {
                currentSpeed = currentSpeed * 0.9;
            }
        }
        if (isEngineRunning && batteryLevel > 0) {
            if (random.nextInt(100) < 5) batteryLevel--;
        }

        broadcastTelemetry();
    }

    private void broadcastTelemetry() {
        TelemetryData data = new TelemetryData();
        data.setMissionId("M-101");
        data.setLatitude(currentLat);
        data.setLongitude(currentLon);
        data.setAltitude(currentAlt);
        data.setSpeed(currentSpeed);
        data.setBatteryLevel(batteryLevel);
        data.setEngineRunning(isEngineRunning);

        repository.save(data);
        messagingTemplate.convertAndSend("/topic/telemetry", data);

        log.info("Telemetry: Alt={}, Lat={}, Mode={}", String.format("%.2f", currentAlt), currentLat,
                isReturnToHome ? "RTH" : (isEmergencyLanding ? "LANDING" : "NORMAL"));
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