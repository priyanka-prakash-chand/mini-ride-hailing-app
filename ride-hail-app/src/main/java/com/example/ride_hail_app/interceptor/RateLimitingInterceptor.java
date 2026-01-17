package com.example.ride_hail_app.interceptor;

import com.example.ride_hail_app.config.RateLimitingConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for rate limiting API requests
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RateLimitingConfig.RateLimiter rateLimiter;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String region = request.getHeader("X-Region");
        
        // Rate limit ride creation requests
        if (path.startsWith("/v1/rides") && "POST".equals(request.getMethod())) {
            String rateLimitKey = region != null ? region : "default";
            if (!rateLimiter.isRideRequestAllowed(rateLimitKey)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", "60");
                return false;
            }
        }
        
        // Rate limit location updates
        if (path.contains("/location") && "PUT".equals(request.getMethod())) {
            if (!rateLimiter.isLocationUpdateAllowed()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", "1");
                return false;
            }
        }
        
        return true;
    }
}

