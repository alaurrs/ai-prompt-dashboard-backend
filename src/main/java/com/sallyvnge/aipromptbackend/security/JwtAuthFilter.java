package com.sallyvnge.aipromptbackend.security;

import com.sallyvnge.aipromptbackend.service.AppUserDetailsService;
import com.sallyvnge.aipromptbackend.repository.UserRepository;
import com.sallyvnge.aipromptbackend.domain.UserEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Normalize path to be independent of server.servlet.context-path
        String ctx = request.getContextPath(); // e.g., "/api" or ""
        String uri = request.getRequestURI();  // e.g., "/api/auth/login"
        String path = (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;
        return path.startsWith("/auth/") || path.startsWith("/actuator/") || request.getMethod().equalsIgnoreCase("OPTIONS");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
    throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) {
            header = request.getHeader("X-Forwarded-Authorization");
        }
        String ctx = request.getContextPath();
        String uri = request.getRequestURI();
        String path = (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;
        if (log.isDebugEnabled()) {
            log.debug("JwtAuthFilter path={}, method={}, hasAuthHeader={}", path, request.getMethod(), header != null);
        }

        String bearer = null;
        if (header != null) {
            String h = header.trim();
            if (h.regionMatches(true, 0, "Bearer ", 0, 7) && h.length() > 7) {
                bearer = h.substring(7).trim();
            }
        } else {
            // Fallback for SSE/EventSource: allow access_token on respond endpoint (GET only)
            String qp = request.getParameter("access_token");
            if (qp != null && !qp.isBlank()) {
                // Allow query token for any non-public path (covers SSE POST/GET and other clients)
                if (!path.startsWith("/auth/") && !path.startsWith("/actuator/")) {
                    bearer = qp;
                    if (log.isDebugEnabled()) log.debug("Using access_token query param for path={}", path);
                }
            }
            // Cookie fallback
            if (bearer == null && request.getCookies() != null) {
                for (var c : request.getCookies()) {
                    if ("access_token".equalsIgnoreCase(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                        bearer = c.getValue();
                        if (log.isDebugEnabled()) log.debug("Using access_token cookie");
                        break;
                    }
                }
            }
        }

        if (bearer != null) {
            String token = bearer;
            try {
                JwtService.JwtPrincipal principal = jwtService.validateAndParse(token);

                if ("refresh".equals(principal.tokenType())) {
                    if (log.isDebugEnabled()) log.debug("Rejecting refresh token on protected route");
                    response.setHeader("X-Auth-Reason", "refresh_token_on_protected_route");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid refresh token");
                    return;
                }

                UserDetails userDetails = null;
                String emailFromToken = principal.email();
                if (emailFromToken != null && !emailFromToken.isBlank()) {
                    userDetails = userDetailsService.loadUserByUsername(emailFromToken);
                } else if (principal.userId() != null) {
                    // Fallback: resolve user by ID (some tokens may omit email)
                    UserEntity ue = userRepository.findById(principal.userId()).orElse(null);
                    if (ue != null && ue.getEmail() != null && !ue.getEmail().isBlank()) {
                        userDetails = userDetailsService.loadUserByUsername(ue.getEmail());
                        if (log.isDebugEnabled()) log.debug("Resolved email={} from userId={}", ue.getEmail(), principal.userId());
                    }
                }

                if (userDetails == null) {
                    // As a last resort, authenticate with roles from token (default ROLE_USER)
                    var roles = (principal.roles() == null || principal.roles().isEmpty())
                            ? java.util.List.of("ROLE_USER")
                            : principal.roles();
                    var authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    if (log.isDebugEnabled()) log.debug("Authentication set from token roles for userId={}", principal.userId());
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        userDetails.getAuthorities()
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                if (log.isDebugEnabled()) log.debug("Authentication set for principal email={}", userDetails.getUsername());
            } catch (Exception e) {
                if (log.isDebugEnabled()) log.debug("JWT validation failed: {}", e.getMessage());
                response.setHeader("X-Auth-Reason", "jwt_invalid");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
