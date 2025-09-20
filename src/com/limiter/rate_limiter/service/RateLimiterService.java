package com.limiter.rate_limiter.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import com.limiter.rate_limiter.core.Result;
import com.limiter.rate_limiter.core.TokenBucket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;



@Service
public class RateLimiterService {
	
	private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final long BUCKET_TTL_SECONDS = 10 * 60; // 10 minutes
    
    private final Counter requestsAllowed;
    private final Counter requestsDenied;

    private final MeterRegistry registry;
    
    // Cache to avoid re-registering gauges for the same user
    private final Map<String, Boolean> gaugeRegistered = new ConcurrentHashMap<>();
    
    @Autowired
    public RateLimiterService(MeterRegistry registry) {
    	this.registry=registry;
        this.requestsAllowed = Counter.builder("rate_limiter_requests_allowed")
                .description("Number of requests allowed by rate limiter")
                .register(registry);

        this.requestsDenied = Counter.builder("rate_limiter_requests_denied")
                .description("Number of requests denied by rate limiter")
                .register(registry);
    }

    

    // Consuming a token Atomically using Redis transaction

    public Result tryConsume(String userKey, int capacity, double refillRate) {

        String redisKey = "bucket:" + userKey;
        int attempt = 0;

        while (attempt < 5) {
        	
        	int currentAttempt = attempt;  // since we need variable that is local inside the loop, to use in logging 
        	
            List<Object> results = redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings("unchecked")
                public <K, V> List<Object> execute(RedisOperations<K, V> operations) {

                    operations.watch((K) redisKey);

                    TokenBucket bucket = (TokenBucket) operations.opsForValue().get((K) redisKey);

                    long now = Instant.now().toEpochMilli();

                    if (bucket == null || bucket.getCapacity() != capacity || bucket.getRefillRate() != refillRate) {
                    	log.debug("Initializing new bucket for key={} with capacity={} and refillRate={}", redisKey, capacity, refillRate);
                        bucket = new TokenBucket(capacity, refillRate);
                    } 
                    else {
                        // refilling tokens based on elapsed time
                        int tokensToAdd = (int) (( (now - bucket.getLastRefillTime()) / 1000.0) * bucket.getRefillRate());
                        if (tokensToAdd > 0) {
                            bucket.setAvailableTokens(Math.min(bucket.getAvailableTokens() + tokensToAdd, capacity));
                            bucket.setLastRefillTime(now);
                            log.trace("Refilled {} tokens for key={} (available now={})", tokensToAdd, redisKey, bucket.getAvailableTokens());
                        }
                    }

                    // If no tokens left, no need to run transaction
                    if (bucket.getAvailableTokens() <= 0) {
                    	log.info("Request DENIED for key={} (no tokens available)", redisKey);
                        return List.of("DENIED", bucket);
                    }

                    // Consuming token
                    bucket.setAvailableTokens(bucket.getAvailableTokens() - 1);
                    bucket.setLastAccessTime(now);

                    // Starting transaction (redis)
                    operations.multi();
                    // staging this redis operation ,to check for concurrent access
                    operations.opsForValue().set((K) redisKey, (V) bucket, BUCKET_TTL_SECONDS, TimeUnit.SECONDS);

                    // now running the transaction
                    List<Object> execResults = operations.exec();
                    // if watch key is changed , then operations.exec() return null
                    if (execResults == null) { 
                    	// // transaction is failed, retry
                    	log.warn("Transaction conflict on key={} (attempt={})", redisKey, currentAttempt + 1);
                        return null; 
                    }

                    log.info("Request ALLOWED for key={} (tokens left={})", redisKey, bucket.getAvailableTokens());
                    return List.of("ALLOWED", bucket);
                }
            });

            if (results != null) {
                String status = (String) results.get(0);
                TokenBucket finalBucket = (TokenBucket) results.get(1);
                boolean allowed = "ALLOWED".equals(status);
                if (allowed) {
                    requestsAllowed.increment();
                } else {
                    requestsDenied.increment();
                }
                
                // --- Register gauge if not already done for this user ---
                gaugeRegistered.computeIfAbsent(userKey, key -> {
                    registry.gauge("rate_limiter_tokens_available", Tags.of("user", userKey), finalBucket, b -> b.getAvailableTokens());
                    return true;
                });
                
                return new Result(allowed, finalBucket);
            }

            
            try {
            	// small backoff to avoid tight retry loop
                Thread.sleep(50); 
            } catch (InterruptedException e) {
            	
                Thread.currentThread().interrupt();
                log.error("Retry loop interrupted for key={}", redisKey, e);
            }
            attempt++;
            
        }
        
        

        // giving up after 5 retries
        log.error("Giving up after {} attempts for key={}", attempt, redisKey);
        return new Result(false, null); 
    }
}




