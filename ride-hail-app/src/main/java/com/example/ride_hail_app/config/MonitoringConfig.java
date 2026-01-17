package com.example.ride_hail_app.config;

import com.example.ride_hail_app.interceptor.MetricsInterceptor;
import com.example.ride_hail_app.monitoring.NewRelicMetrics;
import com.example.ride_hail_app.repository.DriverRepository;
import com.example.ride_hail_app.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for New Relic monitoring
 * Sets up custom metrics, database monitoring, and alerts
 */
@Configuration
@EnableScheduling
public class MonitoringConfig implements WebMvcConfigurer {
    
    @Autowired
    private MetricsInterceptor metricsInterceptor;
    
    @Autowired
    private NewRelicMetrics newRelicMetrics;
    
    @Autowired(required = false)
    private DriverRepository driverRepository;
    
    @Autowired(required = false)
    private RideRepository rideRepository;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(metricsInterceptor);
    }
    
    /**
     * Register gauges for real-time metrics
     * Updates every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void updateGauges() {
        if (driverRepository != null && rideRepository != null) {
            // Register active drivers gauge per region
            // In production, query by region
            newRelicMetrics.registerActiveDriversGauge("us-east", () -> 
                driverRepository.count()
            );
            
            // Register pending rides gauge
            newRelicMetrics.registerPendingRidesGauge("us-east", () -> 
                rideRepository.count()
            );
        }
    }
}

