package com.example.ride_hail_app.service;

import com.example.ride_hail_app.dto.RideRequestDTO;
import com.example.ride_hail_app.dto.RideResponseDTO;
import com.example.ride_hail_app.locking.DistributedLockService;
import com.example.ride_hail_app.locking.IDistributedLockService;
import com.example.ride_hail_app.model.*;
import com.example.ride_hail_app.monitoring.NewRelicMetrics;
import com.example.ride_hail_app.repository.DriverRepository;
import com.example.ride_hail_app.repository.RideRepository;
import com.example.ride_hail_app.repository.RiderRepository;
import com.example.ride_hail_app.statemachine.RideStateMachine;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Main service for ride management
 * Includes state machine, idempotency, caching, and notifications
 */
@Service
public class RideService {

  private final RideRepository rideRepository;
  private final RiderRepository riderRepository;
  private final DriverRepository driverRepository;
  private final DriverMatchingService driverMatchingService;
  private final SurgePricingService surgePricingService;
  private final FareCalculationService fareCalculationService;
  private final RideStateMachine stateMachine;
  private final IIdempotencyService idempotencyService;
  private final ICacheService cacheService;
  private final NotificationService notificationService;
  private final ObjectMapper objectMapper;
  private final NewRelicMetrics newRelicMetrics;
  private final IDistributedLockService distributedLockService;
  private final ICacheInvalidationService cacheInvalidationService;

  public RideService(RideRepository rideRepository,
                     RiderRepository riderRepository,
                     DriverRepository driverRepository,
                     DriverMatchingService driverMatchingService,
                     SurgePricingService surgePricingService,
                     FareCalculationService fareCalculationService,
                     RideStateMachine stateMachine,
                     IIdempotencyService idempotencyService,
                     ICacheService cacheService,
                     NotificationService notificationService,
                     ObjectMapper objectMapper, NewRelicMetrics newRelicMetrics,
                     IDistributedLockService distributedLockService,
                     ICacheInvalidationService cacheInvalidationService) {
    this.rideRepository = rideRepository;
    this.riderRepository = riderRepository;
    this.driverRepository = driverRepository;
    this.driverMatchingService = driverMatchingService;
    this.surgePricingService = surgePricingService;
    this.fareCalculationService = fareCalculationService;
    this.stateMachine = stateMachine;
    this.idempotencyService = idempotencyService;
    this.cacheService = cacheService;
    this.notificationService = notificationService;
    this.objectMapper = objectMapper;
    this.newRelicMetrics = newRelicMetrics;
    this.distributedLockService = distributedLockService;
    this.cacheInvalidationService = cacheInvalidationService;
  }

  /**
   * Create a new ride request and attempt to match with a driver
   * This is the core method that handles ride creation
   * Supports idempotency via idempotencyKey
   */
  @Transactional
  public RideResponseDTO createRide(RideRequestDTO request, String tenantId,
                                    String region, String idempotencyKey) {
    // Handle idempotency
    if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
      String cachedResponse =
          idempotencyService.getCachedResponse(idempotencyKey);
      if (cachedResponse != null) {
        try {
          return objectMapper.readValue(cachedResponse, RideResponseDTO.class);
        } catch (Exception e) {
          // If deserialization fails, continue with new request
        }
      }

      // Acquire lock to prevent concurrent processing
      if (!idempotencyService.acquireLock(idempotencyKey)) {
        throw new RuntimeException("Request is already being processed");
      }
    }

    try {
      // Validate and fetch rider
      Rider rider = riderRepository.findById(request.getRiderId())
                        .orElseThrow(()
                                         -> new RuntimeException(
                                             "Rider not found with ID: " +
                                             request.getRiderId()));

      // Validate tenant and region match
      if (tenantId != null && !rider.getTenantId().equals(tenantId)) {
        throw new RuntimeException("Rider tenant mismatch");
      }
      if (region != null && !rider.getRegion().equals(region)) {
        throw new RuntimeException("Rider region mismatch");
      }

      // Use rider's tenant/region if not provided in request
      String finalTenantId = tenantId != null ? tenantId : rider.getTenantId();
      String finalRegion = region != null ? region : rider.getRegion();

      // Create pickup and dropoff locations
      Location pickupLocation = new Location(request.getPickupLatitude(),
                                             request.getPickupLongitude(),
                                             request.getPickupAddress());

      Location dropoffLocation = new Location(request.getDropoffLatitude(),
                                              request.getDropoffLongitude(),
                                              request.getDropoffAddress());

      // Calculate surge pricing
      BigDecimal surgeMultiplier = surgePricingService.calculateSurgeMultiplier(
          request.getPickupLatitude().doubleValue(),
          request.getPickupLongitude().doubleValue(), finalRegion);

      // Calculate base fare
      BigDecimal baseFare = fareCalculationService.calculateBaseFare(
          pickupLocation, dropoffLocation, request.getTier());

      // Calculate total fare
      BigDecimal totalFare =
          fareCalculationService.calculateTotalFare(baseFare, surgeMultiplier);

      // Create ride entity
      Ride ride = new Ride();
      ride.setRideId(UUID.randomUUID().toString());
      ride.setRider(rider);
      ride.setPickupLocation(pickupLocation);
      ride.setDropoffLocation(dropoffLocation);
      ride.setTier(request.getTier());
      ride.setPaymentMethod(request.getPaymentMethod());
      ride.setStatus(RideStatus.PENDING);
      ride.setBaseFare(baseFare);
      ride.setSurgeMultiplier(surgeMultiplier);
      ride.setTotalFare(totalFare);
      ride.setTenantId(finalTenantId);
      ride.setRegion(finalRegion);
      ride.setRequestedAt(LocalDateTime.now());

      // Attempt to match with a driver (within 1s p95 requirement)
      // Use distributed lock to ensure atomic driver allocation
      Driver matchedDriver =
          allocateDriverAtomically(pickupLocation, finalTenantId, finalRegion);

      if (matchedDriver != null) {
        ride.setDriver(matchedDriver);
        ride.setStatus(RideStatus.MATCHED);
        ride.setMatchedAt(LocalDateTime.now());
      }

      // Save ride (track database latency)
      // Use SERIALIZABLE isolation for critical writes to prevent race
      // conditions
      long dbStart = System.currentTimeMillis();
      ride = saveRideWithRetry(ride);
      newRelicMetrics.recordDatabaseQuery("saveRide",
                                          System.currentTimeMillis() - dbStart);

      // Track business metrics
      newRelicMetrics.incrementRideRequests(finalRegion,
                                            request.getTier().toString());

      // Cache the ride and invalidate related caches
      cacheService.cacheRide(ride.getRideId(), ride);
      if (matchedDriver != null) {
        cacheInvalidationService.invalidateDriverCaches(
            matchedDriver.getId(), finalTenantId, finalRegion);
      }

      // Send notification asynchronously
      notificationService.notifyRideCreated(ride);

      // Convert to DTO and return
      RideResponseDTO response = convertToResponseDTO(ride);

      // Cache response for idempotency
      if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
        try {
          String responseJson = objectMapper.writeValueAsString(response);
          idempotencyService.cacheResponse(idempotencyKey, responseJson);
        } catch (Exception e) {
          // Log but don't fail
          System.err.println("Failed to cache idempotency response: " +
                             e.getMessage());
        }
        idempotencyService.releaseLock(idempotencyKey);
      }

      // Send notification if driver matched
      if (ride.getDriver() != null) {
        notificationService.notifyDriverMatched(ride);
        newRelicMetrics.incrementRideMatches(finalRegion);
        newRelicMetrics.incrementDriverMatches(finalRegion);
      } else {
        newRelicMetrics.incrementMatchingFailures(finalRegion,
                                                  "no_driver_available");
      }

      return response;
    } catch (Exception e) {
      if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
        idempotencyService.releaseLock(idempotencyKey);
      }
      throw e;
    }
  }

  /**
   * Convert Ride entity to RideResponseDTO
   */
  private RideResponseDTO convertToResponseDTO(Ride ride) {
    RideResponseDTO dto = new RideResponseDTO();
    dto.setRideId(ride.getRideId());
    dto.setRiderId(ride.getRider().getId());

    if (ride.getDriver() != null) {
      dto.setDriverId(ride.getDriver().getId());
      dto.setDriverName(ride.getDriver().getName());
      dto.setDriverPhoneNumber(ride.getDriver().getPhoneNumber());
      dto.setVehicleNumber(ride.getDriver().getVehicleNumber());
      dto.setVehicleModel(ride.getDriver().getVehicleModel());
    }

    if (ride.getPickupLocation() != null) {
      dto.setPickupLatitude(ride.getPickupLocation().getLatitude());
      dto.setPickupLongitude(ride.getPickupLocation().getLongitude());
      dto.setPickupAddress(ride.getPickupLocation().getAddress());
    }

    if (ride.getDropoffLocation() != null) {
      dto.setDropoffLatitude(ride.getDropoffLocation().getLatitude());
      dto.setDropoffLongitude(ride.getDropoffLocation().getLongitude());
      dto.setDropoffAddress(ride.getDropoffLocation().getAddress());
    }

    dto.setStatus(ride.getStatus());
    dto.setTier(ride.getTier());
    dto.setPaymentMethod(ride.getPaymentMethod());
    dto.setBaseFare(ride.getBaseFare());
    dto.setSurgeMultiplier(ride.getSurgeMultiplier());
    dto.setTotalFare(ride.getTotalFare());
    dto.setRequestedAt(ride.getRequestedAt());
    dto.setMatchedAt(ride.getMatchedAt());
    dto.setStartedAt(ride.getStartedAt());
    dto.setCompletedAt(ride.getCompletedAt());
    dto.setTenantId(ride.getTenantId());
    dto.setRegion(ride.getRegion());

    return dto;
  }

  /**
   * Get ride by ID (with caching)
   */
  @Transactional(readOnly = true)
  public RideResponseDTO getRide(String rideId) {
    // Try cache first
    Ride ride = cacheService.getCachedRide(rideId);

    if (ride == null) {
      ride = rideRepository.findByRideId(rideId).orElseThrow(
          () -> new RuntimeException("Ride not found with ID: " + rideId));
      // Cache for future requests
      cacheService.cacheRide(rideId, ride);
    }

    return convertToResponseDTO(ride);
  }

  /**
   * Mark driver as arriving at pickup location
   */
  @Transactional
  public RideResponseDTO markDriverArriving(String rideId, Long driverId) {
    Ride ride = rideRepository.findByRideId(rideId).orElseThrow(
        () -> new RuntimeException("Ride not found with ID: " + rideId));

    if (ride.getDriver() == null ||
        !ride.getDriver().getId().equals(driverId)) {
      throw new RuntimeException("Driver not authorized for this ride");
    }

    // Use state machine to validate transition
    stateMachine.validateTransition(ride.getStatus(),
                                    RideStatus.DRIVER_ARRIVING);

    RideStatus previousStatus = ride.getStatus();
    ride.setStatus(RideStatus.DRIVER_ARRIVING);
    ride = rideRepository.save(ride);

    // Invalidate caches and notify
    cacheInvalidationService.invalidateRideCaches(rideId, ride.getTenantId(),
                                                  ride.getRegion());
    notificationService.notifyRideStatusChanged(ride,
                                                previousStatus.toString());

    return convertToResponseDTO(ride);
  }

  /**
   * Start a ride (driver picks up the rider)
   */
  @Transactional
  public RideResponseDTO startRide(String rideId, Long driverId) {
    Ride ride = rideRepository.findByRideId(rideId).orElseThrow(
        () -> new RuntimeException("Ride not found with ID: " + rideId));

    if (ride.getDriver() == null ||
        !ride.getDriver().getId().equals(driverId)) {
      throw new RuntimeException("Driver not authorized for this ride");
    }

    // Use state machine to validate transition
    stateMachine.validateTransition(ride.getStatus(), RideStatus.IN_PROGRESS);

    RideStatus previousStatus = ride.getStatus();
    ride.setStatus(RideStatus.IN_PROGRESS);
    ride.setStartedAt(LocalDateTime.now());
    ride = rideRepository.save(ride);

    // Invalidate caches and notify
    cacheInvalidationService.invalidateRideCaches(rideId, ride.getTenantId(),
                                                  ride.getRegion());
    notificationService.notifyRideStatusChanged(ride,
                                                previousStatus.toString());

    return convertToResponseDTO(ride);
  }

  /**
   * Pause a ride
   */
  @Transactional
  public RideResponseDTO pauseRide(String rideId, Long driverId) {
    Ride ride = rideRepository.findByRideId(rideId).orElseThrow(
        () -> new RuntimeException("Ride not found with ID: " + rideId));

    if (ride.getDriver() == null ||
        !ride.getDriver().getId().equals(driverId)) {
      throw new RuntimeException("Driver not authorized for this ride");
    }

    // Use state machine to validate transition
    stateMachine.validateTransition(ride.getStatus(), RideStatus.PAUSED);

    RideStatus previousStatus = ride.getStatus();
    ride.setStatus(RideStatus.PAUSED);
    ride = rideRepository.save(ride);

    // Invalidate caches and notify
    cacheInvalidationService.invalidateRideCaches(rideId, ride.getTenantId(),
                                                  ride.getRegion());
    notificationService.notifyRideStatusChanged(ride,
                                                previousStatus.toString());

    return convertToResponseDTO(ride);
  }

  /**
   * Complete a ride
   */
  @Transactional
  public RideResponseDTO completeRide(String rideId, Long driverId) {
    Ride ride = rideRepository.findByRideId(rideId).orElseThrow(
        () -> new RuntimeException("Ride not found with ID: " + rideId));

    if (ride.getDriver() == null ||
        !ride.getDriver().getId().equals(driverId)) {
      throw new RuntimeException("Driver not authorized for this ride");
    }

    // Use state machine to validate transition
    stateMachine.validateTransition(ride.getStatus(), RideStatus.COMPLETED);

    RideStatus previousStatus = ride.getStatus();
    ride.setStatus(RideStatus.COMPLETED);
    ride.setCompletedAt(LocalDateTime.now());

    // Free up the driver
    Driver driver = ride.getDriver();
    driver.setStatus(DriverStatus.ONLINE);
    driverRepository.save(driver);
    cacheService.evictDriver(driver.getId());

    ride = rideRepository.save(ride);

    // Track business metrics
    newRelicMetrics.incrementRideCompletions(ride.getRegion());

    // Invalidate caches and notify
    cacheInvalidationService.invalidateRideCaches(rideId, ride.getTenantId(),
                                                  ride.getRegion());
    if (driver != null) {
      cacheInvalidationService.invalidateDriverCaches(
          driver.getId(), ride.getTenantId(), ride.getRegion());
    }
    notificationService.notifyRideStatusChanged(ride,
                                                previousStatus.toString());
    notificationService.notifyRideCompleted(ride);

    return convertToResponseDTO(ride);
  }

  /**
   * Cancel a ride
   */
  @Transactional
  public RideResponseDTO cancelRide(String rideId, Long userId,
                                    String userType) {
    Ride ride = rideRepository.findByRideId(rideId).orElseThrow(
        () -> new RuntimeException("Ride not found with ID: " + rideId));

    // Validate user authorization
    if ("rider".equals(userType)) {
      if (!ride.getRider().getId().equals(userId)) {
        throw new RuntimeException("Rider not authorized for this ride");
      }
    } else if ("driver".equals(userType)) {
      if (ride.getDriver() == null ||
          !ride.getDriver().getId().equals(userId)) {
        throw new RuntimeException("Driver not authorized for this ride");
      }
    } else {
      throw new RuntimeException(
          "Invalid user type. Must be 'rider' or 'driver'");
    }

    // Use state machine to validate transition
    stateMachine.validateTransition(ride.getStatus(), RideStatus.CANCELLED);

    RideStatus previousStatus = ride.getStatus();
    ride.setStatus(RideStatus.CANCELLED);
    ride.setCancelledAt(LocalDateTime.now());

    // Free up the driver if matched
    if (ride.getDriver() != null) {
      Driver driver = ride.getDriver();
      driver.setStatus(DriverStatus.ONLINE);
      driverRepository.save(driver);
    }

    ride = rideRepository.save(ride);

    // Track business metrics
    newRelicMetrics.incrementRideCancellations(ride.getRegion(), userType);

    // Invalidate all related caches
    cacheInvalidationService.invalidateRideCaches(rideId, ride.getTenantId(),
                                                  ride.getRegion());
    if (ride.getDriver() != null) {
      cacheInvalidationService.invalidateDriverCaches(
          ride.getDriver().getId(), ride.getTenantId(), ride.getRegion());
    }
    notificationService.notifyRideStatusChanged(ride,
                                                previousStatus.toString());

    return convertToResponseDTO(ride);
  }

  /**
   * Allocate a driver atomically using distributed lock
   */
  private Driver allocateDriverAtomically(Location pickupLocation,
                                          String tenantId, String region) {
    String lockKey = "driver_allocation:" + tenantId + ":" + region;

    DistributedLockService.LockHandle lockHandle =
        distributedLockService.acquireLock(lockKey, Duration.ofSeconds(5));

    if (lockHandle != null) {
      try {
        return driverMatchingService.findBestDriver(pickupLocation, tenantId,
                                                    region);
      } finally {
        distributedLockService.releaseLock(lockHandle);
      }
    }
    return null;
  }

  /**
   * Save ride with retry logic for optimistic locking failures
   */
  @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3,
             backoff = @Backoff(delay = 100, multiplier = 2))
  private Ride saveRideWithRetry(Ride ride) {
    return rideRepository.save(ride);
  }
}
