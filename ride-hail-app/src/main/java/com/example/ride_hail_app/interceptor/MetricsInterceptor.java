package com.example.ride_hail_app.interceptor;

import com.example.ride_hail_app.monitoring.NewRelicMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to track API latencies and metrics
 */
@Component
public class MetricsInterceptor implements HandlerInterceptor {
    
    @Autowired
    private NewRelicMetrics newRelicMetrics;
    
    private static final ThreadLocal<Long> startTime = new ThreadLocal<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        startTime.set(System.currentTimeMillis());
        return true;
    }
    
    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception ex
    ) {
        Long start = startTime.get();
        if (start != null) {
            long duration = System.currentTimeMillis() - start;
            String endpoint = request.getRequestURI();
            String method = request.getMethod();
            String region = request.getHeader("X-Region");
            String tenantId = request.getHeader("X-Tenant-Id");
            
            // Record API latency
            newRelicMetrics.recordApiLatency(endpoint, method, duration, region, tenantId);
            
            // Track errors
            if (response.getStatus() >= 400) {
                String errorType = response.getStatus() >= 500 ? "server_error" : "client_error";
                newRelicMetrics.incrementApiErrors(endpoint, errorType);
            }
            
            startTime.remove();
        }
    }
}

