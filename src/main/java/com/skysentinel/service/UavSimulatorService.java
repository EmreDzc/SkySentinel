package com.skysentinel.service;

import com.skysentinel.model.TelemetryData;
import com.skysentinel.repository.TelemetryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
public class UavSimulatorService {

    private final TelemetryRepository repository;
    private final Random random = new Random();

    // Start coordinates (Approx. Ankara for realism)
    private double currentLat = 39.9334;
    private double currentLon = 32.8597;

    // Constructor Injection (Best Practice)
    public UavSimulatorService(TelemetryRepository repository) {
        this.repository = repository;
    }

    // Runs every 1000ms (1 second)
    @Scheduled(fixedRate = 1000)
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

        // 3. Save to Neon Database
        repository.save(data);

        // 4. Console Log (so we can see it working)
        System.out.println("LOG: Data Saved! [Lat: " + data.getLatitude() + 
                           ", Lon: " + data.getLongitude() + "]");
    }

}