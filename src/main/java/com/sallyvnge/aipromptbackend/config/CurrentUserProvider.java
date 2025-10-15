package com.sallyvnge.aipromptbackend.config;

import java.util.Optional;

public interface CurrentUserProvider {
    Optional<CurrentUser> get();
    CurrentUser require();
    void set(CurrentUser currentUser);
}
