package com.giftmoji.giftmoji.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	// Spring's static resource handler doesn't know the .webmanifest extension
	// by default and serves it as application/octet-stream; PWA installability
	// checks are happier with the proper manifest media type.
	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.mediaType("webmanifest", MediaType.valueOf("application/manifest+json"));
	}
}
