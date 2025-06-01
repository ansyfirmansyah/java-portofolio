package com.ansyporto.auth.controller;

import com.ansyporto.auth.dto.ApiResponse;
import com.ansyporto.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void verifyEmail_success() throws Exception {
        when(authService.verifyEmail("valid-token")).thenReturn(true);

        mockMvc.perform(get("/auth/verify")
                        .param("token", "valid-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verifikasi berhasil"));
    }

    @Test
    void verifyEmail_failed() throws Exception {
        when(authService.verifyEmail("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/auth/verify")
                        .param("token", "invalid-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token tidak valid atau sudah kedaluwarsa"));
    }
}
