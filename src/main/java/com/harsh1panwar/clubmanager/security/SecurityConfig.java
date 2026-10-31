package com.harsh1panwar.clubmanager.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Public: auth endpoints, static pages, Spring's error page
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()

                    // Organizer-only actions
                    .requestMatchers(HttpMethod.POST, "/api/clubs").hasRole("ORGANIZER")
                    .requestMatchers("/api/clubs/mine").hasRole("ORGANIZER")
                    .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ORGANIZER")
                    .requestMatchers("/api/events/mine").hasRole("ORGANIZER")
                    .requestMatchers("/api/events/*/live-qr", "/api/events/*/live-qr-image").hasRole("ORGANIZER")
                    .requestMatchers("/api/events/*/attendees").hasRole("ORGANIZER")

                    // Attendee-only actions
                    .requestMatchers("/api/events/*/register", "/api/events/*/check-in").hasRole("ATTENDEE")
                    .requestMatchers("/api/events/*/certificate", "/api/registrations/**").hasRole("ATTENDEE")

                    .anyRequest().authenticated()
            )
            // Missing/invalid token -> 401 (role mismatch stays 403)
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
