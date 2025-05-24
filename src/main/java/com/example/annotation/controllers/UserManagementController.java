package com.example.annotation.controllers;

import com.example.annotation.entities.Role;
import com.example.annotation.entities.User;
import com.example.annotation.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Random;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping
    public String listUsers(Model model,
                            @RequestParam(name = "active", required = false) Boolean active,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(name = "keyword", required = false) String keyword,
                            @RequestParam(name = "createdWithPassword", required = false) String createdPassword) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> pageResult = (keyword != null && !keyword.isEmpty()) ?
                ((active == null)
                        ? userRepository.findByUsernameContainingIgnoreCase(keyword, pageable)
                        : userRepository.findByUsernameContainingIgnoreCaseAndActive(keyword, active, pageable))
                : ((active == null)
                        ? userRepository.findAll(pageable)
                        : userRepository.findByActive(active, pageable));

        model.addAttribute("userPage", pageResult);
        model.addAttribute("pageSize", size);
        model.addAttribute("active", active);
        model.addAttribute("keyword", keyword);
        model.addAttribute("createdWithPassword", createdPassword);

        return "admin/users";
    }

    @GetMapping("/create")
    public String showCreateUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.values());
        return "admin/userForm";
    }

    @PostMapping("/create")
    public String createUser(@ModelAttribute User user, Model model) {
        if (userRepository.findByUsernameAndActiveTrue(user.getUsername()).isPresent()) {
            model.addAttribute("error", "Username already exists");
            model.addAttribute("roles", Role.values());
            return "admin/userForm";
        }

        String rawPassword = generateRandomPassword(10);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActive(true);
        userRepository.save(user);

        return "redirect:/admin/users?createdWithPassword=" + rawPassword;
    }

    @GetMapping("/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id).orElseThrow();
        user.setPassword("");
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "admin/userForm";
    }
    @GetMapping("/activate/{id}")
    public String activateUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setActive(true);
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    @GetMapping("/deactivate/{id}")
    public String deactivateUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setActive(false);
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user) {
        User existing = userRepository.findById(id).orElseThrow();
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        existing.setRole(user.getRole());

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        userRepository.save(existing);
        return "redirect:/admin/users";
    }

    @GetMapping("/delete/{id}")
    public String softDeleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setActive(false);
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    @GetMapping("/reactivate/{id}")
    public String reactivateUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setActive(true);
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$!";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

//    private void sendPasswordEmail(String to, String password) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true);
//            helper.setTo(to);
//            helper.setSubject("Your Account Password");
//            helper.setText("Your account has been created. Your password is: " + password);
//            mailSender.send(message);
//        } catch (MessagingException e) {
//            e.printStackTrace();
//        }
//    }

