package com.giftmoji.giftmoji.config;

import com.giftmoji.giftmoji.service.GiftMojiOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
					"/api/status", "/api/auth/**",
					"/oauth2/**", "/login/**"
			).permitAll();
			if (h2ConsoleEnabled) {
				auth.requestMatchers("/h2-console/**").permitAll();
			}
			auth.requestMatchers("/api/merchant/**").hasRole("MERCHANT");
			auth.anyRequest().authenticated();
		});

		http.csrf(csrf -> {
			csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
			if (h2ConsoleEnabled) {
				// H2 console's own login form doesn't send Spring's CSRF token.
				csrf.ignoringRequestMatchers("/h2-console/**");
			}
		});

		if (h2ConsoleEnabled) {
			// H2 console renders in a frame; only relax this locally.
			http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
		}

		// The frontend is a fetch()-driven SPA, not server-rendered pages, so
		// an unauthenticated call to a protected /api/** endpoint should get
		// a plain 401 the client can branch on — not the default
		// redirect-to-Google-login response oauth2Login() otherwise sends
		// for any unauthenticated request.
		http.exceptionHandling(exceptionHandling -> exceptionHandling.defaultAuthenticationEntryPointFor(
				new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
				new AntPathRequestMatcher("/api/**")
		));

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
