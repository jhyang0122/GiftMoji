package com.giftmoji.giftmoji.controller;

import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.repository.UserRepository;
import com.giftmoji.giftmoji.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MerchantRedeemControllerTest {

	private static final String LATTE_ITEM_ID = "b0000000-0000-0000-0000-000000000001";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private VoucherRepository voucherRepository;

	private String createVoucherCode(String suffix) {
		User owner = userRepository.save(User.createFromGoogle(UUID.randomUUID().toString(), "owner" + suffix + "@example.com", "Name", null));
		Voucher voucher = Voucher.purchase("redeem-controller-" + suffix, UUID.fromString(LATTE_ITEM_ID), owner.getId(), LocalDateTime.now().plusDays(1));
		voucherRepository.save(voucher);
		return voucher.getCode();
	}

	@Test
	void redeem_forbiddenForRegularUser() throws Exception {
		String code = createVoucherCode("1");
		mockMvc.perform(post("/api/merchant/redeem")
						.with(oidcLogin())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"code\":\"" + code + "\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void redeem_succeedsForMerchantRole() throws Exception {
		String code = createVoucherCode("2");
		userRepository.save(User.createFromGoogle("merchant-staff-sub", "staff@example.com", "Staff", null));

		mockMvc.perform(post("/api/merchant/redeem")
						.with(oidcLogin()
								.idToken(token -> token.claim("sub", "merchant-staff-sub"))
								.userInfoToken(token -> token.claim("sub", "merchant-staff-sub").claim("email", "staff@example.com"))
								.authorities(
										new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"),
										new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MERCHANT")))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"code\":\"" + code + "\"}"))
				.andExpect(status().isOk());
	}
}
