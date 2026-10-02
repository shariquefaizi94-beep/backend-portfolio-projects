package com.nexaforge.producer.config;

import com.nexaforge.producer.generator.EventGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GeneratorController REST endpoints.
 */
@ExtendWith(MockitoExtension.class)
class GeneratorControllerTest {

    @Mock
    private EventGenerator eventGenerator;

    private GeneratorController controller;

    @BeforeEach
    void setUp() {
        controller = new GeneratorController(eventGenerator);
    }

    @Test
    void start_startsGeneratorAndReturnsStatus() {
        ResponseEntity<Map<String, Object>> response = controller.start();

        verify(eventGenerator).start();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("started", response.getBody().get("status"));
    }

    @Test
    void stop_stopsGeneratorAndReturnsTotalGenerated() {
        when(eventGenerator.getTotalGenerated()).thenReturn(5000L);

        ResponseEntity<Map<String, Object>> response = controller.stop();

        verify(eventGenerator).stop();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("stopped", response.getBody().get("status"));
        assertEquals(5000L, response.getBody().get("totalGenerated"));
    }

    @Test
    void status_returnsCurrentState() {
        when(eventGenerator.isRunning()).thenReturn(true);
        when(eventGenerator.getTotalGenerated()).thenReturn(12345L);

        ResponseEntity<Map<String, Object>> response = controller.status();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().get("running"));
        assertEquals(12345L, response.getBody().get("totalGenerated"));
    }

    @Test
    void status_whenNotRunning_returnsCorrectState() {
        when(eventGenerator.isRunning()).thenReturn(false);
        when(eventGenerator.getTotalGenerated()).thenReturn(0L);

        ResponseEntity<Map<String, Object>> response = controller.status();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, response.getBody().get("running"));
        assertEquals(0L, response.getBody().get("totalGenerated"));
    }
}
