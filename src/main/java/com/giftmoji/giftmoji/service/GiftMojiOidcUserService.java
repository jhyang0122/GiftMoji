package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GiftMojiOidcUserService extends OidcUserService {

	private static final Logger log = LoggerFactory.getLogger(GiftMojiOidcUserService.class);
	private static final int SUBJECT_LOG_PREFIX_LENGTH = 8;

	private final UserRepository userRepository;

	public GiftMojiOidcUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser oidcUser = super.loadUser(userRequest);

		String googleId = oidcUser.getSubject();
		String email = oidcUser.getEmail();
		String displayName = oidcUser.getFullName();
		String pictureUrl = oidcUser.getPicture();

		Optional<User> existing = userRepository.findByGoogleId(googleId);
		if (existing.isPresent()) {
			existing.get().recordLogin(email, displayName, pictureUrl);
			userRepository.save(existing.get());
			log.debug("Updated last login for user {}", maskSubject(googleId));
		} else {
			User created = User.createFromGoogle(googleId, email, displayName, pictureUrl);
			userRepository.save(created);
			log.info("Created new user for Google subject {}", maskSubject(googleId));
		}

		return oidcUser;
	}

	// The Google "sub" claim identifies a person; log only a short prefix,
	// never the full value or email, matching VoucherService.maskCode.
	private String maskSubject(String googleId) {
		if (googleId == null || googleId.length() <= SUBJECT_LOG_PREFIX_LENGTH) {
			return googleId;
		}
		return googleId.substring(0, SUBJECT_LOG_PREFIX_LENGTH) + "...";
	}
}
