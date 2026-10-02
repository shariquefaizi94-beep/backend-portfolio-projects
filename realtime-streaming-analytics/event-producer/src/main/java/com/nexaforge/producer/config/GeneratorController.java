package com.nexaforge.producer.config;

import com.nexaforge.producer.generator.EventGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API to control the event generator for benchmarking.
 */
@RestController
@RequestMapping("/api/v1/generator")
public class GeneratorController {

    private final EventGenerator eventGenerator;

    public GeneratorController(EventGenerator eventGenerator) {
        this.eventGenerator = eventGenerator;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        eventGenerator.start();
        return ResponseEntity.ok(Map.of("status", "started"));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        eventGenerator.stop();
        return ResponseEntity.ok(Map.of("status", "stopped", "totalGenerated", eventGenerator.getTotalGenerated()));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "running", eventGenerator.isRunning(),
                "totalGenerated", eventGenerator.getTotalGenerated()
        ));
    }
}
