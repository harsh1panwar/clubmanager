package com.harsh1panwar.clubmanager.service;

import com.harsh1panwar.clubmanager.dto.AuthRequest;
import com.harsh1panwar.clubmanager.dto.AuthResponse;
import com.harsh1panwar.clubmanager.entity.Role;
import com.harsh1panwar.clubmanager.entity.User;
import com.harsh1panwar.clubmanager.repository.UserRepository;
import com.harsh1panwar.clubmanager.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(AuthRequest request) {

        // Email already exist karta hai?
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Role set karo — default ATTENDEE agar kuch nahi aaya
        Role role = Role.ATTENDEE;
        if (request.getRole() != null &&
                request.getRole().equalsIgnoreCase("ORGANIZER")) {
            role = Role.ORGANIZER;
        }

        // User banao — password hash karke save karo
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);

        // JWT token generate karo aur return karo
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getRole().name(), user.getName());
    }

    public AuthResponse login(AuthRequest request) {

        // User dhundho email se
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Password match karo
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // Token generate karo
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getRole().name(), user.getName());
    }
}