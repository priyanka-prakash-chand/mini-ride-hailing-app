package com.example.ride_hail_app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enable Spring Retry for handling optimistic locking failures
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Configuration for retry behavior is done via @Retryable annotations
}

