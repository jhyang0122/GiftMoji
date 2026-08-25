package com.giftmoji.giftmoji.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

	// Static index.html serves "/" (the PWA app shell); this stays off that path.
	@GetMapping("/api/status")
	public Map<String, String> home() {
		return Map.of(
				"service", "GiftMoji",
				"status", "running"
		);
	}

}
