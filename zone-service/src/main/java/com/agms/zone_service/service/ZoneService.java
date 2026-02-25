package com.agms.zone_service.service;

import com.agms.zone_service.dto.ZoneRequest;
import com.agms.zone_service.dto.ZoneResponse;
import com.agms.zone_service.dto.iot.IoTRegisterResponse;
import com.agms.zone_service.model.Zone;
import com.agms.zone_service.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final IoTIntegrationService iotIntegrationService;

    /**
     * Creates a zone with full IoT provisioning:
     * 1. Register IoT user
     * 2. Login → get Bearer token
     * 3. Add device → get deviceId
     * 4. Save zone with deviceId and iotUserId
     */
    @Transactional
    public ZoneResponse createZone(ZoneRequest request) {
        log.info("Creating zone: {}", request.getName());

        // Step 1: Register IoT user
        IoTRegisterResponse registered = iotIntegrationService.registerUser(
                request.getIotUsername(),
                request.getIotEmail(),
                request.getIotPassword()
        );
        log.info("IoT user registered with id: {}", registered.getUserId());

        // Step 2: Login → receive Bearer token
        String token = iotIntegrationService.login(
                request.getIotUsername(),
                request.getIotPassword()
        );
        log.info("IoT login successful, token obtained");

        // Step 3: Add device → receive deviceId
        String deviceId = iotIntegrationService.addDevice(
                token,
                request.getDeviceName(),
                request.getDeviceType(),
                request.getName()   // zone name used as device location
        );
        log.info("IoT device registered with id: {}", deviceId);

        // Step 4: Persist zone
        Zone zone = Zone.builder()
                .name(request.getName())
                .description(request.getDescription())
                .deviceId(deviceId)
                .iotUserId(registered.getUserId())
                .minTemp(request.getMinTemp())
                .maxTemp(request.getMaxTemp())
                .build();

        Zone saved = zoneRepository.save(zone);
        log.info("Zone saved with id: {}", saved.getId());

        return toResponse(saved);
    }

    public List<ZoneResponse> getAllZones() {
        return zoneRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ZoneResponse getZoneById(Long id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zone not found with id: " + id));
        return toResponse(zone);
    }

    private ZoneResponse toResponse(Zone zone) {
        return ZoneResponse.builder()
                .id(zone.getId())
                .name(zone.getName())
                .description(zone.getDescription())
                .deviceId(zone.getDeviceId())
                .iotUserId(zone.getIotUserId())
                .minTemp(zone.getMinTemp())
                .maxTemp(zone.getMaxTemp())
                .build();
    }
}
