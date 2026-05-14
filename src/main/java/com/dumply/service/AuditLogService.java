package com.dumply.service;

import com.dumply.common.dto.AuditEventType;
import com.dumply.common.dto.AuditLogResponse;
import com.dumply.model.AuditLog;
import com.dumply.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service responsável pelo registro e consulta de eventos de auditoria
 * de segurança (Etapa 5 — Reqs. 5.1, 5.2, 5.3 e 5.4).
 *
 * <p>Todas as gravações são executadas de forma <b>assíncrona</b> ({@link Async})
 * para que jamais bloqueiem o fluxo principal de autenticação ou de exercício
 * de direitos LGPD, mesmo em cenários de indisponibilidade temporária do banco.
 * Esse padrão é especialmente importante para o {@link AuditEventType#LOGIN_FAIL},
 * que pode ser disparado em altíssima frequência durante um ataque de força
 * bruta — qualquer latência adicional aqui pioraria a resposta da aplicação.</p>
 *
 * <p>Eventos persistidos seguem o modelo da entidade {@link AuditLog}, criada
 * no Passo 1 e configurada como append-only no nível da aplicação
 * ({@link AuditLogRepository}).</p>
 *
 * <p><b>Nota:</b> para ativar a execução assíncrona, é necessário declarar
 * {@code @EnableAsync} em alguma classe de configuração (sugestão:
 * {@link com.dumply.DumplyApplication} ou uma nova {@code AsyncConfig}).</p>
 */
@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private static final String OUTCOME_SUCCESS = "SUCCESS";
    private static final String OUTCOME_FAILURE = "FAILURE";

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void recordSuccess(AuditEventType type, UUID userId, UUID companyId,
                              String email, HttpServletRequest request, String details) {
        persist(type, OUTCOME_SUCCESS, userId, companyId, email, request, details);
    }

    @Async
    public void recordFailure(AuditEventType type, UUID userId, UUID companyId,
                              String email, HttpServletRequest request, String details) {
        persist(type, OUTCOME_FAILURE, userId, companyId, email, request, details);
    }

    @Async
    public void record(AuditEventType type, String outcome, UUID userId, UUID companyId,
                       String email, HttpServletRequest request) {
        persist(type, outcome, userId, companyId, email, request, null);
    }

    private void persist(AuditEventType type, String outcome, UUID userId, UUID companyId,
                         String email, HttpServletRequest request, String details) {
        try {
            AuditLog event = new AuditLog(
                    userId,
                    companyId,
                    email,
                    type.name(),
                    outcome,
                    extractIp(request),
                    extractUserAgent(request),
                    details
            );
            auditLogRepository.save(event);
        } catch (Exception ex) {
            // Falha na gravação NUNCA deve quebrar o fluxo principal.
            logger.error("Falha ao gravar audit log [{} / {}] para o e-mail {}: {}",
                    type, outcome, email, ex.getMessage());
        }
    }

    public Page<AuditLogResponse> search(String eventType,
                                         String outcome,
                                         String email,
                                         String search,
                                         UUID userId,
                                         UUID companyId,
                                         String ipAddress,
                                         LocalDateTime since,
                                         LocalDateTime until,
                                         Pageable pageable) {

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (eventType != null && !eventType.isBlank()) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            if (outcome != null && !outcome.isBlank()) {
                predicates.add(cb.equal(root.get("outcome"), outcome));
            }
            if (email != null && !email.isBlank()) {
                predicates.add(cb.equal(root.get("email"), email));
            }
            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), searchPattern),
                        cb.like(cb.lower(root.get("details")), searchPattern)
                ));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (companyId != null) {
                predicates.add(cb.equal(root.get("companyId"), companyId));
            }
            if (ipAddress != null && !ipAddress.isBlank()) {
                predicates.add(cb.equal(root.get("ipAddress"), ipAddress));
            }
            if (since != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), since));
            }
            if (until != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), until));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public long countRecentFailedLogins(UUID companyId, String email, LocalDateTime since) {
        return auditLogRepository.countByCompanyIdAndEmailAndEventTypeAndTimestampAfter(
                companyId,
                email,
                AuditEventType.LOGIN_FAIL.name(),
                since
        );
    }


    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getCompanyId(),
                log.getEmail(),
                log.getEventType(),
                log.getOutcome(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getDetails(),
                log.getTimestamp()
        );
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        String ua = request.getHeader("User-Agent");
        if (ua == null) return null;
        return ua.length() > 500 ? ua.substring(0, 500) : ua;
    }
}
