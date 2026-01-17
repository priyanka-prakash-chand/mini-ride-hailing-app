package com.example.ride_hail_app.statemachine;

import com.example.ride_hail_app.model.RideStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * State machine for managing clean ride state transitions
 * Ensures only valid state transitions are allowed
 */
@Component
public class RideStateMachine {
    
    // Define valid state transitions
    private static final Map<RideStatus, Set<RideStatus>> VALID_TRANSITIONS = new HashMap<>();
    
    static {
        // From PENDING, can go to MATCHED or CANCELLED
        VALID_TRANSITIONS.put(RideStatus.PENDING, EnumSet.of(
            RideStatus.MATCHED,
            RideStatus.CANCELLED
        ));
        
        // From MATCHED, can go to DRIVER_ARRIVING, CANCELLED
        VALID_TRANSITIONS.put(RideStatus.MATCHED, EnumSet.of(
            RideStatus.DRIVER_ARRIVING,
            RideStatus.CANCELLED
        ));
        
        // From DRIVER_ARRIVING, can go to IN_PROGRESS, CANCELLED
        VALID_TRANSITIONS.put(RideStatus.DRIVER_ARRIVING, EnumSet.of(
            RideStatus.IN_PROGRESS,
            RideStatus.CANCELLED
        ));
        
        // From IN_PROGRESS, can go to PAUSED, COMPLETED, CANCELLED
        VALID_TRANSITIONS.put(RideStatus.IN_PROGRESS, EnumSet.of(
            RideStatus.PAUSED,
            RideStatus.COMPLETED,
            RideStatus.CANCELLED
        ));
        
        // From PAUSED, can go back to IN_PROGRESS, COMPLETED, CANCELLED
        VALID_TRANSITIONS.put(RideStatus.PAUSED, EnumSet.of(
            RideStatus.IN_PROGRESS,
            RideStatus.COMPLETED,
            RideStatus.CANCELLED
        ));
        
        // Terminal states - no transitions allowed
        VALID_TRANSITIONS.put(RideStatus.COMPLETED, EnumSet.noneOf(RideStatus.class));
        VALID_TRANSITIONS.put(RideStatus.CANCELLED, EnumSet.noneOf(RideStatus.class));
    }
    
    /**
     * Check if a state transition is valid
     */
    public boolean isValidTransition(RideStatus currentStatus, RideStatus newStatus) {
        if (currentStatus == newStatus) {
            return true; // Same state is always valid
        }
        
        Set<RideStatus> allowedTransitions = VALID_TRANSITIONS.get(currentStatus);
        return allowedTransitions != null && allowedTransitions.contains(newStatus);
    }
    
    /**
     * Validate and throw exception if transition is invalid
     */
    public void validateTransition(RideStatus currentStatus, RideStatus newStatus) {
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid state transition from %s to %s", currentStatus, newStatus)
            );
        }
    }
    
    /**
     * Get all valid next states for a given state
     */
    public Set<RideStatus> getValidNextStates(RideStatus currentStatus) {
        return VALID_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(RideStatus.class));
    }
}

