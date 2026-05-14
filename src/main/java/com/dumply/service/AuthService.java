package com.dumply.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.dumply.common.dto.*;
import com.dumply.common.exception.AccountBlockedException;
import com.dumply.common.exception.BusinessException;
import com.dumply.config.security.TokenService;
import com.dumply.config.tenant.TenantContext;
import com.dumply.model.User;
import com.dumply.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();
    private final EmailService emailService;
    private final TokenBlacklistService blacklistService;
    private final ConsentService consentService;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       EmailService emailService,
                       TokenBlacklistService blacklistService,
                       ConsentService consentService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.blacklistService = blacklistService;
        this.consentService = consentService;
        this.auditLogService = auditLogService;
    }

    // ============================================================
    //  LOGIN — Req. 5.1
    // ============================================================

    public ResponseDTO login(LoginRequestDTO body, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(body.email()).orElse(null);

        if (user == null) {
            auditLogService.recordFailure(AuditEventType.LOGIN_FAIL,
                    null, null, body.email(), httpRequest, "user_not_found");
            throw new BadCredentialsException("Credenciais Inválidas");
        }

        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        if (user.getLocktime() != null && user.getLocktime().isAfter(LocalDateTime.now())) {
            auditLogService.recordFailure(AuditEventType.ACCOUNT_LOCKED,
                    user.getId(), companyId, user.getEmail(), httpRequest,
                    "lock_until=" + user.getLocktime());
            throw new AccountBlockedException("Conta bloqueada por tentativas de login inválidas");
        }

        if (!passwordEncoder.matches(body.password(), user.getPassword())) {
            processFailedLogin(user);
            auditLogService.recordFailure(AuditEventType.LOGIN_FAIL,
                    user.getId(), companyId, user.getEmail(), httpRequest,
                    "invalid_password attempts=" + user.getFailedLoginAttempts());
            throw new BadCredentialsException("Credenciais Inválidas");
        }

        resetFailedLogin(user);

        // 2FA habilitado: não emite o token ainda, apenas sinaliza ao frontend.
        if (user.is2faEnabled()) {
            auditLogService.recordSuccess(AuditEventType.LOGIN_SUCCESS,
                    user.getId(), companyId, user.getEmail(), httpRequest,
                    "awaiting_2fa");
            return new ResponseDTO(null, true, user.getEmail());
        }

        auditLogService.recordSuccess(AuditEventType.LOGIN_SUCCESS,
                user.getId(), companyId, user.getEmail(), httpRequest, null);
        return new ResponseDTO(tokenService.generateToken(user));
    }

    private void processFailedLogin(User user) {
        int MAX_FAILED_ATTEMPTS = 5;
        int LOCK_TIME_MINUTES = 30;

        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setLocktime(LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES));
            user.setAccountNonLocked(false);
        }

        userRepository.save(user);
    }

    private void resetFailedLogin(User user) {
        if (user.getFailedLoginAttempts() > 0 || user.getLocktime() != null) {
            user.setFailedLoginAttempts(0);
            user.setLocktime(null);
            user.setAccountNonLocked(true);
            userRepository.save(user);
        }
    }

    // ============================================================
    //  2FA — Req. 5.2
    // ============================================================

    @Transactional
    public Map<String, String> setup2FA() {
        User user = getAuthenticatedUser();

        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        user.setSecret2fa(key.getKey());
        userRepository.save(user);

        String issuer = "Dumply";
        String account = user.getEmail();
        String qrCodeUrl = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, account, key.getKey(), issuer);
        return Map.of("qrCodeUrl", qrCodeUrl);
    }

    @Transactional
    public void confirmEnable2FA(int code, HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser();
        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        if (gAuth.authorize(user.getSecret2fa(), code)) {
            user.set2faEnabled(true);
            userRepository.save(user);
            auditLogService.recordSuccess(AuditEventType.TWO_FA_ENABLED,
                    user.getId(), companyId, user.getEmail(), httpRequest, null);
        } else {
            auditLogService.recordFailure(AuditEventType.TWO_FA_FAIL,
                    user.getId(), companyId, user.getEmail(), httpRequest,
                    "context=enable_confirmation");
            throw new RuntimeException("Código 2FA inválido");
        }
    }

    public ResponseDTO verify2FA(String email, int code, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));

        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        if (gAuth.authorize(user.getSecret2fa(), code)) {
            auditLogService.recordSuccess(AuditEventType.TWO_FA_SUCCESS,
                    user.getId(), companyId, user.getEmail(), httpRequest, null);
            return new ResponseDTO(tokenService.generateToken(user));
        } else {
            auditLogService.recordFailure(AuditEventType.TWO_FA_FAIL,
                    user.getId(), companyId, user.getEmail(), httpRequest,
                    "context=login_verification");
            throw new BadCredentialsException("Código 2FA inválido");
        }
    }

    @Transactional
    public void requestDisable2FACode(HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser();
        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        String code = String.valueOf((int) ((Math.random() * (999999 - 100000)) + 100000));
        user.setDisable2faCode(code);
        userRepository.save(user);

        emailService.sendDisable2FACode(user.getEmail(), code);

        auditLogService.recordSuccess(AuditEventType.TWO_FA_DISABLE_REQUESTED,
                user.getId(), companyId, user.getEmail(), httpRequest, null);
    }

    @Transactional
    public void confirmDisable2FA(String code, HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser();
        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        if (user.getDisable2faCode() != null && user.getDisable2faCode().equals(code)) {
            user.set2faEnabled(false);
            user.setSecret2fa(null);
            user.setDisable2faCode(null);
            userRepository.save(user);

            auditLogService.recordSuccess(AuditEventType.TWO_FA_DISABLED,
                    user.getId(), companyId, user.getEmail(), httpRequest, null);
        } else {
            auditLogService.recordFailure(AuditEventType.TWO_FA_FAIL,
                    user.getId(), companyId, user.getEmail(), httpRequest,
                    "context=disable_confirmation");
            throw new RuntimeException("Código de verificação inválido");
        }
    }

    // ============================================================
    //  LOGOUT — Req. 5.1
    // ============================================================

    public void logout(HttpServletRequest httpRequest) {
        String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        if (token != null) {
            DecodedJWT decodedJWT = tokenService.validateToken(token);
            long expiration = decodedJWT.getExpiresAt().getTime() - System.currentTimeMillis();
            blacklistService.blacklistToken(token, expiration);
        }

        try {
            User user = getAuthenticatedUser();
            UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;
            auditLogService.recordSuccess(AuditEventType.LOGOUT,
                    user.getId(), companyId, user.getEmail(), httpRequest, null);
        } catch (Exception ignored) {
            // Logout sem usuário identificável — registra ainda assim, com dados parciais
            auditLogService.recordSuccess(AuditEventType.LOGOUT,
                    null, null, null, httpRequest, "user_lookup_failed");
        }
    }

    // ============================================================
    //  RECUPERAÇÃO E ALTERAÇÃO DE SENHA — Reqs. 2.6 e 2.7
    // ============================================================

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        logger.info("Solicitação de recuperação de senha para o e-mail: {}", request.email());

        userRepository.findByEmail(request.email()).ifPresentOrElse(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), token);

            UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;
            auditLogService.recordSuccess(AuditEventType.PASSWORD_RESET_REQUEST,
                    user.getId(), companyId, user.getEmail(), httpRequest, null);
        }, () -> {
            // E-mail inexistente: registra como falha para detectar enumeração
            auditLogService.recordFailure(AuditEventType.PASSWORD_RESET_REQUEST,
                    null, null, request.email(), httpRequest, "user_not_found");
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByPasswordResetToken(request.token()).orElse(null);

        if (user == null) {
            logger.error("Falha no reset de senha: Token inválido");
            auditLogService.recordFailure(AuditEventType.PASSWORD_RESET_FAIL,
                    null, null, null, httpRequest, "invalid_token");
            throw new BusinessException("Token de recuperação inválido.");
        }

        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        if (user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            logger.error("Falha no reset de senha: Token expirado para o usuário: {}", user.getEmail());
            auditLogService.recordFailure(AuditEventType.PASSWORD_RESET_FAIL,
                    user.getId(), companyId, user.getEmail(), httpRequest, "expired_token");
            throw new BusinessException("Token de recuperação expirado.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
        logger.info("Senha do usuário {} resetada com sucesso.", user.getEmail());

        auditLogService.recordSuccess(AuditEventType.PASSWORD_RESET_SUCCESS,
                user.getId(), companyId, user.getEmail(), httpRequest, null);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser();
        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            logger.error("Falha ao alterar senha: Senha antiga incorreta para o usuário {}", user.getEmail());
            auditLogService.recordFailure(AuditEventType.PASSWORD_CHANGED,
                    user.getId(), companyId, user.getEmail(), httpRequest, "wrong_current_password");
            throw new BusinessException("A senha atual informada está incorreta.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        logger.info("Senha alterada com sucesso para o usuário {}", user.getEmail());

        auditLogService.recordSuccess(AuditEventType.PASSWORD_CHANGED,
                user.getId(), companyId, user.getEmail(), httpRequest, null);
    }

    // ============================================================
    //  CONTEXTO DO USUÁRIO AUTENTICADO
    // ============================================================

    public User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();
        UUID companyId = TenantContext.getCompanyId();

        return userRepository.findByEmailAndCompanyId(email, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    public ProfileDTO getLoggedUser() {
        User user = getAuthenticatedUser();

        return new ProfileDTO(
                user.getFullName(),
                user.getRole(),
                user.isFirstLogin(),
                user.is2faEnabled()
        );
    }

    // ============================================================
    //  PRIMEIRO LOGIN COM ACEITE LGPD — Req. 4.4
    // ============================================================

    @Transactional
    public ProfileDTO completeWelcome(CompleteWelcomeRequest request,
                                      HttpServletRequest httpRequest) {
        if (request == null
                || request.consentGiven() == null
                || !request.consentGiven()) {
            throw new BusinessException(
                    "Aceite dos termos da Política de Privacidade é obrigatório " +
                            "para concluir o cadastro."
            );
        }

        User user = getAuthenticatedUser();
        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        consentService.recordInitialConsent(user, request.consentVersion(), httpRequest);

        user.setFirstLogin(false);
        userRepository.save(user);

        // Audita o aceite inicial (também coberto na consent_log, mas redundância aqui
        // é desejável para investigações no painel /admin/audit/logs).
        auditLogService.recordSuccess(AuditEventType.CONSENT_GRANTED,
                user.getId(), companyId, user.getEmail(), httpRequest,
                "purpose=ALL_MANDATORY version=" + request.consentVersion());

        logger.info("Primeiro login concluído com aceite — usuário: {} | versão: {}",
                user.getEmail(), request.consentVersion());

        return new ProfileDTO(
                user.getFullName(),
                user.getRole(),
                user.isFirstLogin(),
                user.is2faEnabled()
        );
    }

    public ResponseDTO register(RegisterRequestDTO body) {
        if (userRepository.existsByEmailGlobal(body.email())) {
            throw new BusinessException("Usuário já existe");
        }

        User user = new User();
        user.setEmail(body.email());
        user.setPassword(passwordEncoder.encode(body.password()));
        user.setDocument(body.document());
        user.setFullName(body.fullName());
        user.setRole(body.role());

        userRepository.save(user);

        return new ResponseDTO(tokenService.generateToken(user));
    }
}
