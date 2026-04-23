package com.dumply.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.dumply.common.dto.*;
import com.dumply.common.exception.AccountBlockedException;
import com.dumply.common.exception.BusinessException;
import com.dumply.config.security.TokenService;
import com.dumply.model.User;
import com.dumply.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dumply.config.tenant.TenantContext;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final EmailService emailService;
    private final TokenBlacklistService blacklistService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       EmailService emailService,
                       TokenBlacklistService blacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.blacklistService = blacklistService;
    }

    public ResponseDTO login(LoginRequestDTO body) {
        User user = userRepository.findByEmail(body.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais Inválidas"));

        if (user.getLocktime() != null && user.getLocktime().isAfter(LocalDateTime.now())) {
            throw new AccountBlockedException("Conta bloqueada por tentativas de login inválidas");
        }

        if (!passwordEncoder.matches(body.password(), user.getPassword())) {
            processFailedLogin(user);
            throw new BadCredentialsException("Credenciais Inválidas");
        }

        resetFailedLogin(user);

        //Se o 2fa estiver habilitado, não retornamos o token ainda
        if (user.is2faEnabled()) {
            return new ResponseDTO(null, true, user.getEmail());
        }

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

    //Método para gerar o QR Code do 2FA
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
    public void confirmEnable2FA(int code) {
        User user = getAuthenticatedUser();
        if (gAuth.authorize(user.getSecret2fa(), code)) {
            user.set2faEnabled(true);
            userRepository.save(user);
        } else {
            throw new RuntimeException("Código 2FA inválido"); //<-- Trocar para nova exception "InvalidActivate2FACode"
        }
    }

    public ResponseDTO verify2FA(String email, int code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));

        if (gAuth.authorize(user.getSecret2fa(), code)) {
            return new ResponseDTO(tokenService.generateToken(user));
        } else {
            throw new BadCredentialsException("Código 2FA inválido");
        }

    }

    public void logout() {
        String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        if (token != null) {
            DecodedJWT decodedJWT = tokenService.validateToken(token);
            long expiration = decodedJWT.getExpiresAt().getTime() - System.currentTimeMillis();
            blacklistService.blacklistToken(token, expiration);
        }
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        logger.info("Solicitação de recuperação de senha para o e-mail: {}", request.email());

        // Não enviamos e-mail para evitar enumeração de e-mails
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(1)); //Expira em 1 hora
            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.token())
                .orElseThrow(()-> {
                    logger.error("Falha no reset de senha: Token inválido");
                    return new BusinessException("Token de recuperação inválido.");
                });

        if (user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            logger.error("Falha no reset de senha: Token expirado para o usuário: {}", user.getEmail());
            throw new BusinessException("Token de recuperação expirado.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
        logger.info("Senha do usuário {} resetada com sucesso.", user.getEmail());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getAuthenticatedUser();

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            logger.error("Falha ao alterar senha: Senha antiga incorreta para o usuário {}", user.getEmail());
            throw new BusinessException("A senha atual informada está incorreta.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        logger.info("Senha alterada com sucesso para o usuário {}", user.getEmail());
    }

    @Transactional
    public void requestDisable2FACode() {
        User user = getAuthenticatedUser();
        String code = String.valueOf((int) ((Math.random() * (999999 - 100000)) + 100000)); //Geração do código
        user.setDisable2faCode(code);
        userRepository.save(user);

        emailService.sendDisable2FACode(user.getEmail(), code);
    }

    @Transactional
    public void confirmDisable2FA(String code) {
        User user = getAuthenticatedUser();

        if (user.getDisable2faCode() != null && user.getDisable2faCode().equals(code)) {
            user.set2faEnabled(false);
            user.setSecret2fa(null);
            user.setDisable2faCode(null);
            userRepository.save(user);
        } else {
            throw new RuntimeException("Código de verificação inválido"); //<-- Trocar para nova exception "InvalidDisable2FACode"
        }
    }

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

    @Transactional
    public ProfileDTO completeWelcome() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        user.setFirstLogin(false);
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