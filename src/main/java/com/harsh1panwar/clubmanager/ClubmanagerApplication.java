package com.harsh1panwar.clubmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class ClubmanagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClubmanagerApplication.class, args);
	}
}