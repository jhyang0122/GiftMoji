package com.giftmoji.giftmoji.controller;

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
class MerchantControllerTest {

	// Seeded via V4__seed_merchants_and_items.sql
	private static final String COFFEE_MERCHANT_ID = "a0000000-0000-0000-0000-000000000001";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void listMerchants_unauthenticated_returns401() throws Exception {
		mockMvc.perform(get("/api/merchants")).andExpect(status().isUnauthorized());
	}

	@Test
	void listMerchants_authenticated_returnsSeededData() throws Exception {
		mockMvc.perform(get("/api/merchants").with(oidcLogin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.name == 'Brew & Bean Coffee')]").exists());
	}

	@Test
	void listItems_authenticated_returnsSeededItemsForMerchant() throws Exception {
		mockMvc.perform(get("/api/merchants/" + COFFEE_MERCHANT_ID + "/items").with(oidcLogin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.name == 'Signature Latte')]").exists());
	}
}
