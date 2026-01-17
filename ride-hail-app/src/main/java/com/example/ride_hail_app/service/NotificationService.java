package com.example.ride_hail_app.service;

import com.example.ride_hail_app.model.Ride;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications asynchronously
 * Uses Kafka for async messaging (can be replaced with other messaging systems)
 * Gracefully handles Kafka unavailability
 */
@Service
public class NotificationService {
    
    private static final String RIDE_EVENTS_TOPIC = "ride-events";
    private static final String NOTIFICATIONS_TOPIC = "notifications";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public NotificationService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    // No-arg constructor for when Kafka is not available
    public NotificationService() {
        this.kafkaTemplate = null;
    }
    
    /**
     * Send notification asynchronously when ride is created
     */
    @Async("taskExecutor")
    public void notifyRideCreated(Ride ride) {
        if (kafkaTemplate == null) {
            // Kafka not available, log and continue
            System.out.println("Kafka not available, skipping notification for ride: " + ride.getRideId());
            return;
        }
        try {
            NotificationEvent event = new NotificationEvent(
                "RIDE_CREATED",
                ride.getRideId(),
                ride.getRider().getId(),
                "Your ride request has been received"
            );
            kafkaTemplate.send(RIDE_EVENTS_TOPIC, ride.getRideId(), event);
        } catch (Exception e) {
            // Log error but don't fail the main operation
            System.err.println("Failed to send ride created notification: " + e.getMessage());
        }
    }
    
    /**
     * Send notification when driver is matched
     */
    @Async("taskExecutor")
    public void notifyDriverMatched(Ride ride) {
        if (kafkaTemplate == null) return;
        try {
            if (ride.getDriver() != null) {
                // Notify rider
                NotificationEvent riderEvent = new NotificationEvent(
                    "DRIVER_MATCHED",
                    ride.getRideId(),
                    ride.getRider().getId(),
                    "Driver " + ride.getDriver().getName() + " has been assigned to your ride"
                );
                kafkaTemplate.send(NOTIFICATIONS_TOPIC, "rider:" + ride.getRider().getId(), riderEvent);
                
                // Notify driver
                NotificationEvent driverEvent = new NotificationEvent(
                    "RIDE_ASSIGNED",
                    ride.getRideId(),
                    ride.getDriver().getId(),
                    "You have been assigned a new ride"
                );
                kafkaTemplate.send(NOTIFICATIONS_TOPIC, "driver:" + ride.getDriver().getId(), driverEvent);
            }
        } catch (Exception e) {
            System.err.println("Failed to send driver matched notification: " + e.getMessage());
        }
    }
    
    /**
     * Send notification when ride status changes
     */
    @Async("taskExecutor")
    public void notifyRideStatusChanged(Ride ride, String previousStatus) {
        if (kafkaTemplate == null) return;
        try {
            NotificationEvent event = new NotificationEvent(
                "RIDE_STATUS_CHANGED",
                ride.getRideId(),
                ride.getRider().getId(),
                "Ride status changed from " + previousStatus + " to " + ride.getStatus()
            );
            kafkaTemplate.send(RIDE_EVENTS_TOPIC, ride.getRideId(), event);
        } catch (Exception e) {
            System.err.println("Failed to send status change notification: " + e.getMessage());
        }
    }
    
    /**
     * Send notification when ride is completed
     */
    @Async("taskExecutor")
    public void notifyRideCompleted(Ride ride) {
        if (kafkaTemplate == null) return;
        try {
            NotificationEvent event = new NotificationEvent(
                "RIDE_COMPLETED",
                ride.getRideId(),
                ride.getRider().getId(),
                "Your ride has been completed. Total fare: " + ride.getTotalFare()
            );
            kafkaTemplate.send(NOTIFICATIONS_TOPIC, "rider:" + ride.getRider().getId(), event);
        } catch (Exception e) {
            System.err.println("Failed to send ride completed notification: " + e.getMessage());
        }
    }
    
    /**
     * Notification event DTO
     */
    public static class NotificationEvent {
        private String eventType;
        private String rideId;
        private Long userId;
        private String message;
        private long timestamp;
        
        public NotificationEvent(String eventType, String rideId, Long userId, String message) {
            this.eventType = eventType;
            this.rideId = rideId;
            this.userId = userId;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters and Setters
        public String getEventType() {
            return eventType;
        }
        
        public void setEventType(String eventType) {
            this.eventType = eventType;
        }
        
        public String getRideId() {
            return rideId;
        }
        
        public void setRideId(String rideId) {
            this.rideId = rideId;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}

