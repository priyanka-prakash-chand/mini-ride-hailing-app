package com.example.ride_hail_app.integration;

import com.example.ride_hail_app.dto.RideRequestDTO;
import com.example.ride_hail_app.model.PaymentMethod;
import com.example.ride_hail_app.model.RideStatus;
import com.example.ride_hail_app.model.RideTier;
import com.example.ride_hail_app.repository.DriverRepository;
import com.example.ride_hail_app.repository.RideRepository;
import com.example.ride_hail_app.repository.RiderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ride management APIs
 * Tests the full request/response cycle including database operations
 */
@SpringBootTest(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RideIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private RideRepository rideRepository;
    
    @Autowired
    private RiderRepository riderRepository;
    
    @Autowired
    private DriverRepository driverRepository;
    
    private RideRequestDTO rideRequest;
    
    @BeforeEach
    void setUp() {
        // Test data should be initialized by DataInitializer
        rideRequest = new RideRequestDTO();
        rideRequest.setRiderId(1L);
        rideRequest.setPickupLatitude(BigDecimal.valueOf(40.7128));
        rideRequest.setPickupLongitude(BigDecimal.valueOf(-74.0060));
        rideRequest.setPickupAddress("New York, NY");
        rideRequest.setDropoffLatitude(BigDecimal.valueOf(40.7589));
        rideRequest.setDropoffLongitude(BigDecimal.valueOf(-73.9851));
        rideRequest.setDropoffAddress("Times Square, NY");
        rideRequest.setTier(RideTier.ECONOMY);
        rideRequest.setPaymentMethod(PaymentMethod.CARD);
    }
    
    @Test
    void testCreateRide_Integration() throws Exception {
        // When
        String response = mockMvc.perform(post("/v1/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", "tenant1")
                .header("X-Region", "us-east")
                .content(objectMapper.writeValueAsString(rideRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rideId").exists())
            .andExpect(jsonPath("$.status").value(RideStatus.MATCHED.toString()))
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // Then - Verify ride was saved to database
        String rideId = objectMapper.readTree(response).get("rideId").asText();
        assertTrue(rideRepository.findByRideId(rideId).isPresent());
    }
    
    @Test
    void testGetRide_Integration() throws Exception {
        // Given - Create a ride first
        String createResponse = mockMvc.perform(post("/v1/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", "tenant1")
                .header("X-Region", "us-east")
                .content(objectMapper.writeValueAsString(rideRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        String rideId = objectMapper.readTree(createResponse).get("rideId").asText();
        
        // When
        mockMvc.perform(get("/v1/rides/" + rideId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rideId").value(rideId))
            .andExpect(jsonPath("$.status").exists());
    }
    
    @Test
    void testStartRide_Integration() throws Exception {
        // Given - Create and get a matched ride
        String createResponse = mockMvc.perform(post("/v1/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", "tenant1")
                .header("X-Region", "us-east")
                .content(objectMapper.writeValueAsString(rideRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        String rideId = objectMapper.readTree(createResponse).get("rideId").asText();
        Long driverId = objectMapper.readTree(createResponse).get("driverId").asLong();
        
        // When
        mockMvc.perform(post("/v1/rides/" + rideId + "/start")
                .param("driverId", driverId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(RideStatus.IN_PROGRESS.toString()));
        
        // Then - Verify status in database
        assertEquals(RideStatus.IN_PROGRESS, 
            rideRepository.findByRideId(rideId).get().getStatus());
    }
    
    @Test
    void testCompleteRide_Integration() throws Exception {
        // Given - Create, match, and start a ride
        String createResponse = mockMvc.perform(post("/v1/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", "tenant1")
                .header("X-Region", "us-east")
                .content(objectMapper.writeValueAsString(rideRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        String rideId = objectMapper.readTree(createResponse).get("rideId").asText();
        Long driverId = objectMapper.readTree(createResponse).get("driverId").asLong();
        
        // Start the ride
        mockMvc.perform(post("/v1/rides/" + rideId + "/start")
                .param("driverId", driverId.toString()))
            .andExpect(status().isOk());
        
        // When - Complete the ride
        mockMvc.perform(post("/v1/rides/" + rideId + "/complete")
                .param("driverId", driverId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(RideStatus.COMPLETED.toString()));
        
        // Then - Verify status in database
        assertEquals(RideStatus.COMPLETED, 
            rideRepository.findByRideId(rideId).get().getStatus());
    }
    
    @Test
    void testCancelRide_Integration() throws Exception {
        // Given - Create a ride
        String createResponse = mockMvc.perform(post("/v1/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", "tenant1")
                .header("X-Region", "us-east")
                .content(objectMapper.writeValueAsString(rideRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        String rideId = objectMapper.readTree(createResponse).get("rideId").asText();
        Long riderId = objectMapper.readTree(createResponse).get("riderId").asLong();
        
        // When - Cancel the ride
        mockMvc.perform(post("/v1/rides/" + rideId + "/cancel")
                .param("userId", riderId.toString())
                .param("role", "rider"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(RideStatus.CANCELLED.toString()));
        
        // Then - Verify status in database
        assertEquals(RideStatus.CANCELLED, 
            rideRepository.findByRideId(rideId).get().getStatus());
    }
    
    @Test
    void testCreateRide_ValidationError() throws Exception {
        // Given - Invalid request (missing required fields)
        RideRequestDTO invalidRequest = new RideRequestDTO();
        
        // When/Then
        mockMvc.perform(post("/v1/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", "tenant1")
                .header("X-Region", "us-east")
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGetRide_NotFound() throws Exception {
        // When/Then
        mockMvc.perform(get("/v1/rides/non-existent-id"))
            .andExpect(status().isInternalServerError());
    }
}

