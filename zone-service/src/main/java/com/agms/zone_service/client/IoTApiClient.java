package com.agms.zone_service.client;

import com.agms.zone_service.dto.iot.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "iot-api-client", url = "${iot.api.base-url}")
public interface IoTApiClient {

    /**
     * Register a new user on the IoT platform.
     */
    @PostMapping("/auth/register")
    IoTRegisterResponse register(@RequestBody IoTRegisterRequest request);

    /**
     * Login to obtain a Bearer token.
     */
    @PostMapping("/auth/login")
    IoTLoginResponse login(@RequestBody IoTLoginRequest request);

    /**
     * Add a new device. Requires a valid Bearer token.
     * The Authorization header value must be prefixed with "Bearer ", e.g. "Bearer eyJ..."
     */
    @PostMapping("/devices")
    IoTAddDeviceResponse addDevice(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody IoTAddDeviceRequest request
    );
}
