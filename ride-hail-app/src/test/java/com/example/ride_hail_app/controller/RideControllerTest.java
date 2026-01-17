package com.example.ride_hail_app.controller;

import com.example.ride_hail_app.dto.RideRequestDTO;
import com.example.ride_hail_app.dto.RideResponseDTO;
import com.example.ride_hail_app.model.PaymentMethod;
import com.example.ride_hail_app.model.RideStatus;
import com.example.ride_hail_app.model.RideTier;
import com.example.ride_hail_app.service.PaymentService;
import com.example.ride_hail_app.service.RideService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = RideController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class
})
class RideControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RideService rideService;
    
    @MockBean
    private PaymentService paymentService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testCreateRide_Success() throws Exception {
        // Given
        RideRequestDTO request = new RideRequestDTO();
        request.setRiderId(1L);
        request.setPickupLatitude(BigDecimal.valueOf(40.7128));
        request.setPickupLongitude(BigDecimal.valueOf(-74.0060));
        request.setDropoffLatitude(BigDecimal.valueOf(40.7589));
        request.setDropoffLongitude(BigDecimal.valueOf(-73.9851));
        request.setTier(RideTier.ECONOMY);
        request.setPaymentMethod(PaymentMethod.CARD);
        
        RideResponseDTO response = new RideResponseDTO();
        response.setRideId("test-ride-id");
        response.setStatus(RideStatus.MATCHED);
        
        when(rideService.createRide(any(), anyString(), anyString(), anyString()))
            .thenReturn(response);
        
        // When/Then
        mockMvc.perform(post("/v1/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", "tenant1")
                .header("X-Region", "us-east")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rideId").value("test-ride-id"))
            .andExpect(jsonPath("$.status").value("MATCHED"));
    }
    
    @Test
    void testCreateRide_ValidationError() throws Exception {
        // Given - Missing required fields
        RideRequestDTO request = new RideRequestDTO();
        
        // When/Then
        mockMvc.perform(post("/v1/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGetRide_Success() throws Exception {
        // Given
        RideResponseDTO response = new RideResponseDTO();
        response.setRideId("test-ride-id");
        response.setStatus(RideStatus.IN_PROGRESS);
        
        when(rideService.getRide("test-ride-id")).thenReturn(response);
        
        // When/Then
        mockMvc.perform(get("/v1/rides/test-ride-id"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rideId").value("test-ride-id"))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
    
    @Test
    void testStartRide_Success() throws Exception {
        // Given
        RideResponseDTO response = new RideResponseDTO();
        response.setRideId("test-ride-id");
        response.setStatus(RideStatus.IN_PROGRESS);
        
        when(rideService.startRide("test-ride-id", 1L)).thenReturn(response);
        
        // When/Then
        mockMvc.perform(post("/v1/rides/test-ride-id/start")
                .param("driverId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}

