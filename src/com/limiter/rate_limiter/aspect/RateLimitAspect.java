package com.limiter.rate_limiter.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.limiter.rate_limiter.annotation.RateLimit;
import com.limiter.rate_limiter.core.RateLimitProperties;
import com.limiter.rate_limiter.core.Result;
import com.limiter.rate_limiter.core.TokenBucket;
import com.limiter.rate_limiter.exception.TooManyRequestsException;
import com.limiter.rate_limiter.service.RateLimiterService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Aspect
@Component
public class RateLimitAspect {
	
	private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private RateLimitProperties rateLimitProperty;

    @Around("@annotation(rateLimit)")
    public Object applyRateLimiting(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
    	long start = System.currentTimeMillis();

        // Get request and response objects
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();

        
        // Build user key based on IP
        String userKey = "IP_" + request.getRemoteAddr();

        // Determine capacity and refill rate
        int capacity = rateLimit.capacity() > 0 ? rateLimit.capacity() : rateLimitProperty.getCapacity();
        double refillRate = rateLimit.refillTokensPerSecond() > 0 ? rateLimit.refillTokensPerSecond() : rateLimitProperty.getRefillTokensPerSecond();

        // Atomically consume token
        Result result = rateLimiterService.tryConsume(userKey, capacity, refillRate);

        // Add custom rate limit headers
        addRateLimitHeaders(response, result.getBucket());
        
        // extracting current requested method 
        String methodName = pjp.getSignature().toShortString();

        // Proceed or throw exception
        if (result.isAllowed()) {
        	long duration = System.currentTimeMillis() - start;
        	log.info("Request allowed: IP={}, method={}, duration={}ms", userKey, methodName, duration);
            return pjp.proceed();
        } else {
        	log.warn("Rate limit exceeded for IP={} on method={}", userKey, methodName);
            throw new TooManyRequestsException("Too many requests, rate limit exceeded");
        }
    }

    private void addRateLimitHeaders(HttpServletResponse response, TokenBucket bucket) {
        if (bucket == null) {
            return; // Safety check
        }

        long availableTokens = bucket.getAvailableTokens();
        long capacity = bucket.getCapacity();

        response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(availableTokens));

        long estimatedNextRefill = (long) Math.ceil((capacity - availableTokens) / bucket.getRefillRate());
        response.setHeader("X-RateLimit-Reset", String.valueOf(estimatedNextRefill));
    }
}
