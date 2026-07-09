package com.lakshmanan.bookmyevent.service;

import com.lakshmanan.bookmyevent.domain.Role;
import com.lakshmanan.bookmyevent.domain.User;
import com.lakshmanan.bookmyevent.dto.auth.AuthResponse;
import com.lakshmanan.bookmyevent.dto.auth.LoginRequest;
import com.lakshmanan.bookmyevent.dto.auth.RegisterRequest;
import com.lakshmanan.bookmyevent.exception.BadRequestException;
import com.lakshmanan.bookmyevent.repository.UserRepository;
import com.lakshmanan.bookmyevent.security.JwtService;
import com.lakshmanan.bookmyevent.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("An account with this email already exists");
        }
        User user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER
        );
        User saved = userRepository.save(user);

        UserPrincipal principal = UserPrincipal.from(saved);
        String token = jwtService.generateToken(principal);
        return AuthResponse.bearer(token, saved.getId(), saved.getName(), saved.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException (mapped to 401) when authentication fails.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        String token = jwtService.generateToken(principal);
        return AuthResponse.bearer(token, user.getId(), user.getName(), user.getRole().name());
    }
}
