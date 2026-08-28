package com.giftmoji.giftmoji.controller;

import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GiftControllerTest {

	private static final String LATTE_ITEM_ID = "b0000000-0000-0000-0000-000000000001";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UserRepository userRepository;

	@Test
	void receivedGifts_unauthenticated_returns401() throws Exception {
		mockMvc.perform(get("/api/gifts/received")).andExpect(status().isUnauthorized());
	}

	@Test
	void sendGift_endToEnd_debitsWalletAndAppearsInReceivedList() throws Exception {
		userRepository.save(User.createFromGoogle("sender-sub", "sender@example.com", "Sender", null));
		userRepository.save(User.createFromGoogle("receiver-sub", "receiver@example.com", "Receiver", null));

		String body = "{\"itemId\":\"" + LATTE_ITEM_ID + "\",\"receiverEmail\":\"receiver@example.com\",\"message\":\"Hi!\"}";

		String response = mockMvc.perform(post("/api/gifts")
						.with(oidcLogin()
								.idToken(token -> token.claim("sub", "sender-sub"))
								.userInfoToken(token -> token.claim("sub", "sender-sub").claim("email", "sender@example.com")))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String giftId = JsonPath.read(response, "$.giftId");
		assertThat(giftId).isNotBlank();

		User debitedSender = userRepository.findByGoogleId("sender-sub").orElseThrow();
		assertThat(debitedSender.getWalletBalance()).isEqualByComparingTo("44.50");

		mockMvc.perform(get("/api/gifts/received")
						.with(oidcLogin()
								.idToken(token -> token.claim("sub", "receiver-sub"))
								.userInfoToken(token -> token.claim("sub", "receiver-sub").claim("email", "receiver@example.com"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].itemName").value("Signature Latte"));
	}

	@Test
	void cancel_forbiddenForNonSender() throws Exception {
		userRepository.save(User.createFromGoogle("sender2-sub", "sender2@example.com", "Sender", null));
		userRepository.save(User.createFromGoogle("receiver2-sub", "receiver2@example.com", "Receiver", null));
		userRepository.save(User.createFromGoogle("stranger-sub", "stranger@example.com", "Stranger", null));

		String body = "{\"itemId\":\"" + LATTE_ITEM_ID + "\",\"receiverEmail\":\"receiver2@example.com\",\"message\":null}";
		String response = mockMvc.perform(post("/api/gifts")
						.with(oidcLogin()
								.idToken(token -> token.claim("sub", "sender2-sub"))
								.userInfoToken(token -> token.claim("sub", "sender2-sub").claim("email", "sender2@example.com")))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String giftId = JsonPath.read(response, "$.giftId");

		mockMvc.perform(post("/api/gifts/" + giftId + "/cancel")
						.with(oidcLogin()
								.idToken(token -> token.claim("sub", "stranger-sub"))
								.userInfoToken(token -> token.claim("sub", "stranger-sub").claim("email", "stranger@example.com")))
						.with(csrf()))
				.andExpect(status().isForbidden());
	}
}
