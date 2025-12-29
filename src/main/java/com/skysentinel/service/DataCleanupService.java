package com.skysentinel.service;

import com.skysentinel.repository.TelemetryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DataCleanupService {

    private final TelemetryRepository telemetryRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deleteByTimestampBefore(){
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        telemetryRepository.deleteByTimestampBefore(threshold);
        System.out.println("Temizlik yapıldı: 1 saatten eski veriler silindi.");
    }
}
