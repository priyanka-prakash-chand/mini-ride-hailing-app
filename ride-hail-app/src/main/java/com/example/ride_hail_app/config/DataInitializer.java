package com.example.ride_hail_app.config;

import com.example.ride_hail_app.model.*;
import com.example.ride_hail_app.repository.DriverRepository;
import com.example.ride_hail_app.repository.RiderRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Data initializer to create test data for development
 * Only runs in 'dev' profile or when explicitly enabled
 */
@Component
@Profile("!prod")
public class DataInitializer {
    
    @Autowired
    private RiderRepository riderRepository;
    
    @Autowired
    private DriverRepository driverRepository;
    
    @PostConstruct
    public void init() {
        // Only initialize if database is empty
        if (riderRepository.count() == 0 && driverRepository.count() == 0) {
            createTestData();
        }
    }
    
    private void createTestData() {
        // Create test riders
        Rider rider1 = new Rider();
        rider1.setName("John Doe");
        rider1.setPhoneNumber("+1234567890");
        rider1.setEmail("john.doe@example.com");
        rider1.setTenantId("tenant1");
        rider1.setRegion("us-east");
        riderRepository.save(rider1);
        
        Rider rider2 = new Rider();
        rider2.setName("Jane Smith");
        rider2.setPhoneNumber("+1234567891");
        rider2.setEmail("jane.smith@example.com");
        rider2.setTenantId("tenant1");
        rider2.setRegion("us-east");
        riderRepository.save(rider2);
        
        // Create test drivers
        Driver driver1 = new Driver();
        driver1.setName("Mike Johnson");
        driver1.setPhoneNumber("+1987654321");
        driver1.setVehicleNumber("ABC-1234");
        driver1.setVehicleModel("Toyota Camry");
        driver1.setStatus(DriverStatus.ONLINE);
        driver1.setTenantId("tenant1");
        driver1.setRegion("us-east");
        driver1.setRating(BigDecimal.valueOf(4.8));
        driver1.setCurrentLocation(new Location(
            BigDecimal.valueOf(40.7128),  // NYC coordinates
            BigDecimal.valueOf(-74.0060),
            "New York, NY"
        ));
        driverRepository.save(driver1);
        
        Driver driver2 = new Driver();
        driver2.setName("Sarah Williams");
        driver2.setPhoneNumber("+1987654322");
        driver2.setVehicleNumber("XYZ-5678");
        driver2.setVehicleModel("Honda Accord");
        driver2.setStatus(DriverStatus.ONLINE);
        driver2.setTenantId("tenant1");
        driver2.setRegion("us-east");
        driver2.setRating(BigDecimal.valueOf(4.9));
        driver2.setCurrentLocation(new Location(
            BigDecimal.valueOf(40.7589),  // Times Square area
            BigDecimal.valueOf(-73.9851),
            "Times Square, NY"
        ));
        driverRepository.save(driver2);
        
        Driver driver3 = new Driver();
        driver3.setName("David Brown");
        driver3.setPhoneNumber("+1987654323");
        driver3.setVehicleNumber("DEF-9012");
        driver3.setVehicleModel("Tesla Model 3");
        driver3.setStatus(DriverStatus.ONLINE);
        driver3.setTenantId("tenant1");
        driver3.setRegion("us-east");
        driver3.setRating(BigDecimal.valueOf(5.0));
        driver3.setCurrentLocation(new Location(
            BigDecimal.valueOf(40.7614),  // Central Park area
            BigDecimal.valueOf(-73.9776),
            "Central Park, NY"
        ));
        driverRepository.save(driver3);
        
        System.out.println("Test data initialized:");
        System.out.println("- 2 Riders created (IDs: 1, 2)");
        System.out.println("- 3 Drivers created (IDs: 1, 2, 3)");
    }
}

