package com.example.ride_hail_app.repository;

import com.example.ride_hail_app.model.Driver;
import com.example.ride_hail_app.model.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    
    List<Driver> findByStatusAndTenantIdAndRegion(
        DriverStatus status, 
        String tenantId, 
        String region
    );

    @Query("SELECT d FROM Driver d WHERE d.status = :status " +
           "AND d.tenantId = :tenantId AND d.region = :region " +
           "AND d.lastLocationUpdate IS NOT NULL " +
           "ORDER BY d.rating DESC")
    List<Driver> findAvailableDriversForMatching(
        @Param("status") DriverStatus status,
        @Param("tenantId") String tenantId,
        @Param("region") String region
    );
}

