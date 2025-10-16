package com.sallyvnge.aipromptbackend.api.controller;

import com.sallyvnge.aipromptbackend.api.dto.users.UserMeDto;
import com.sallyvnge.aipromptbackend.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserMeDto> me(@AuthenticationPrincipal JwtService.JwtPrincipal principal) {
        String display = principal.displayName();
        String avatar  = "https://api.dicebear.com/7.x/identicon/svg?seed=" + principal.email();
        return ResponseEntity.ok(new UserMeDto(principal.email(), display, avatar));
    }
}
