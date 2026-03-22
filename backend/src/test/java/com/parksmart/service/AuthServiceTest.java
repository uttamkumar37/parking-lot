package com.parksmart.service;

import com.parksmart.dto.request.RegisterRequest;
import com.parksmart.dto.response.AuthResponse;
import com.parksmart.entity.User;
import com.parksmart.exception.ConflictException;
import com.parksmart.repository.UserRepository;
import com.parksmart.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Alice Smith");
        registerRequest.setEmail("alice@example.com");
        registerRequest.setPassword("Password@1");
        registerRequest.setPhone("+14155551234");

        savedUser = User.builder()
            .id(UUID.randomUUID())
            .name("Alice Smith")
            .email("alice@example.com")
            .password("encoded-password")
            .role(User.Role.USER)
            .build();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_withNewEmail_returnsAuthResponse() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@1")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(any(), anyMap())).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getRole()).isEqualTo(User.Role.USER);
        verify(passwordEncoder).encode("Password@1");
        verify(userRepository).save(argThat(u -> 
            u.getEmail().equals("alice@example.com") &&
            u.getRole() == User.Role.USER));
    }

    @Test
    void register_withDuplicateEmail_throwsConflictException() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordIsEncoded() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("Password@1")).thenReturn("hashed-pw");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(jwtUtil.generateToken(any(), anyMap())).thenReturn("tok");

        authService.register(registerRequest);

        verify(userRepository).save(argThat(u -> !u.getPassword().equals("Password@1")));
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_withValidCredentials_returnsAuthResponse() {
        var loginReq = new com.parksmart.dto.request.LoginRequest();
        loginReq.setEmail("alice@example.com");
        loginReq.setPassword("Password@1");

        when(userRepository.findByEmail("alice@example.com"))
            .thenReturn(Optional.of(savedUser));
        when(jwtUtil.generateToken(any(), anyMap())).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginReq);

        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(
            new UsernamePasswordAuthenticationToken("alice@example.com", "Password@1")
        );
    }

    @Test
    void login_withBadCredentials_throwsBadCredentialsException() {
        var loginReq = new com.parksmart.dto.request.LoginRequest();
        loginReq.setEmail("alice@example.com");
        loginReq.setPassword("wrong");

        doThrow(new BadCredentialsException("Bad credentials"))
            .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(loginReq))
            .isInstanceOf(BadCredentialsException.class);
    }
}
