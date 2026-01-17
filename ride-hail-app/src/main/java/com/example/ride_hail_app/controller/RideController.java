package com.example.ride_hail_app.controller;

import com.example.ride_hail_app.dto.RideRequestDTO;
import com.example.ride_hail_app.dto.RideResponseDTO;
import com.example.ride_hail_app.service.PaymentService;
import com.example.ride_hail_app.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/rides")
public class RideController {
    
    private final RideService rideService;
    private final PaymentService paymentService;
    
    public RideController(RideService rideService, PaymentService paymentService) {
        this.rideService = rideService;
        this.paymentService = paymentService;
    }
    
    /**
     * POST /v1/rides - Create a ride request
     * 
     * This endpoint creates a new ride request and attempts to match it with an available driver.
     * Multi-tenant and multi-region support via headers.
     * 
     * @param request The ride request details
     * @param tenantId Optional tenant ID from header (X-Tenant-Id)
     * @param region Optional region from header (X-Region)
     * @return RideResponseDTO with ride details and matching status
     */
    @PostMapping
    public ResponseEntity<RideResponseDTO> createRide(
        @Valid @RequestBody RideRequestDTO request,
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
        @RequestHeader(value = "X-Region", required = false) String region,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        RideResponseDTO response = rideService.createRide(request, tenantId, region, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /v1/rides/{rideId} - Get ride details
     */
    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponseDTO> getRide(@PathVariable String rideId) {
        RideResponseDTO response = rideService.getRide(rideId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /v1/rides/{rideId}/arriving - Mark driver as arriving at pickup location
     */
    @PostMapping("/{rideId}/arriving")
    public ResponseEntity<RideResponseDTO> markDriverArriving(
        @PathVariable String rideId,
        @RequestParam Long driverId
    ) {
        RideResponseDTO response = rideService.markDriverArriving(rideId, driverId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /v1/rides/{rideId}/start - Start a ride (driver picks up rider)
     */
    @PostMapping("/{rideId}/start")
    public ResponseEntity<RideResponseDTO> startRide(
        @PathVariable String rideId,
        @RequestParam Long driverId
    ) {
        RideResponseDTO response = rideService.startRide(rideId, driverId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /v1/rides/{rideId}/pause - Pause a ride
     */
    @PostMapping("/{rideId}/pause")
    public ResponseEntity<RideResponseDTO> pauseRide(
        @PathVariable String rideId,
        @RequestParam Long driverId
    ) {
        RideResponseDTO response = rideService.pauseRide(rideId, driverId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /v1/rides/{rideId}/complete - Complete a ride
     */
    @PostMapping("/{rideId}/complete")
    public ResponseEntity<RideResponseDTO> completeRide(
        @PathVariable String rideId,
        @RequestParam Long driverId
    ) {
        RideResponseDTO response = rideService.completeRide(rideId, driverId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /v1/rides/{rideId}/cancel - Cancel a ride
     */
    @PostMapping("/{rideId}/cancel")
    public ResponseEntity<RideResponseDTO> cancelRide(
        @PathVariable String rideId,
        @RequestParam Long userId,
        @RequestParam String userType
    ) {
        RideResponseDTO response = rideService.cancelRide(rideId, userId, userType);
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /v1/rides/{rideId}/payment - Process payment for a completed ride
     */
    @PostMapping("/{rideId}/payment")
    public ResponseEntity<PaymentService.PaymentResult> processPayment(
        @PathVariable String rideId,
        @RequestBody Map<String, String> paymentRequest
    ) {
        String paymentToken = paymentRequest.get("paymentToken");
        PaymentService.PaymentResult result = paymentService.processPayment(rideId, paymentToken);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
}

