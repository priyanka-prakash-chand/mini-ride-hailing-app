package com.example.ride_hail_app.repository;

import com.example.ride_hail_app.model.Rider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RiderRepository extends JpaRepository<Rider, Long> {
    Optional<Rider> findByPhoneNumberAndTenantId(String phoneNumber, String tenantId);
}

