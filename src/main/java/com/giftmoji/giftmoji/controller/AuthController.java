package com.giftmoji.giftmoji.controller;

import com.giftmoji.giftmoji.api.UserResponse;
import com.giftmoji.giftmoji.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserRepository userRepository;

	public AuthController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	// Logout is handled by Spring Security's LogoutFilter directly
	// (see SecurityConfig: logoutUrl "/api/auth/logout"), not here.
	@GetMapping("/me")
	public ResponseEntity<UserResponse> me(@AuthenticationPrincipal OidcUser principal) {
		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return userRepository.findByGoogleId(principal.getSubject())
				.map(user -> ResponseEntity.ok(UserResponse.from(user)))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
	}
}
