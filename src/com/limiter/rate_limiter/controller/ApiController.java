package com.limiter.rate_limiter.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.limiter.rate_limiter.annotation.RateLimit;

@RestController
@RequestMapping("/api")
public class ApiController {


	
	@GetMapping("/info")
	@RateLimit(capacity = 5, refillTokensPerSecond = 1)  // defining custom values for property 
	public ResponseEntity<String>  getInfo() {
		return ResponseEntity.ok("hello_gautam");
	}
	
	
	@GetMapping("/data")
	@RateLimit(capacity = 3, refillTokensPerSecond = 1)
	public ResponseEntity<String>  getData() {
		return ResponseEntity.ok("hello_gautam");
	}
	
}
