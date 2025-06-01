package com.ansyporto.auth.service;

import com.ansyporto.auth.dto.RegisterRequest;
import com.ansyporto.auth.entity.Role;
import com.ansyporto.auth.entity.User;
import com.ansyporto.auth.entity.UserActivityAudit;
import com.ansyporto.auth.entity.VerificationToken;
import com.ansyporto.auth.exception.RateLimitException;
import com.ansyporto.auth.repository.UserActivityAuditRepository;
import com.ansyporto.auth.repository.UserRepository;
import com.ansyporto.auth.repository.VerificationTokenRepository;
import com.ansyporto.auth.utils.EmailValidatorUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final UserActivityAuditRepository auditRepository;
    private final VerificationTokenRepository tokenRepository;
    private final MailService mailService;
    private final MessageSource messageSource;

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
                String message = messageSource.getMessage("registration.rateLimit", null, LocaleContextHolder.getLocale());
                throw new RateLimitException(message);
            }

            if (!EmailValidatorUtil.isEmailDomainValid(email)) {
                String message = messageSource.getMessage("registration.invalidDomain", null, LocaleContextHolder.getLocale());
                throw new IllegalArgumentException(message);
            }

            if (userRepository.existsByEmail(email)) {
                String message = messageSource.getMessage("registration.duplicateEmail", null, LocaleContextHolder.getLocale());
                throw new IllegalStateException(message);
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

            // Send email verification
            String token = UUID.randomUUID().toString();
            VerificationToken verificationToken = VerificationToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .used(false)
                    .build();

            tokenRepository.save(verificationToken);

            mailService.sendVerificationEmail(user.getEmail(), token);
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

    public boolean verifyEmail(String token) {
        Optional<VerificationToken> opt = tokenRepository.findByToken(token);
        if (opt.isEmpty()) return false;

        VerificationToken v = opt.get();
        if (v.isUsed() || v.getExpiresAt().isBefore(Instant.now())) return false;

        v.setUsed(true);
        v.getUser().setEmailVerified(true);

        tokenRepository.save(v);
        userRepository.save(v.getUser());
        return true;
    }
}
