package com.agms.crop_service.controller;

import com.agms.crop_service.dto.CropRequest;
import com.agms.crop_service.dto.CropResponse;
import com.agms.crop_service.model.CropStatus;
import com.agms.crop_service.service.CropService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crops")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;

    /**
     * Create a new crop. Status defaults to SEEDLING.
     * POST /api/crops
     */
    @PostMapping
    public ResponseEntity<CropResponse> createCrop(@Valid @RequestBody CropRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cropService.createCrop(request));
    }

    /**
     * Update the growth status of a crop.
     * PUT /api/crops/{id}/status?status=VEGETATIVE
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<CropResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam CropStatus status) {
        return ResponseEntity.ok(cropService.updateStatus(id, status));
    }

    /**
     * Retrieve all crops.
     * GET /api/crops
     */
    @GetMapping
    public ResponseEntity<List<CropResponse>> getAllCrops() {
        return ResponseEntity.ok(cropService.getAllCrops());
    }
}
