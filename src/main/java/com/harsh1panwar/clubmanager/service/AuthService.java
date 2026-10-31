package com.harsh1panwar.clubmanager.service;

import com.harsh1panwar.clubmanager.dto.AuthRequest;
import com.harsh1panwar.clubmanager.dto.AuthResponse;
import com.harsh1panwar.clubmanager.dto.LoginRequest;
import com.harsh1panwar.clubmanager.entity.Role;
import com.harsh1panwar.clubmanager.entity.User;
import com.harsh1panwar.clubmanager.exception.ApiException;
import com.harsh1panwar.clubmanager.repository.UserRepository;
import com.harsh1panwar.clubmanager.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(AuthRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Email already registered");
        }

        // Default role is ATTENDEE unless ORGANIZER is explicitly requested
        Role role = "ORGANIZER".equalsIgnoreCase(request.getRole()) ? Role.ORGANIZER : Role.ATTENDEE;

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash, never plain text
                .role(role)
                .build();

        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user), user.getRole().name(), user.getName());
    }

    public AuthResponse login(LoginRequest request) {
        // Same message for unknown email and wrong password, so attackers can't probe which emails exist
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return new AuthResponse(jwtService.generateToken(user), user.getRole().name(), user.getName());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
