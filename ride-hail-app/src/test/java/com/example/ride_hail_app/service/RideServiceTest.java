package com.example.ride_hail_app.service;

import com.example.ride_hail_app.dto.RideRequestDTO;
import com.example.ride_hail_app.dto.RideResponseDTO;
import com.example.ride_hail_app.model.*;
import com.example.ride_hail_app.locking.DistributedLockService;
import com.example.ride_hail_app.monitoring.NewRelicMetrics;
import com.example.ride_hail_app.repository.DriverRepository;
import com.example.ride_hail_app.repository.RideRepository;
import com.example.ride_hail_app.repository.RiderRepository;
import com.example.ride_hail_app.statemachine.RideStateMachine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {
    
    @Mock
    private RideRepository rideRepository;
    
    @Mock
    private RiderRepository riderRepository;
    
    @Mock
    private DriverRepository driverRepository;
    
    @Mock
    private DriverMatchingService driverMatchingService;
    
    @Mock
    private SurgePricingService surgePricingService;
    
    @Mock
    private FareCalculationService fareCalculationService;
    
    @Mock
    private RideStateMachine stateMachine;
    
    @Mock
    private IdempotencyService idempotencyService;
    
    @Mock
    private CacheService cacheService;
    
    @Mock
    private NotificationService notificationService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private NewRelicMetrics newRelicMetrics;
    
    @Mock
    private DistributedLockService distributedLockService;
    
    @Mock
    private CacheInvalidationService cacheInvalidationService;
    
    @InjectMocks
    private RideService rideService;
    
    private Rider testRider;
    private Driver testDriver;
    private RideRequestDTO rideRequest;
    
    @BeforeEach
    void setUp() {
        testRider = new Rider();
        testRider.setId(1L);
        testRider.setName("Test Rider");
        testRider.setPhoneNumber("+1234567890");
        testRider.setEmail("rider@test.com");
        testRider.setTenantId("tenant1");
        testRider.setRegion("us-east");
        
        testDriver = new Driver();
        testDriver.setId(1L);
        testDriver.setName("Test Driver");
        testDriver.setStatus(DriverStatus.ONLINE);
        testDriver.setTenantId("tenant1");
        testDriver.setRegion("us-east");
        testDriver.setCurrentLocation(new Location(
            BigDecimal.valueOf(40.7128),
            BigDecimal.valueOf(-74.0060),
            "New York, NY"
        ));
        
        rideRequest = new RideRequestDTO();
        rideRequest.setRiderId(1L);
        rideRequest.setPickupLatitude(BigDecimal.valueOf(40.7128));
        rideRequest.setPickupLongitude(BigDecimal.valueOf(-74.0060));
        rideRequest.setDropoffLatitude(BigDecimal.valueOf(40.7589));
        rideRequest.setDropoffLongitude(BigDecimal.valueOf(-73.9851));
        rideRequest.setTier(RideTier.ECONOMY);
        rideRequest.setPaymentMethod(PaymentMethod.CARD);
    }
    
    @Test
    void testCreateRide_Success() {
        // Given
        when(riderRepository.findById(1L)).thenReturn(Optional.of(testRider));
        when(surgePricingService.calculateSurgeMultiplier(anyDouble(), anyDouble(), anyString()))
            .thenReturn(BigDecimal.valueOf(1.2));
        when(fareCalculationService.calculateBaseFare(any(), any(), any()))
            .thenReturn(BigDecimal.valueOf(100.0));
        when(fareCalculationService.calculateTotalFare(any(), any()))
            .thenReturn(BigDecimal.valueOf(120.0));
        when(driverMatchingService.findBestDriver(any(), anyString(), anyString()))
            .thenReturn(testDriver);
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId(1L);
            return ride;
        });
        when(idempotencyService.acquireLock(anyString())).thenReturn(true);
        when(idempotencyService.getCachedResponse(anyString())).thenReturn(null);
        
        // Mock distributed lock service - LockHandle constructor is package-private, so we mock it
        com.example.ride_hail_app.locking.DistributedLockService.LockHandle lockHandle = 
            mock(com.example.ride_hail_app.locking.DistributedLockService.LockHandle.class);
        when(distributedLockService.acquireLock(anyString(), any(java.time.Duration.class)))
            .thenReturn(lockHandle);
        when(distributedLockService.releaseLock(any(com.example.ride_hail_app.locking.DistributedLockService.LockHandle.class)))
            .thenReturn(true);
        
        // When
        RideResponseDTO response = rideService.createRide(rideRequest, "tenant1", "us-east", null);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getRideId());
        assertEquals(RideStatus.MATCHED, response.getStatus());
        verify(rideRepository, times(1)).save(any(Ride.class));
        verify(driverRepository, times(1)).save(any(Driver.class));
    }
    
    @Test
    void testCreateRide_RiderNotFound() {
        // Given
        when(riderRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(RuntimeException.class, () -> {
            rideService.createRide(rideRequest, "tenant1", "us-east", null);
        });
    }
    
    @Test
    void testCreateRide_WithIdempotency() throws JsonProcessingException {
        // Given
        String idempotencyKey = "test-key-123";
        String cachedResponse = "{\"rideId\":\"cached-id\"}";
        RideResponseDTO cachedRideResponse = new RideResponseDTO();
        cachedRideResponse.setRideId("cached-id");
        
        when(idempotencyService.getCachedResponse(idempotencyKey)).thenReturn(cachedResponse);
        when(objectMapper.readValue(cachedResponse, RideResponseDTO.class))
            .thenReturn(cachedRideResponse);
        
        // When
        RideResponseDTO response = rideService.createRide(rideRequest, "tenant1", "us-east", idempotencyKey);
        
        // Then
        assertNotNull(response);
        assertEquals("cached-id", response.getRideId());
        verify(rideRepository, never()).save(any());
    }
    
    @Test
    void testGetRide_Success() {
        // Given
        Ride ride = new Ride();
        ride.setRideId("test-ride-id");
        ride.setRider(testRider);
        ride.setStatus(RideStatus.MATCHED);
        
        when(cacheService.getCachedRide("test-ride-id")).thenReturn(null);
        when(rideRepository.findByRideId("test-ride-id")).thenReturn(Optional.of(ride));
        
        // When
        RideResponseDTO response = rideService.getRide("test-ride-id");
        
        // Then
        assertNotNull(response);
        assertEquals("test-ride-id", response.getRideId());
        verify(cacheService, times(1)).cacheRide(anyString(), any(Ride.class));
    }
    
    @Test
    void testStartRide_Success() {
        // Given
        Ride ride = new Ride();
        ride.setRideId("test-ride-id");
        ride.setRider(testRider);
        ride.setDriver(testDriver);
        ride.setStatus(RideStatus.MATCHED);
        
        when(rideRepository.findByRideId("test-ride-id")).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenReturn(ride);
        
        // When
        RideResponseDTO response = rideService.startRide("test-ride-id", 1L);
        
        // Then
        assertNotNull(response);
        assertEquals(RideStatus.IN_PROGRESS, response.getStatus());
        verify(stateMachine, times(1)).validateTransition(any(), any());
    }
    
    @Test
    void testCancelRide_Success() {
        // Given
        Ride ride = new Ride();
        ride.setRideId("test-ride-id");
        ride.setRider(testRider);
        ride.setDriver(testDriver);
        ride.setStatus(RideStatus.PENDING);
        ride.setTenantId("tenant1");
        ride.setRegion("us-east");
        
        when(rideRepository.findByRideId("test-ride-id")).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenReturn(ride);
        
        // When
        RideResponseDTO response = rideService.cancelRide("test-ride-id", 1L, "rider");
        
        // Then
        assertNotNull(response);
        assertEquals(RideStatus.CANCELLED, response.getStatus());
        verify(cacheInvalidationService, times(1)).invalidateRideCaches(anyString(), anyString(), anyString());
    }
}

