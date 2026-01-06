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

    // Rastgelelik yerine daha kontrollü bir simülasyon için
    private final double HOME_LAT = 39.9334;
    private final double HOME_LON = 32.8597;

    // Anlık Durum Değişkenleri
    private double currentLat = HOME_LAT;
    private double currentLon = HOME_LON;
    private double currentAlt = 0.0;
    private double currentSpeed = 0.0;
    private double targetAltitude = 0.0; // Varsayılan hedef 0

    private boolean isEngineRunning = false;
    private boolean isReturnToHome = false;
    private boolean isLanding = false;

    private int batteryLevel = 100;

    // Komutları İşleyen Metot
    public void executeCommand(CommandRequestDTO command) {
        log.info("Komut Alındı: {} - Değer: {}", command.getCommandType(), command.getValue());

        switch (command.getCommandType()) {
            case "TAKEOFF":
                if (!isEngineRunning) {
                    isEngineRunning = true;     // Motoru çalıştır
                    targetAltitude = 1000.0;    // Hedef irtifa ver
                    isLanding = false;
                    isReturnToHome = false;
                    log.info("Motor Başlatıldı, Kalkışa geçiliyor.");
                }
                break;

            case "LAND":
                if (isEngineRunning) {
                    targetAltitude = 0.0;       // Yere in
                    isLanding = true;
                    isReturnToHome = false;
                    log.info("İniş komutu alındı.");
                }
                break;

            case "RTH":
                if (isEngineRunning) {
                    targetAltitude = 1000.0;    // Önce güvenli irtifaya çık
                    isReturnToHome = true;
                    isLanding = false;
                    log.info("Eve Dönüş (RTH) Aktif.");
                }
                break;

            case "CHANGE_ALTITUDE":
                if (command.getValue() != null && isEngineRunning) {
                    targetAltitude = command.getValue();
                    log.info("Yeni İrtifa Hedefi: {}", targetAltitude);
                }
                break;
        }
    }

    // Simülasyon Döngüsü (Her 500ms'de bir çalışır - Daha akıcı olması için hızı artırdım)
    @Scheduled(fixedRate = 500)
    public void generateTelemetry() {

        // 1. İrtifa Fiziği (Yumuşak Geçiş)
        if (isEngineRunning) {
            double climbRate = 2.5; // Her döngüde 2.5 metre tırmanma/alçalma

            if (currentAlt < targetAltitude) {
                currentAlt += climbRate;
                if (currentAlt > targetAltitude) currentAlt = targetAltitude; // Hedefi geçme
            } else if (currentAlt > targetAltitude) {
                currentAlt -= climbRate;
                if (currentAlt < targetAltitude) currentAlt = targetAltitude;
            }

            // Hız Simülasyonu: Yerden yükseldikçe hızlansın
            if (currentAlt > 10) {
                if (currentSpeed < 65.0) currentSpeed += 0.5; // Yavaşça hızlan
            } else {
                currentSpeed = 0; // Yerdeyken hız 0
            }

        } else {
            // Motor kapalıysa
            currentSpeed = 0;
            if (currentAlt > 0) currentAlt -= 5.0; // Serbest düşüş gibi (basitleştirilmiş)
            if (currentAlt < 0) currentAlt = 0;
        }

        // 2. Konum Fiziği (RTH Mantığı)
        if (isReturnToHome && currentAlt > 50) {
            // Eve doğru süzül
            double latDiff = HOME_LAT - currentLat;
            double lonDiff = HOME_LON - currentLon;

            currentLat += latDiff * 0.02; // Her adımda %2 yaklaş
            currentLon += lonDiff * 0.02;

            // Eve çok yaklaştıysa inişe geç
            if (Math.abs(latDiff) < 0.0001 && Math.abs(lonDiff) < 0.0001) {
                isReturnToHome = false;
                isLanding = true;
                targetAltitude = 0.0;
                log.info("Eve ulaşıldı, iniş yapılıyor.");
            }
        } else if (isEngineRunning && !isLanding && currentAlt > 10) {
            // Normal uçuşta rastgele hafif salınım (Loiter)
            currentLat += (Math.random() - 0.5) * 0.0001;
            currentLon += (Math.random() - 0.5) * 0.0001;
        }

        // 3. İniş ve Motor Kapatma Kontrolü
        if (isLanding && currentAlt <= 0.5) {
            currentAlt = 0.0;
            isEngineRunning = false; // Yere değince motoru kapat
            isLanding = false;
            log.info("İniş tamamlandı. Motor durduruldu.");
        }

        // 4. Batarya Simülasyonu
        if (isEngineRunning && batteryLevel > 0) {
            // %1 ihtimalle batarya düşsün (çok hızlı bitmesin)
            if (new Random().nextInt(100) < 1) {
                batteryLevel--;
            }
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
        data.setEngineRunning(isEngineRunning); // Burası JSON'a "engineRunning": true/false olarak çıkar

        // Veritabanına kaydet (Log kirliliği olmaması için her döngüde kaydetmek yerine
        // sadece belirli aralıklarla kaydedebilirsin ama şimdilik kalsın)
        repository.save(data);

        // WebSocket ile gönder
        messagingTemplate.convertAndSend("/topic/telemetry", data);
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