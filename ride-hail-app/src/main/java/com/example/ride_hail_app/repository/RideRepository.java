package com.example.ride_hail_app.repository;

import com.example.ride_hail_app.model.Ride;
import com.example.ride_hail_app.model.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    Optional<Ride> findByRideId(String rideId);
    
    List<Ride> findByRiderIdAndStatusIn(Long riderId, List<RideStatus> statuses);
    
    List<Ride> findByDriverIdAndStatusIn(Long driverId, List<RideStatus> statuses);
}

