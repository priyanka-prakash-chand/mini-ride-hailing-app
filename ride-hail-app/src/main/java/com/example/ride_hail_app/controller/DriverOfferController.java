package com.example.ride_hail_app.controller;

import com.example.ride_hail_app.service.IDriverOfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/offers")
public class DriverOfferController {
    
    private final IDriverOfferService driverOfferService;
    
    public DriverOfferController(IDriverOfferService driverOfferService) {
        this.driverOfferService = driverOfferService;
    }
    
    /**
     * POST /v1/offers/{rideId}/send - Send ride offer to driver
     */
    @PostMapping("/{rideId}/send")
    public ResponseEntity<Map<String, String>> sendOffer(
        @PathVariable String rideId,
        @RequestParam Long driverId
    ) {
        boolean sent = driverOfferService.sendOffer(rideId, driverId);
        if (sent) {
            return ResponseEntity.ok(Map.of(
                "message", "Offer sent successfully",
                "rideId", rideId,
                "driverId", driverId.toString()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Driver already has an active offer",
                "rideId", rideId,
                "driverId", driverId.toString()
            ));
        }
    }
    
    /**
     * POST /v1/offers/{rideId}/accept - Accept a ride offer
     */
    @PostMapping("/{rideId}/accept")
    public ResponseEntity<Map<String, String>> acceptOffer(
        @PathVariable String rideId,
        @RequestParam Long driverId
    ) {
        boolean accepted = driverOfferService.acceptOffer(rideId, driverId);
        if (accepted) {
            return ResponseEntity.ok(Map.of(
                "message", "Offer accepted successfully",
                "rideId", rideId,
                "driverId", driverId.toString()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Offer expired or already processed",
                "rideId", rideId,
                "driverId", driverId.toString()
            ));
        }
    }
    
    /**
     * POST /v1/offers/{rideId}/decline - Decline a ride offer
     */
    @PostMapping("/{rideId}/decline")
    public ResponseEntity<Map<String, String>> declineOffer(
        @PathVariable String rideId,
        @RequestParam Long driverId
    ) {
        driverOfferService.declineOffer(rideId, driverId);
        return ResponseEntity.ok(Map.of(
            "message", "Offer declined successfully",
            "rideId", rideId,
            "driverId", driverId.toString()
        ));
    }
}

