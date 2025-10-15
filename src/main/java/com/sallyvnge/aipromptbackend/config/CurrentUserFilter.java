package com.sallyvnge.aipromptbackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class CurrentUserFilter extends OncePerRequestFilter {
    private final DevHeaderUserResolver resolver;
    private final CurrentUserProvider provider;

    public CurrentUserFilter(DevHeaderUserResolver resolver, CurrentUserProvider provider) {
        this.resolver = resolver;
        this.provider = provider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var cu = resolver.resolve(request);
        provider.set(cu);
        chain.doFilter(request, response);
    }

}
