package com.sallyvnge.aipromptbackend.config;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class DevHeaderUserResolver {
    public CurrentUser resolve(HttpServletRequest req) {
        String email = req.getHeader("X-Demo-Email");
        if (email == null || email.isBlank()) email = "me@example.com";
        return new CurrentUser(null, email, null);
    }
}
