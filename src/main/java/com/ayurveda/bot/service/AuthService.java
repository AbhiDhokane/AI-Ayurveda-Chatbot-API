package com.ayurveda.bot.service;

import com.ayurveda.bot.model.AppUser;
import com.ayurveda.bot.repository.AppUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AppUserRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(AppUserRepository repository) { this.repository = repository; }

    public AppUser signup(String name, String email, String password) {
        name = name == null ? "" : name.trim();
        email = email == null ? "" : email.trim().toLowerCase();
        if (name.length() < 2) throw new IllegalArgumentException("Please enter your full name.");
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) throw new IllegalArgumentException("Please enter a valid email address.");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password must contain at least 6 characters.");
        if (repository.existsByEmailIgnoreCase(email)) throw new IllegalArgumentException("An account with this email already exists.");
        return repository.save(new AppUser(name, email, encoder.encode(password)));
    }

    public AppUser login(String email, String password) {
        AppUser user = repository.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (!encoder.matches(password == null ? "" : password, user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid email or password.");
        return user;
    }
}
