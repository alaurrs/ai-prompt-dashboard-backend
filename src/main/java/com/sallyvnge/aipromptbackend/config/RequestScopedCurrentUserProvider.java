package com.sallyvnge.aipromptbackend.config;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;

@Component
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestScopedCurrentUserProvider implements CurrentUserProvider {
    private CurrentUser currentUser;
    @Override public Optional<CurrentUser> get() {
        return Optional.ofNullable(currentUser);
    }

    @Override
    public CurrentUser require() {
        if (currentUser == null) {
            throw new IllegalStateException("No current user");
        }
        return currentUser;
    }
    @Override
    public void set(CurrentUser user) {
         this.currentUser = user;
    }
}
