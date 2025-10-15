package com.sallyvnge.aipromptbackend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptOnce {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("Solutec123456"));
    }
}
