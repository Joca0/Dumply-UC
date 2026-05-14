package com.dumply.service;

import com.dumply.common.dto.ConsentPurpose;
import com.dumply.common.dto.ConsentStatusDTO;
import com.dumply.common.exception.BusinessException;
import com.dumply.model.ConsentLog;
import com.dumply.model.User;
import com.dumply.repository.ConsentLogRepository;
import com.dumply.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsável pela gestão integral do ciclo de vida do consentimento
 * do titular dos dados pessoais.
 *
 * <p>Concentra a lógica relacionada aos seguintes requisitos da disciplina:
 * <ul>
 *   <li><b>Req. 4.4</b> — Registro explícito de consentimento</li>
 *   <li><b>Req. 4.5</b> — Consentimento associado à finalidade</li>
 *   <li><b>Req. 4.6</b> — Possibilidade de revogação do consentimento</li>
 *   <li><b>Req. 4.7</b> — Registro de data e versão do consentimento</li>
 * </ul></p>
 *
 * <p>Toda manifestação (concessão ou revogação) gera um registro imutável
 * na tabela {@code consent_log}, em conformidade com o princípio da prestação
 * de contas (Art. 6º, X da LGPD).</p>
 *
 * <p>A versão corrente da Política de Privacidade está definida na constante
 * {@link #CURRENT_POLICY_VERSION}. Alterações materiais nessa política devem
 * incrementar essa constante e exigir nova manifestação do titular.</p>
 */
@Service
public class ConsentService {

    private static final Logger logger = LoggerFactory.getLogger(ConsentService.class);

    /**
     * Versão vigente da Política de Privacidade. Alterações materiais devem
     * incrementar este valor — usuários cujo {@code consentVersion} difira
     * desta constante serão obrigados a reaceitar os termos no próximo login.
     */
    public static final String CURRENT_POLICY_VERSION = "v1.0";

    private final ConsentLogRepository consentLogRepository;
    private final UserRepository userRepository;

    public ConsentService(ConsentLogRepository consentLogRepository,
                          UserRepository userRepository) {
        this.consentLogRepository = consentLogRepository;
        this.userRepository = userRepository;
    }

    // ============================================================
    //  CONCESSÃO INICIAL — Primeiro login (Req. 4.4)
    // ============================================================

    /**
     * Registra a concessão inicial do consentimento no fluxo de primeiro login.
     *
     * <p>Atualiza os campos {@code consentGiven}, {@code consentGivenAt} e
     * {@code consentVersion} da entidade {@link User} e gera um evento
     * {@code GRANT} na {@code consent_log} para cada finalidade obrigatória.</p>
     *
     * @param user    usuário autenticado realizando o aceite
     * @param version versão da política aceita (deve coincidir com {@link #CURRENT_POLICY_VERSION})
     * @param request requisição HTTP corrente (usada para extrair IP e User-Agent)
     * @throws BusinessException se a versão informada não for a vigente
     */
    @Transactional
    public void recordInitialConsent(User user, String version, HttpServletRequest request) {
        if (version == null || version.isBlank()) {
            throw new BusinessException("Versão da política de privacidade é obrigatória.");
        }
        if (!CURRENT_POLICY_VERSION.equals(version)) {
            throw new BusinessException(
                    "Versão da política de privacidade desatualizada. Versão vigente: "
                            + CURRENT_POLICY_VERSION
            );
        }

        LocalDateTime now = LocalDateTime.now();
        user.setConsentGiven(true);
        user.setConsentGivenAt(now);
        user.setConsentVersion(version);
        userRepository.save(user);

        String ip = extractIp(request);
        String userAgent = extractUserAgent(request);

        // Registra um evento GRANT para cada finalidade obrigatória.
        Arrays.stream(ConsentPurpose.values())
                .filter(ConsentPurpose::isMandatory)
                .forEach(purpose -> persistEvent(user.getId(), "GRANT", purpose, version, ip, userAgent));

        logger.info("Consentimento inicial registrado para o usuário {} (versão {})",
                user.getEmail(), version);
    }

    // ============================================================
    //  CONCESSÃO INCREMENTAL — Finalidade adicional (Req. 4.5)
    // ============================================================

    /**
     * Registra a concessão de consentimento para uma finalidade específica
     * (tipicamente uma finalidade opcional como {@link ConsentPurpose#MARKETING}).
     *
     * @param user    titular autenticado
     * @param purpose finalidade do tratamento
     * @param version versão da política aceita
     * @param request requisição HTTP corrente
     */
    @Transactional
    public void grantConsent(User user, ConsentPurpose purpose, String version,
                             HttpServletRequest request) {
        if (purpose == null) {
            throw new BusinessException("Finalidade do consentimento é obrigatória.");
        }
        if (version == null || version.isBlank()) {
            version = CURRENT_POLICY_VERSION;
        }

        persistEvent(
                user.getId(),
                "GRANT",
                purpose,
                version,
                extractIp(request),
                extractUserAgent(request)
        );

        logger.info("Consentimento concedido: usuário={}, finalidade={}, versão={}",
                user.getEmail(), purpose, version);
    }

    // ============================================================
    //  REVOGAÇÃO — Direito do titular (Req. 4.6)
    // ============================================================

    /**
     * Registra a revogação de consentimento para uma finalidade específica.
     *
     * <p>Caso a finalidade revogada seja obrigatória ({@link ConsentPurpose#isMandatory()}),
     * o método lança {@link BusinessException} orientando o titular a utilizar
     * o fluxo de exclusão de conta ({@code DELETE /lgpd/me}), pois a continuidade
     * do serviço é incompatível com a revogação do consentimento basilar.</p>
     *
     * @param user    titular autenticado
     * @param purpose finalidade a ser revogada
     * @param request requisição HTTP corrente
     * @throws BusinessException se a finalidade for obrigatória
     */
    @Transactional
    public void revokeConsent(User user, ConsentPurpose purpose, HttpServletRequest request) {
        if (purpose == null) {
            throw new BusinessException("Finalidade do consentimento é obrigatória.");
        }
        if (purpose.isMandatory()) {
            throw new BusinessException(
                    "A finalidade '" + purpose + "' é indispensável ao serviço. " +
                            "Para revogá-la, utilize o fluxo de exclusão de conta."
            );
        }

        persistEvent(
                user.getId(),
                "REVOKE",
                purpose,
                user.getConsentVersion() != null ? user.getConsentVersion() : CURRENT_POLICY_VERSION,
                extractIp(request),
                extractUserAgent(request)
        );

        logger.info("Consentimento revogado: usuário={}, finalidade={}",
                user.getEmail(), purpose);
    }

    // ============================================================
    //  CONSULTA DE ESTADO — Auxilia /lgpd/me/data (Req. 4.8)
    // ============================================================

    /**
     * Recupera o estado atual dos consentimentos do titular, agregando o
     * último evento de cada finalidade para determinar se está ativa ou revogada.
     */
    public ConsentStatusDTO getStatus(User user) {
        List<ConsentLog> history = consentLogRepository
                .findByUserIdOrderByTimestampDesc(user.getId());

        // Agrupa por finalidade e pega o evento mais recente de cada uma.
        Map<String, ConsentLog> latestByPurpose = history.stream()
                .collect(Collectors.toMap(
                        ConsentLog::getPurpose,
                        log -> log,
                        (existing, replacement) -> existing // já está ordenado DESC
                ));

        List<ConsentPurpose> active = new ArrayList<>();
        List<ConsentPurpose> revoked = new ArrayList<>();

        for (ConsentPurpose purpose : ConsentPurpose.values()) {
            ConsentLog last = latestByPurpose.get(purpose.name());
            if (last != null && "GRANT".equals(last.getAction())) {
                active.add(purpose);
            } else if (last != null && "REVOKE".equals(last.getAction())) {
                revoked.add(purpose);
            }
        }

        return new ConsentStatusDTO(
                user.getConsentGiven(),
                user.getConsentGivenAt(),
                user.getConsentVersion(),
                active,
                revoked
        );
    }

    /**
     * Recupera o histórico completo de manifestações do titular.
     * Utilizado pelo endpoint de exportação de dados ({@code GET /lgpd/me/export}, Req. 4.9).
     */
    public List<ConsentLog> getFullHistory(UUID userId) {
        return consentLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    // ============================================================
    //  VERIFICAÇÃO DE VERSÃO — Re-aceite quando política mudar
    // ============================================================

    /**
     * Verifica se o titular precisa reaceitar uma nova versão da política.
     * Retorna {@code true} se:
     * <ul>
     *   <li>O usuário nunca aceitou ({@code consentGiven == false}); ou</li>
     *   <li>A versão aceita pelo usuário difere da {@link #CURRENT_POLICY_VERSION}.</li>
     * </ul>
     */
    public boolean requiresConsentRenewal(User user) {
        if (user.getConsentGiven() == null || !user.getConsentGiven()) {
            return true;
        }
        return !CURRENT_POLICY_VERSION.equals(user.getConsentVersion());
    }

    // ============================================================
    //  HELPERS INTERNOS
    // ============================================================

    private void persistEvent(UUID userId, String action, ConsentPurpose purpose,
                              String version, String ip, String userAgent) {
        ConsentLog event = new ConsentLog(
                userId,
                action,
                purpose.name(),
                version,
                ip,
                userAgent
        );
        consentLogRepository.save(event);
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // Prefere o cabeçalho X-Forwarded-For (em cenário com Nginx proxy reverso).
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For pode conter uma lista; o primeiro é o cliente real.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ua = request.getHeader("User-Agent");
        if (ua == null) {
            return null;
        }
        // Trunca para o tamanho da coluna (500 chars).
        return ua.length() > 500 ? ua.substring(0, 500) : ua;
    }
}
