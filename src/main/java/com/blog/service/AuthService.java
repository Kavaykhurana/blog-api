package com.blog.service;

import com.blog.config.JwtTokenProvider;
import com.blog.dto.request.LoginRequest;
import com.blog.dto.request.RegisterRequest;
import com.blog.dto.response.AuthResponse;
import com.blog.exception.BadRequestException;
import com.blog.model.User;
import com.blog.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username is already taken!");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered!");
        }

        User user = new User(
            request.username(),
            request.email(),
            passwordEncoder.encode(request.password()),
            request.displayName() != null && !request.displayName().trim().isEmpty() ? request.displayName() : request.username(),
            request.bio(),
            "ROLE_USER"
        );

        User saved = userRepository.save(user);
        
        // Auto authenticate on register for seamless experience
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        return new AuthResponse(token, saved.getUsername(), saved.getEmail(), saved.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(request.usernameOrEmail())
            .or(() -> userRepository.findByEmail(request.usernameOrEmail()))
            .orElseThrow(() -> new BadRequestException("User not found"));

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getRole());
    }

    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
