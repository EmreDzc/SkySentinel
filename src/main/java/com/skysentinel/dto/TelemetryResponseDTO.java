package com.skysentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TelemetryResponseDTO {

    private UUID id;
    private String missionId;
    private double latitude;
    private double longitude;
    private double altitude;
    private double speed;
    private int batteryLevel;
    private boolean isEngineRunning;
    private LocalDateTime timestamp;

}
