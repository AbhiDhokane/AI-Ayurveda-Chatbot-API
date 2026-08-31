package com.ayurveda.bot.controller;

import com.ayurveda.bot.model.Herb;
import com.ayurveda.bot.service.HerbService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Optional;

@Controller
public class HerbController {

    private final HerbService herbService;

    public HerbController(HerbService herbService) {
        this.herbService = herbService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("herbs", herbService.getAllHerbs());
        model.addAttribute("categories", herbService.getCategories());
        return "index";
    }

    @GetMapping("/herbs")
    public String listHerbs(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String category,
                            Model model) {
        List<Herb> herbs;
        if (category != null && !category.isBlank()) {
            herbs = herbService.getByCategory(category);
        } else if (q != null && !q.isBlank()) {
            herbs = herbService.searchHerbs(q);
        } else {
            herbs = herbService.getAllHerbs();
        }
        model.addAttribute("herbs", herbs);
        model.addAttribute("categories", herbService.getCategories());
        model.addAttribute("searchQuery", q);
        model.addAttribute("selectedCategory", category);
        return "herbs";
    }

    @GetMapping("/herb/{id}")
    public String herbDetail(@PathVariable String id, Model model) {
        Optional<Herb> herbOpt = herbService.getHerbById(id);
        if (herbOpt.isEmpty()) {
            return "redirect:/herbs";
        }
        model.addAttribute("herb", herbOpt.get());
        return "herb-detail";
    }

    @GetMapping("/chat")
    public String chatPage(HttpSession session, Model model) {
        if (session.getAttribute(AuthController.USER_ID) == null) return "redirect:/login";
        model.addAttribute("userName", session.getAttribute(AuthController.USER_NAME));
        model.addAttribute("userEmail", session.getAttribute(AuthController.USER_EMAIL));
        return "chat";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
