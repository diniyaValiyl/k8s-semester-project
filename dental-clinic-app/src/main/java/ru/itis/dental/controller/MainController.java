package ru.itis.dental.controller;

import ru.itis.dental.dto.RegistrationDTO;
import ru.itis.dental.entity.UserEntity;
import ru.itis.dental.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final UserService userService;

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            // Получаем email для разных типов аутентификации
            String email = null;
            Object principal = authentication.getPrincipal();

            if (principal instanceof OAuth2User) {
                // Для входа через Google
                email = ((OAuth2User) principal).getAttribute("email");
                log.debug("OAuth2 user email: {}", email);
            } else {
                // Для обычной формы логина
                email = authentication.getName();
                log.debug("Form login email: {}", email);
            }

            if (email != null) {
                String finalEmail = email;
                userService.findByEmail(finalEmail).ifPresent(user -> {
                    model.addAttribute("userName", user.getName());
                    model.addAttribute("userRole", user.getRole().name());
                    model.addAttribute("isAuthenticated", true);
                });
            }
        } else {
            model.addAttribute("isAuthenticated", false);
        }
        return "index";
    }

    @GetMapping("/login")
    public String login(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registrationDTO")) {
            model.addAttribute("registrationDTO", new RegistrationDTO());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationDTO") RegistrationDTO dto,
                           BindingResult bindingResult,
                           Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Пожалуйста, исправьте ошибки в форме");
            return "register";
        }

        try {
            // устанавливаем роль PATIENT
            userService.register(dto.getName(), dto.getEmail(), dto.getPassword(), UserEntity.Role.PATIENT);
            model.addAttribute("success", "Регистрация успешно завершена!");
            return "login";
        } catch (Exception e) {
            log.error("Ошибка регистрации: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}