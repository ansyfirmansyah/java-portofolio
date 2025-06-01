package com.ansyporto.auth.service;

import com.ansyporto.auth.dto.RegisterRequest;
import com.ansyporto.auth.entity.Role;
import com.ansyporto.auth.entity.User;
import com.ansyporto.auth.entity.UserActivityAudit;
import com.ansyporto.auth.exception.RateLimitException;
import com.ansyporto.auth.repository.UserActivityAuditRepository;
import com.ansyporto.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserActivityAuditRepository auditRepository;

    private static final List<String> ALLOWED_DOMAINS = List.of("gmail.com", "outlook.com", "microsoft.com", "yahoo.com");

    public void register(RegisterRequest request, HttpServletRequest http) {
        String email = request.getEmail();
        String domain = email.substring(email.indexOf("@") + 1);
        String ip = http.getRemoteAddr();
        boolean success = false;
        UUID userId = null;

        try {
            Instant limit = Instant.now().minus(60, ChronoUnit.MINUTES);
            if (auditRepository.countByIpAddressAndActivityTypeAndActivityTimeAfter(ip, "REGISTER", limit) > 10) {
                throw new RateLimitException("Terlalu banyak percobaan registrasi dari IP ini / Too many registration attempts from this IP");
            }

            if (!ALLOWED_DOMAINS.contains(domain)) {
                throw new IllegalArgumentException("Email domain tidak diperbolehkan / not allowed");
            }

            if (userRepository.existsByEmail(email)) {
                throw new IllegalStateException("Email sudah terdaftar / already registered");
            }

            String salt = BCrypt.gensalt();
            String hashedPassword = BCrypt.hashpw(request.getPassword(), salt);

            User user = User.builder()
                    .email(email)
                    .password(hashedPassword)
                    .role(Role.USER)
                    .emailVerified(false)
                    .build();

            user = userRepository.save(user);
            userId = user.getId();
            success = true;

            // TODO: Send email verification
        } finally {
            auditRepository.save(UserActivityAudit.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .email(email)
                    .activityType("REGISTER")
                    .success(success)
                    .ipAddress(http.getRemoteAddr())
                    .userAgent(http.getHeader("User-Agent"))
                    .build());
        }
    }
}
