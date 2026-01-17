package com.example.ride_hail_app.model;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class Location {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;

    public Location() {
    }

    public Location(BigDecimal latitude, BigDecimal longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Calculate distance in kilometers using Haversine formula
     */
    public double distanceTo(Location other) {
        if (this.latitude == null || this.longitude == null ||
            other.latitude == null || other.longitude == null) {
            return Double.MAX_VALUE;
        }

        final int EARTH_RADIUS_KM = 6371;
        
        double lat1 = Math.toRadians(this.latitude.doubleValue());
        double lat2 = Math.toRadians(other.latitude.doubleValue());
        double deltaLat = Math.toRadians(other.latitude.doubleValue() - this.latitude.doubleValue());
        double deltaLon = Math.toRadians(other.longitude.doubleValue() - this.longitude.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}

