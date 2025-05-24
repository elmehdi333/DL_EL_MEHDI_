package com.example.annotation.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RedirectController {

    @GetMapping("/redirect")
    public String redirectByRole(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = auth.getName();
        model.addAttribute("username", username);

        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return "admin/home"; // ✅ matches templates/admin/home.html
            } else if ("ROLE_ANNOTATOR".equals(authority.getAuthority())) {
                return "annotator/home"; // ✅ matches templates/annotator/home.html
            }
        }

        return "redirect:/login";
    }
}
