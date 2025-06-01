package com.ansyporto.auth.service;

import com.ansyporto.auth.dto.RegisterRequest;
import com.ansyporto.auth.entity.Role;
import com.ansyporto.auth.entity.User;
import com.ansyporto.auth.exception.RateLimitException;
import com.ansyporto.auth.repository.UserRepository;
import com.ansyporto.auth.repository.UserActivityAuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserActivityAuditRepository auditRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerSuccess() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@gmail.com");
        registerRequest.setPassword("Password1!");

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(auditRepository.countByIpAddressAndActivityTypeAndActivityTimeAfter(any(), any(), any())).thenReturn(0L);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@gmail.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        assertDoesNotThrow(() -> authService.register(registerRequest, request));

        verify(userRepository).save(any(User.class));
        verify(auditRepository).save(any());
    }

    @Test
    void registerFail_EmailAlreadyExists() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@gmail.com");
        registerRequest.setPassword("Password1!");

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(true);
        when(auditRepository.countByIpAddressAndActivityTypeAndActivityTimeAfter(any(), any(), any())).thenReturn(0L);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                authService.register(registerRequest, request)
        );

        assertEquals("Email sudah terdaftar / already registered", thrown.getMessage());
        verify(auditRepository).save(any());
    }

    @Test
    void registerFail_RateLimitExceeded() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@gmail.com");
        registerRequest.setPassword("Password1!");

        when(auditRepository.countByIpAddressAndActivityTypeAndActivityTimeAfter(any(), any(), any())).thenReturn(99L);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        RateLimitException thrown = assertThrows(RateLimitException.class, () ->
                authService.register(registerRequest, request)
        );

        assertEquals("Terlalu banyak percobaan registrasi dari IP ini / Too many registration attempts from this IP", thrown.getMessage());
        verify(auditRepository).save(any());
    }
}
