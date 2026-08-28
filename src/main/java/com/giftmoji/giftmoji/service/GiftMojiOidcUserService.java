package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class GiftMojiOidcUserService extends OidcUserService {

	private static final Logger log = LoggerFactory.getLogger(GiftMojiOidcUserService.class);
	private static final int SUBJECT_LOG_PREFIX_LENGTH = 8;

	private final UserRepository userRepository;
	private final Set<String> merchantEmails;

	public GiftMojiOidcUserService(
			UserRepository userRepository,
			@Value("${giftmoji.merchant-emails:}") String merchantEmailsProperty) {
		this.userRepository = userRepository;
		this.merchantEmails = parseAllowlist(merchantEmailsProperty);
	}

	@Override
	@Transactional
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser oidcUser = super.loadUser(userRequest);

		String googleId = oidcUser.getSubject();
		String email = oidcUser.getEmail();
		if (email == null || email.isBlank()) {
			// email is required (app_user.email is NOT NULL, and receivers are
			// looked up by it) — fail the login cleanly rather than NPE later.
			throw new OAuth2AuthenticationException("Google account has no email");
		}
		String displayName = oidcUser.getFullName();
		String pictureUrl = oidcUser.getPicture();
		boolean isMerchant = merchantEmails.contains(email.toLowerCase());

		User user;
		Optional<User> existing = userRepository.findByGoogleId(googleId);
		if (existing.isPresent()) {
			user = existing.get();
			user.recordLogin(email, displayName, pictureUrl);
			log.debug("Updated last login for user {}", maskSubject(googleId));
		} else {
			user = User.createFromGoogle(googleId, email, displayName, pictureUrl);
			log.info("Created new user for Google subject {}", maskSubject(googleId));
		}
		// Applied on every login so a merchant-emails config change takes
		// effect on redeploy without a manual DB fix.
		user.syncMerchantRole(isMerchant);
		userRepository.save(user);

		return new DefaultOidcUser(authoritiesFor(isMerchant), oidcUser.getIdToken(), oidcUser.getUserInfo());
	}

	private List<GrantedAuthority> authoritiesFor(boolean isMerchant) {
		if (isMerchant) {
			return List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_MERCHANT"));
		}
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	private Set<String> parseAllowlist(String property) {
		if (property == null || property.isBlank()) {
			return Set.of();
		}
		Set<String> emails = new HashSet<>();
		for (String email : property.split(",")) {
			String trimmed = email.trim().toLowerCase();
			if (!trimmed.isEmpty()) {
				emails.add(trimmed);
			}
		}
		return emails;
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
