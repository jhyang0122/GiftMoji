package com.giftmoji.giftmoji.config;

import com.giftmoji.giftmoji.service.GiftMojiOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final GiftMojiOidcUserService oidcUserService;

	public SecurityConfig(GiftMojiOidcUserService oidcUserService) {
		this.oidcUserService = oidcUserService;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			@Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) throws Exception {

		http.authorizeHttpRequests(auth -> {
			auth.requestMatchers(
					"/", "/index.html", "/css/**", "/js/**", "/icons/**",
					"/manifest.webmanifest", "/service-worker.js",
					"/api/status", "/api/voucher/**", "/api/auth/**",
					"/oauth2/**", "/login/**"
			).permitAll();
			if (h2ConsoleEnabled) {
				auth.requestMatchers("/h2-console/**").permitAll();
			}
			auth.anyRequest().authenticated();
		});

		http.csrf(csrf -> {
			csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
			// Voucher endpoints are anonymous and not user-linked yet (that
			// wiring is a separate follow-up), so keep app.js's existing
			// fetch() calls to them working unchanged.
			csrf.ignoringRequestMatchers("/api/voucher/**");
			if (h2ConsoleEnabled) {
				// H2 console's own login form doesn't send Spring's CSRF token.
				csrf.ignoringRequestMatchers("/h2-console/**");
			}
		});

		if (h2ConsoleEnabled) {
			// H2 console renders in a frame; only relax this locally.
			http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
		}

		http.oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
				// Single-page app shell with no protected deep link to return
				// to — always land back on the PWA shell.
				.defaultSuccessUrl("/", true)
		);

		http.logout(logout -> logout
				.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler((request, response, authentication) ->
						response.setStatus(HttpServletResponse.SC_NO_CONTENT))
		);

		return http.build();
	}
}
