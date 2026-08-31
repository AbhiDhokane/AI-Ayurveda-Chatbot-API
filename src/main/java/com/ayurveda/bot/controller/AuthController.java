package com.ayurveda.bot.controller;

import com.ayurveda.bot.model.AppUser;
import com.ayurveda.bot.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    public static final String USER_ID = "userId";
    public static final String USER_NAME = "userName";
    public static final String USER_EMAIL = "userEmail";
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        try {
            AppUser user = authService.login(email, password);
            session.setAttribute(USER_ID, user.getId());
            session.setAttribute(USER_NAME, user.getName());
            session.setAttribute(USER_EMAIL, user.getEmail());
            return "redirect:/chat";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/signup")
    public String signupPage() { return "signup"; }

    @PostMapping("/signup")
    public String signup(@RequestParam String name, @RequestParam String email, @RequestParam String password,
                         HttpSession session, Model model) {
        try {
            AppUser user = authService.signup(name, email, password);
            session.setAttribute(USER_ID, user.getId());
            session.setAttribute(USER_NAME, user.getName());
            session.setAttribute(USER_EMAIL, user.getEmail());
            return "redirect:/chat";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "signup";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
