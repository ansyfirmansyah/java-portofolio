package com.ansyporto.auth.controller;

import com.ansyporto.auth.dto.ApiResponse;
import com.ansyporto.auth.dto.RegisterRequest;
import com.ansyporto.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        authService.register(request, http);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Pendaftaran berhasil. Silakan cek email Anda untuk verifikasi.",
                        "Registration successful. Please check your email to verify."
                )
        );
    }
}

