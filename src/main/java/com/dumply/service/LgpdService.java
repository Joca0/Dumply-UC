package com.dumply.service;

import com.dumply.common.dto.*;
import com.dumply.common.exception.BusinessException;
import com.dumply.model.Company;
import com.dumply.model.ConsentLog;
import com.dumply.model.User;
import com.dumply.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service responsável pelos direitos do titular previstos no Art. 18 da LGPD,
 * em atendimento aos requisitos 4.8 (consulta), 4.9 (portabilidade) e
 * 4.10 (eliminação) da disciplina.
 *
 * <p>Trabalha em conjunto com o {@link ConsentService} para compor as
 * respostas que incluem o estado e o histórico de consentimentos, e com o
 * {@link AuditLogService} para registrar cada exercício de direito do titular
 * na trilha de auditoria (Req. 5.1 a 5.4).</p>
 *
 * <h3>Estratégia de exclusão (Req. 4.10)</h3>
 * <p>Adota-se a abordagem de <b>anonimização estruturada</b>: em vez de remover
 * fisicamente o registro do {@link User} (o que comprometeria a integridade
 * referencial em arquitetura multi-tenant), substituem-se os atributos
 * identificáveis por valores anonimizados e revogam-se todas as credenciais.
 * O vínculo {@code company_id} é preservado para manter a integridade contábil
 * e operacional do tenant; a {@code consent_log} é mantida por 5 anos para
 * fins de prestação de contas (Art. 16, II da LGPD).</p>
 */
@Service
public class LgpdService {

    private static final Logger logger = LoggerFactory.getLogger(LgpdService.class);

    /**
     * Versão do schema do pacote de exportação ({@code GET /lgpd/me/export}).
     * Alterações de estrutura devem incrementar esta constante.
     */
    public static final String EXPORT_SCHEMA_VERSION = "1.0";

    private final UserRepository userRepository;
    private final ConsentService consentService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public LgpdService(UserRepository userRepository,
                       ConsentService consentService,
                       TokenBlacklistService tokenBlacklistService,
                       PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.consentService = consentService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    // ============================================================
    //  Req. 4.8 — Consulta aos dados do titular
    // ============================================================

    /**
     * Retorna a representação completa dos dados pessoais do titular,
     * com o {@code document} mascarado, em atendimento ao Art. 18, II
     * da LGPD e ao Req. 4.8.
     */
    public LgpdDataDTO getMyData(User user, HttpServletRequest httpRequest) {
        ConsentStatusDTO consentStatus = consentService.getStatus(user);
        Company company = user.getCompany();
        UUID companyId = company != null ? company.getId() : null;

        LgpdDataDTO.CompanyContextDTO companyContext = company == null ? null :
                new LgpdDataDTO.CompanyContextDTO(
                        company.getId(),
                        company.getName(),
                        company.getSlug(),
                        company.getStatus() != null ? company.getStatus().name() : null,
                        company.getCreatedAt()
                );

        auditLogService.recordSuccess(AuditEventType.DATA_ACCESSED,
                user.getId(), companyId, user.getEmail(), httpRequest,
                "endpoint=/lgpd/me/data");

        return new LgpdDataDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                maskDocument(user.getDocument()),
                user.getRole(),
                user.is2faEnabled(),
                user.isFirstLogin(),
                consentStatus,
                companyContext
        );
    }

    // ============================================================
    //  Req. 4.9 — Exportação de dados (portabilidade)
    // ============================================================

    /**
     * Gera o pacote completo de exportação do titular em formato JSON
     * interoperável, em atendimento ao Art. 18, V da LGPD e ao Req. 4.9.
     */
    public LgpdExportDTO exportMyData(User user, HttpServletRequest httpRequest) {
        Company company = user.getCompany();
        UUID companyId = company != null ? company.getId() : null;

        LgpdExportDTO.CompanyDTO companyDto = company == null ? null :
                new LgpdExportDTO.CompanyDTO(
                        company.getId(),
                        company.getName(),
                        company.getSlug(),
                        company.getStatus() != null ? company.getStatus().name() : null,
                        company.getCreatedAt()
                );

        LgpdExportDTO.SubjectDTO subject = new LgpdExportDTO.SubjectDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getDocument(), // em exportação, o documento vai em texto pleno
                user.getRole(),
                user.is2faEnabled()
        );

        List<ConsentLog> history = consentService.getFullHistory(user.getId());
        List<LgpdExportDTO.ConsentEventDTO> consentHistory = history.stream()
                .map(log -> new LgpdExportDTO.ConsentEventDTO(
                        log.getId(),
                        log.getAction(),
                        log.getPurpose(),
                        log.getVersion(),
                        log.getIpAddress(),
                        log.getUserAgent(),
                        log.getTimestamp()
                ))
                .toList();

        LgpdExportDTO.SecuritySummaryDTO security = new LgpdExportDTO.SecuritySummaryDTO(
                user.getFailedLoginAttempts(),
                user.getLocktime(),
                user.isAccountNonLocked(),
                user.is2faEnabled()
        );

        auditLogService.recordSuccess(AuditEventType.DATA_EXPORTED,
                user.getId(), companyId, user.getEmail(), httpRequest,
                "endpoint=/lgpd/me/export schema=" + EXPORT_SCHEMA_VERSION);

        logger.info("Exportação de dados LGPD gerada para o titular {}", user.getEmail());

        return new LgpdExportDTO(
                EXPORT_SCHEMA_VERSION,
                LocalDateTime.now(),
                subject,
                companyDto,
                consentHistory,
                security
        );
    }

    // ============================================================
    //  Req. 4.10 — Exclusão (anonimização estruturada)
    // ============================================================

    /**
     * Realiza a anonimização estruturada da conta do titular, em atendimento
     * ao Art. 18, VI da LGPD e ao Req. 4.10.
     *
     * <p>Procedimento aplicado:
     * <ol>
     *   <li>Confirmação dupla (senha + string {@code "EXCLUIR"});</li>
     *   <li>Substituição de {@code email}, {@code fullName} e {@code document}
     *       por valores anonimizados;</li>
     *   <li>Limpeza de credenciais ({@code password}, {@code secret2fa},
     *       {@code passwordResetToken}, {@code disable2faCode});</li>
     *   <li>Desativação do 2FA e bloqueio da conta;</li>
     *   <li>Revogação imediata do JWT corrente via {@link TokenBlacklistService};</li>
     *   <li>Registro do evento na trilha de auditoria ({@code audit_log});</li>
     *   <li>Preservação do vínculo {@code company_id} e do histórico de
     *       {@code consent_log} (prestação de contas — Art. 16, II).</li>
     * </ol></p>
     *
     * @param user             titular autenticado
     * @param request          payload com senha e string de confirmação
     * @param currentToken     token JWT corrente (a ser blacklistado); pode ser {@code null}
     * @param tokenRemainingMs tempo restante do token, em milissegundos
     * @param httpRequest      requisição HTTP corrente, usada para auditoria
     */
    @Transactional
    public void deleteMyAccount(User user, AccountDeletionRequest request,
                                String currentToken, long tokenRemainingMs,
                                HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException("Payload de exclusão é obrigatório.");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("Senha atual é obrigatória para confirmar a exclusão.");
        }
        if (!AccountDeletionRequest.EXPECTED_CONFIRMATION.equals(request.confirmation())) {
            throw new BusinessException(
                    "Confirmação inválida. Para excluir sua conta, informe a palavra '"
                            + AccountDeletionRequest.EXPECTED_CONFIRMATION + "'."
            );
        }

        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            auditLogService.recordFailure(AuditEventType.ACCOUNT_DELETED,
                    user.getId(), companyId, user.getEmail(), httpRequest,
                    "wrong_password");
            logger.warn("Tentativa de exclusão com senha incorreta — usuário {}", user.getEmail());
            throw new BusinessException("Senha atual incorreta.");
        }

        String originalEmail = user.getEmail();
        String anonymousId = UUID.randomUUID().toString();

        // 1) Anonimização dos campos identificáveis
        user.setEmail("anonimizado_" + anonymousId + "@deletado.local");
        user.setFullName("Usuário Anonimizado");
        user.setDocument(null);

        // 2) Limpeza de credenciais e estados de segurança
        user.setPassword(null);
        user.setSecret2fa(null);
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        user.setDisable2faCode(null);
        user.set2faEnabled(false);
        user.setAccountNonLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLocktime(null);
        user.setFirstLogin(false);

        // 3) Mantemos consentGiven/consentVersion como evidência histórica.
        //    A trilha completa permanece em consent_log.

        userRepository.save(user);

        // 4) Revoga imediatamente o token corrente
        if (currentToken != null && !currentToken.isBlank() && tokenRemainingMs > 0) {
            tokenBlacklistService.blacklistToken(currentToken, tokenRemainingMs);
        }

        // 5) Auditoria — registra com o e-mail ORIGINAL (importante para investigações)
        auditLogService.recordSuccess(AuditEventType.ACCOUNT_DELETED,
                user.getId(), companyId, originalEmail, httpRequest,
                "anonymized_id=" + anonymousId);

        logger.info("Conta anonimizada com sucesso — usuário original: {} | identificador anônimo: {}",
                originalEmail, anonymousId);
    }

    // ============================================================
    //  Utilitários
    // ============================================================

    /**
     * Aplica mascaramento parcial em CPF ou CNPJ.
     *
     * <ul>
     *   <li>CPF (11 dígitos): {@code 12345678900} → {@code ***.456.789-**}</li>
     *   <li>CNPJ (14 dígitos): {@code 12345678000190} → {@code **.345.678/****-**}</li>
     *   <li>Documentos com tamanho desconhecido: oculta tudo exceto os 3 últimos.</li>
     * </ul>
     */
    public static String maskDocument(String document) {
        if (document == null || document.isBlank()) {
            return null;
        }
        String digits = document.replaceAll("\\D", "");

        if (digits.length() == 11) {
            // CPF — exibe apenas os blocos do meio
            return "***." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-**";
        }
        if (digits.length() == 14) {
            // CNPJ — exibe apenas o miolo
            return "**." + digits.substring(2, 5) + "." + digits.substring(5, 8) + "/****-**";
        }

        // Fallback: revela apenas os 3 últimos caracteres
        int visible = Math.min(3, digits.length());
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < digits.length() - visible; i++) {
            masked.append("*");
        }
        masked.append(digits.substring(digits.length() - visible));
        return masked.toString();
    }
}
