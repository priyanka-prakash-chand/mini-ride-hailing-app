package com.example.ride_hail_app.dto;

import com.example.ride_hail_app.model.PaymentMethod;
import com.example.ride_hail_app.model.RideTier;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class RideRequestDTO {
    @NotNull(message = "Rider ID is required")
    private Long riderId;

    @NotNull(message = "Pickup latitude is required")
    private BigDecimal pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    private BigDecimal pickupLongitude;

    private String pickupAddress;

    @NotNull(message = "Dropoff latitude is required")
    private BigDecimal dropoffLatitude;

    @NotNull(message = "Dropoff longitude is required")
    private BigDecimal dropoffLongitude;

    private String dropoffAddress;

    @NotNull(message = "Ride tier is required")
    private RideTier tier;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String tenantId; // Optional, can be extracted from headers
    private String region;    // Optional, can be extracted from headers

    // Getters and Setters
    public Long getRiderId() {
        return riderId;
    }

    public void setRiderId(Long riderId) {
        this.riderId = riderId;
    }

    public BigDecimal getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(BigDecimal pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public BigDecimal getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(BigDecimal pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public BigDecimal getDropoffLatitude() {
        return dropoffLatitude;
    }

    public void setDropoffLatitude(BigDecimal dropoffLatitude) {
        this.dropoffLatitude = dropoffLatitude;
    }

    public BigDecimal getDropoffLongitude() {
        return dropoffLongitude;
    }

    public void setDropoffLongitude(BigDecimal dropoffLongitude) {
        this.dropoffLongitude = dropoffLongitude;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public RideTier getTier() {
        return tier;
    }

    public void setTier(RideTier tier) {
        this.tier = tier;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}

