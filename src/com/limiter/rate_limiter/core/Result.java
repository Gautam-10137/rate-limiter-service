package com.limiter.rate_limiter.core;

public class Result {
	
	private final boolean allowed;
    private final TokenBucket bucket;

    public Result(boolean allowed, TokenBucket bucket) {
        this.allowed = allowed;
        this.bucket = bucket;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public TokenBucket getBucket() {
        return bucket;
    }

}
