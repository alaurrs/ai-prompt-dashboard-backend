package com.sallyvnge.aipromptbackend.service;

import com.sallyvnge.aipromptbackend.domain.UserEntity;
import com.sallyvnge.aipromptbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow( () -> new UsernameNotFoundException("Not found"));

        if (!UserEntity.Status.ACTIVE.equals(user.getStatus())) throw new DisabledException("Account not active");
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_USER")
                .accountLocked(UserEntity.Status.LOCKED.equals(user.getStatus()))
                .disabled(UserEntity.Status.DISABLED.equals(user.getStatus()))
                .build();
    }
}
