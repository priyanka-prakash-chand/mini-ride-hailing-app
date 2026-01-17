package com.example.ride_hail_app.service;

import com.example.ride_hail_app.model.PaymentMethod;
import com.example.ride_hail_app.model.Ride;
import com.example.ride_hail_app.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for payment processing via external PSPs (Payment Service Providers)
 * In production, this would integrate with payment gateways like Stripe, Razorpay, etc.
 */
@Service
public class PaymentService {
    
    private final RideRepository rideRepository;
    
    public PaymentService(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }
    
    /**
     * Process payment for a completed ride
     * In production, this would call external PSP APIs
     */
    @Transactional
    public PaymentResult processPayment(String rideId, String paymentToken) {
        Ride ride = rideRepository.findByRideId(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found with ID: " + rideId));
        
        if (ride.getStatus() != com.example.ride_hail_app.model.RideStatus.COMPLETED) {
            throw new RuntimeException("Payment can only be processed for completed rides");
        }
        
        // Simulate payment processing
        // In production, this would:
        // 1. Call PSP API (Stripe, Razorpay, etc.)
        // 2. Handle payment gateway response
        // 3. Update payment status in database
        // 4. Send receipt
        
        boolean success = simulatePaymentProcessing(ride, paymentToken);
        
        if (success) {
            return new PaymentResult(
                true,
                "Payment processed successfully",
                generateTransactionId(),
                ride.getTotalFare()
            );
        } else {
            return new PaymentResult(
                false,
                "Payment processing failed",
                null,
                null
            );
        }
    }
    
    /**
     * Simulate payment processing
     * In production, replace with actual PSP integration
     */
    private boolean simulatePaymentProcessing(Ride ride, String paymentToken) {
        // Simulate payment processing logic
        // For cash payments, always succeed
        if (ride.getPaymentMethod() == PaymentMethod.CASH) {
            return true;
        }
        
        // For other payment methods, simulate success (90% success rate for demo)
        return Math.random() > 0.1;
    }
    
    /**
     * Generate a transaction ID
     */
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis();
    }
    
    /**
     * Payment result DTO
     */
    public static class PaymentResult {
        private boolean success;
        private String message;
        private String transactionId;
        private java.math.BigDecimal amount;
        
        public PaymentResult(boolean success, String message, String transactionId, java.math.BigDecimal amount) {
            this.success = success;
            this.message = message;
            this.transactionId = transactionId;
            this.amount = amount;
        }
        
        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getTransactionId() {
            return transactionId;
        }
        
        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }
        
        public java.math.BigDecimal getAmount() {
            return amount;
        }
        
        public void setAmount(java.math.BigDecimal amount) {
            this.amount = amount;
        }
    }
}

