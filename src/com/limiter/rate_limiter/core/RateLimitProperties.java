package com.limiter.rate_limiter.core;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="ratelimit")
public class RateLimitProperties {


	private int capacity;
	

	private double refillTokensPerSecond;
	
	private long ttlMinutes;

	public int getCapacity() {
		return capacity;
	}

	public double getRefillTokensPerSecond() {
		return refillTokensPerSecond;
	}

	
	public long getTtlMinutes() {
		return ttlMinutes;
	}

	public void setTtlMinutes(long ttlMinutes) {
		this.ttlMinutes = ttlMinutes;
	}

	public void setCapacity(int capacity) { 
		this.capacity = capacity; 
	}
	public void setRefillTokensPerSecond(double refillTokensPerSecond) { 
		this.refillTokensPerSecond = refillTokensPerSecond; 
	}

	
	
	
}
