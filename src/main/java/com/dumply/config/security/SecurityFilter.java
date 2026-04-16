package com.dumply.config.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.dumply.service.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dumply.config.tenant.TenantContext;
import com.dumply.model.User;
import com.dumply.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenBlacklistService blacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = recoverToken(request);
        if (token != null) {
            if (blacklistService.isTokenBlacklisted(token)) {
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token blacklisted.");
                return;
            }
        }

        try {
            if (token != null) {
                try {
                    var decodedJWT = tokenService.validateToken(token);

                    if (decodedJWT == null) {
                        writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token inválido ou expirado.");
                        return;
                    }

                    String email = decodedJWT.getSubject();
                    String companyIdStr = decodedJWT.getClaim("companyId").asString();
                    UUID companyId = companyIdStr != null ? UUID.fromString(companyIdStr) : null;
                    String role = decodedJWT.getClaim("role").asString();

                    TenantContext.setCompanyId(companyId);

                    // Futuramente estudar outra lógica que não precise revalidar no banco toda hora, como o JWT é assinado e confiável..
                    // Buscar por email + companyId para evitar vazamento entre tenants (mesmo email em empresas diferentes)
                    User user = userRepository.findByEmailAndCompanyId(email, companyId)
                            .orElseThrow(() -> new AccessDeniedException("Credenciais inválidas"));

                    // Garantir que o usuário pertence à empresa do token (defesa em profundidade)
                    if (!user.getCompany().getId().equals(companyId)) {
                        writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Acesso proibido");
                        return;
                    }


                    var authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    );

                    var userDetails =
                            new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authorities);

                    var authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, token, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } catch (JWTVerificationException ex) {
                    // Token inválido, expirado ou erro na busca do usuário
                    writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token inválido ou expirado.");
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // MUITO IMPORTANTE: Limpar o contexto independente do resultado
            TenantContext.clear();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7);
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        Map<String, String> error = new HashMap<>();
        error.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
