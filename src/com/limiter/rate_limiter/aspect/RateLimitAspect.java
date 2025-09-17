package com.limiter.rate_limiter.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.limiter.rate_limiter.annotation.RateLimit;
import com.limiter.rate_limiter.core.RateLimitProperties;
import com.limiter.rate_limiter.core.TokenBucket;
import com.limiter.rate_limiter.exception.TooManyRequestsException;
import com.limiter.rate_limiter.service.RateLimiterService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Aspect
@Component
public class RateLimitAspect {
	
	@Autowired
	private RateLimiterService rateLimiterService;
	
	@Autowired
	private RateLimitProperties rateLimitProperty;
	
	
	@Around("@annotation(rateLimit)")
	public Object applyRateLimiting(ProceedingJoinPoint pjp,RateLimit rateLimit) throws Throwable{
		
		
		
		// now we will check if token available or not
		
		// generate key for storing the user data
		HttpServletRequest request= ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String userKey= "IP_"+ request.getRemoteAddr();
		
		// Getting response object to customize header
		HttpServletResponse response= ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
		
		// now try to consume token with current key	
		int capacity = rateLimit.capacity() > 0 ? rateLimit.capacity() : rateLimitProperty.getCapacity(); // runtime value from properties
		double refillRate = rateLimit.refillTokensPerSecond() > 0 ? rateLimit.refillTokensPerSecond() : rateLimitProperty.getRefillTokensPerSecond();;
		
		System.out.println(capacity);
		boolean allowed = rateLimiterService.tryConsume(userKey, capacity,refillRate);
		TokenBucket bucket = rateLimiterService.resolveBucket(userKey, capacity,refillRate);
		if(allowed) {
			addRateLimitHeaders(response, bucket);
			return pjp.proceed();
		}
		else {
			addRateLimitHeaders(response, bucket);
			throw new TooManyRequestsException("Too many request, rate limit exceeded");
		}
	
		
	}
	
	public void addRateLimitHeaders(HttpServletResponse response, TokenBucket bucket) {
		
		long availableTokens= bucket.getAvailableTokens();
		long capacity= bucket.getCapacity();
		
		response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
		response.setHeader("X-RateLimit-Remaining", String.valueOf(availableTokens));
		
		long estimatedNextRefill= (long)Math.ceil((capacity-availableTokens)/bucket.getRefillRate());
		
		response.setHeader("X-RateLimit-Reset", String.valueOf(estimatedNextRefill));
		
	}
	
}
