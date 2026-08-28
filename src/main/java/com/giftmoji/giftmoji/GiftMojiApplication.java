package com.giftmoji.giftmoji;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GiftMojiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GiftMojiApplication.class, args);
	}

}
