package com.dumply.controller;

import com.dumply.common.dto.AuditLogResponse;
import com.dumply.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;
import com.dumply.config.tenant.TenantContext;

/**
 * Endpoint administrativo para análise dos logs de auditoria de segurança,
 * em atendimento ao Req. 5.4 (exemplo de análise de logs apresentado).
 *
 * <p>Acessível apenas por usuários com papel {@code OWNER},
 * em alinhamento ao padrão de proteção observado em
 * {@link com.dumply.controller.UserController}.</p>
 *
 * <h3>Exemplos de uso</h3>
 * <pre>
 * # Todas as falhas de login das últimas 24h
 * GET /admin/audit/logs?eventType=LOGIN_FAIL&amp;since=2026-05-11T00:00:00
 *
 * # Atividade de um IP específico
 * GET /admin/audit/logs?ipAddress=203.0.113.10&amp;size=50
 *
 * # Eventos de um usuário ordenados por data
 * GET /admin/audit/logs?userId=9c7d...&amp;sort=timestamp,desc&amp;page=0&amp;size=20
 * </pre>
 */
@RestController
@RequestMapping("/admin/audit")
@PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
public class AuditController {

    private final AuditLogService auditLogService;

    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Consulta paginada de eventos de auditoria com filtros opcionais.
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<Page<AuditLogResponse>> listLogs(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime until,
            @PageableDefault(size = 25, sort = "timestamp",
                             direction = Sort.Direction.DESC) Pageable pageable) {

        UUID effectiveCompanyId = TenantContext.getCompanyId();

        // Se for passado um companyId via parâmetro, validar se é o mesmo do contexto
        if (companyId != null && !companyId.equals(effectiveCompanyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<AuditLogResponse> page = auditLogService.search(
                eventType, outcome, email, search, userId, effectiveCompanyId,
                ipAddress, since, until, pageable
        );
        return ResponseEntity.ok(page);
    }

    /**
     * Atalho de monitoramento: quantas tentativas falhas de login um e-mail
     * recebeu desde determinado instante. Útil para alertas em painéis.
     */
    @GetMapping("/logs/failed-logins/count")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<Long> countFailedLogins(
            @RequestParam String email,
            @RequestParam
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        UUID companyId = TenantContext.getCompanyId();
        return ResponseEntity.ok(auditLogService.countRecentFailedLogins(companyId, email, since));
    }
}
