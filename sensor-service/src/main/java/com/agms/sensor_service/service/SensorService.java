package com.agms.sensor_service.service;

import com.agms.sensor_service.dto.SensorReadingResponse;
import com.agms.sensor_service.dto.TelemetryResponse;
import com.agms.sensor_service.dto.AutomationProcessRequest;
import com.agms.sensor_service.client.IoTTelemetryClient;
import com.agms.sensor_service.client.AutomationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorService {

    private final IoTTelemetryClient iotTelemetryClient;
    private final AutomationClient automationClient;

    @Value("${iot.api.device-id}")
    private String deviceId;

    @Value("${iot.api.bearer-token}")
    private String bearerToken;

    @Value("${iot.api.zone-id}")
    private Long zoneId;

    // Thread-safe in-memory store for the latest reading
    private final AtomicReference<SensorReadingResponse> latestReading = new AtomicReference<>();

    /**
     * Called by the scheduler every 10 seconds.
     * Fetches telemetry from the IoT API and updates the in-memory store.
     */
    public void fetchAndStore() {
        try {
            log.info("Fetching telemetry for device: {}", deviceId);
            TelemetryResponse telemetry = iotTelemetryClient.getLatestTelemetry(
                    "Bearer " + bearerToken,
                    deviceId
            );

            SensorReadingResponse reading = SensorReadingResponse.builder()
                    .deviceId(telemetry.getDeviceId())
                    .temperature(telemetry.getTemperature())
                    .humidity(telemetry.getHumidity())
                    .soilMoisture(telemetry.getSoilMoisture())
                    .status(telemetry.getStatus())
                    .recordedAt(Instant.now())
                    .build();

            latestReading.set(reading);
            log.info("Telemetry stored: temp={}°C, humidity={}%, soil={}%",
                    reading.getTemperature(), reading.getHumidity(), reading.getSoilMoisture());

            // Forward temperature to automation-service for rule evaluation
            forwardToAutomation(reading.getTemperature());

        } catch (Exception e) {
            log.error("Failed to fetch telemetry from IoT API: {}", e.getMessage());
        }
    }

    /**
     * Returns the last successfully fetched reading, or null if none yet.
     */
    public SensorReadingResponse getLatestReading() {
        return latestReading.get();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void forwardToAutomation(Double temperature) {
        try {
            AutomationProcessRequest automationRequest = AutomationProcessRequest.builder()
                    .zoneId(zoneId)
                    .currentTemp(temperature)
                    .build();
            automationClient.process(automationRequest);
            log.info("Forwarded temp={}°C for zoneId={} to automation-service", temperature, zoneId);
        } catch (Exception e) {
            log.error("Failed to forward telemetry to automation-service: {}", e.getMessage());
        }
    }
}
