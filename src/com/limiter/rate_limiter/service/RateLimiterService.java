package com.limiter.rate_limiter.service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.limiter.rate_limiter.core.RateLimitProperties;
import com.limiter.rate_limiter.core.TokenBucket;

@Service
public class RateLimiterService {
	
//	ConcurrentHashMap<String, TokenBucket> buckets=new ConcurrentHashMap<>();
	
	@Autowired
    private RedisTemplate<String, Object> redisTemplate;
	
	@Autowired
	private RateLimitProperties rateLimitProperties;
	
//	@Scheduled(fixedRate=300000) // setting scheduler of 5 min
//	public void cleanOldBuckets() {
//		long now=System.currentTimeMillis();
//		long expiry= 10*60*1000;   // removing bucket that stayed inactive for more than 10 minutes
//		
//		buckets.entrySet().removeIf(e->now-e.getValue().getLastAccessTime() > expiry);
//	}
	
	public TokenBucket resolveBucket(String userKey, int capacity, double refillRate) {
//		return buckets.computeIfAbsent(userKey, k ->new TokenBucket(capacity, refillRate));
		
		String redisKey="bucket"+userKey;
		TokenBucket bucket=(TokenBucket)redisTemplate.opsForValue().get(redisKey);
		
		if(bucket==null || capacity!=bucket.getCapacity() || refillRate!=bucket.getRefillRate()) {
			bucket=new TokenBucket(capacity,refillRate);
		}
		
		bucket.setLastAccessTime(Instant.now().toEpochMilli());
		return bucket;
		
		
	}
	
	public boolean tryConsume(String userKey, int capacity, double refillRate) {
		TokenBucket bucket=resolveBucket(userKey,capacity,refillRate);
		boolean allowed=bucket.tryConsume();
		String redisKey="bucket"+userKey;
		
		redisTemplate.opsForValue().set(redisKey, bucket,10,TimeUnit.MINUTES);
		return allowed;
	}

}
