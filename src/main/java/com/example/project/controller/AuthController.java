package com.example.project.controller;

import com.example.project.dto.LoginDTO;
import com.example.project.model.User;
import com.example.project.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("loginDTO", new LoginDTO());
        return "auth/login";
    }

    @PostMapping("/login")
    public String doLogin(@ModelAttribute LoginDTO loginDTO,
                          HttpSession session, Model model) {
        String email = loginDTO.getEmail() != null ? loginDTO.getEmail().trim().toLowerCase() : null;
        String password = loginDTO.getPassword();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("loginDTO", loginDTO);
            model.addAttribute("error", "Email dan password tidak boleh kosong");
            return "auth/login";
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !user.getPassword().equals(password)) {
            model.addAttribute("loginDTO", loginDTO);
            model.addAttribute("error", "Email atau password salah");
            return "auth/login";
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            model.addAttribute("loginDTO", loginDTO);
            model.addAttribute("error", "Akun Anda tidak aktif. Hubungi administrator.");
            return "auth/login";
        }

        session.setAttribute("loggedInUser", user);
        session.setAttribute("userRole", user.getRole().name());
        
        // Redirect based on role
        return switch (user.getRole()) {
            case ADMIN -> "redirect:/dashboard";
            case RT -> "redirect:/dashboard";
            case COLLECTOR -> "redirect:/dashboard";
            case RESIDENT -> "redirect:/dashboard";
            default -> "redirect:/dashboard";
        };
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }
}
