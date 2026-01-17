package com.example.ride_hail_app.config;

import com.example.ride_hail_app.interceptor.MetricsInterceptor;
import com.example.ride_hail_app.interceptor.RateLimitingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for interceptors and stateless setup
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private RateLimitingInterceptor rateLimitingInterceptor;
    
    @Autowired
    private MetricsInterceptor metricsInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Metrics interceptor should be first to capture all requests
        // Exclude static resources and root path from interceptors
        registry.addInterceptor(metricsInterceptor)
                .excludePathPatterns("/", "/index.html", "/static/**", "/css/**", "/js/**", "/images/**");
        registry.addInterceptor(rateLimitingInterceptor)
                .excludePathPatterns("/", "/index.html", "/static/**", "/css/**", "/js/**", "/images/**");
    }
}

