package com.limiter.rate_limiter.core;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TokenBucket {
	
	private int capacity;
	private int availableTokens;
	private double refillRate;
	private long lastRefillTime;
	private long lastAccessTime;
	
	public TokenBucket(int capacity, double refillRate) {
		super();
		this.capacity = capacity;
		this.availableTokens = capacity;
		this.refillRate = refillRate;
		this.lastRefillTime = System.currentTimeMillis();
		this.lastAccessTime = this.lastRefillTime;
	}
	
	
	public synchronized boolean tryConsume() {
		
		// first tokens will be refilled acc. to time lapsed.
		refill();
		lastAccessTime=System.currentTimeMillis();
		if(this.availableTokens>0) {
			this.availableTokens--;
			return true;
		}
		return false;
	}
	
	private void refill() {
		long now = System.currentTimeMillis();
		double tokensToAdd=((now-lastRefillTime) / 1000.0)*refillRate;
		
		if(tokensToAdd>0) {
			availableTokens=Math.min(this.availableTokens+(int)tokensToAdd, capacity);
			lastRefillTime=now;
		}
		
	}
	

	public int getAvailableTokens() {
		return this.availableTokens;
	}


	public int getCapacity() {
		return capacity;
	}


	public double getRefillRate() {
		return refillRate;
	}


	public void setLastAccessTime(long lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}


	public long getLastAccessTime() {
		return this.lastAccessTime;
	}


	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}


	public void setAvailableTokens(int availableTokens) {
		this.availableTokens = availableTokens;
	}


	public void setRefillRate(double refillRate) {
		this.refillRate = refillRate;
	}


	public void setLastRefillTime(long lastRefillTime) {
		this.lastRefillTime = lastRefillTime;
	}


	public long getLastRefillTime() {
		return lastRefillTime;
	}
	
	
}
