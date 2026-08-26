package com.giftmoji.giftmoji.controller;

import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Test
	void unauthenticatedMeReturns401() throws Exception {
		mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void staticShellAndExistingApiStayPubliclyAccessible() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk());
		mockMvc.perform(get("/api/status")).andExpect(status().isOk());
	}

	@Test
	void authenticatedMeReturnsPersistedUser() throws Exception {
		userRepository.save(User.createFromGoogle("google-sub-123", "a@b.com", "Ada", null));

		mockMvc.perform(get("/api/auth/me").with(oidcLogin()
						.idToken(token -> token.claim("sub", "google-sub-123"))
						.userInfoToken(token -> token.claim("sub", "google-sub-123").claim("email", "a@b.com"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("a@b.com"));
	}
}
