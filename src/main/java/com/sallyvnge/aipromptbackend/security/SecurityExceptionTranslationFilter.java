package com.sallyvnge.aipromptbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Translates security exceptions thrown by downstream filters (e.g. AuthorizationFilter)
 * into clean HTTP responses instead of letting them bubble to the container.
 *
 * This complements ExceptionTranslationFilter by also handling AuthorizationDeniedException
 * explicitly, which may not always be translated depending on filter order/version.
 */
@Slf4j
public class SecurityExceptionTranslationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (AccessDeniedException ex) {
            if (!response.isCommitted()) {
                if (log.isDebugEnabled()) {
                    log.debug("Access denied on path={}: {}", request.getRequestURI(), ex.getMessage());
                }
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setHeader("X-Auth-Reason", "access_denied");
                response.setHeader("X-Auth-Path", request.getRequestURI());
                response.getWriter().write("{\"error\":\"Access Denied\"}");
            }
        } catch (AuthenticationException ex) {
            if (!response.isCommitted()) {
                if (log.isDebugEnabled()) {
                    log.debug("Unauthenticated on path={}: {}", request.getRequestURI(), ex.getMessage());
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setHeader("X-Auth-Reason", "unauthenticated");
                response.setHeader("X-Auth-Path", request.getRequestURI());
                response.getWriter().write("{\"error\":\"Unauthorized\"}");
            }
        }
    }
}
