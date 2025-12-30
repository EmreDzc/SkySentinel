package com.skysentinel.controller;

import com.skysentinel.dto.CommandRequestDTO;
import com.skysentinel.service.UavSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/commands")
public class CommandController {

    private final UavSimulatorService uavSimulatorService;

    @PostMapping
    public ResponseEntity<String> sendCommand(@RequestBody CommandRequestDTO commandRequestDTO){
        uavSimulatorService.executeCommand(commandRequestDTO);
        return ResponseEntity.ok("Komut işlendi: " + commandRequestDTO.getCommandType());
    }

}
