package com.example.project.controller;

import com.example.project.dto.UserDTO;
import com.example.project.model.Role;
import com.example.project.service.RegionService;
import com.example.project.service.RtService;
import com.example.project.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RegionService regionService;
    private final RtService rtService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "users/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        model.addAttribute("roles", Role.values());
        model.addAttribute("regions", regionService.findAll());
        model.addAttribute("rts", rtService.findAll());
        return "users/create";
    }

    @PostMapping("/create")
    public String save(@ModelAttribute UserDTO dto) {
        userService.save(dto);
        return "redirect:/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        UserDTO dto = new UserDTO();
        var user = userService.findById(id);
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        if (user.getRegion() != null) dto.setRegionId(user.getRegion().getId());
        if (user.getRt() != null) dto.setRtId(user.getRt().getId());
        
        model.addAttribute("userDTO", dto);
        model.addAttribute("roles", Role.values());
        model.addAttribute("regions", regionService.findAll());
        model.addAttribute("rts", rtService.findAll());
        return "users/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute UserDTO dto) {
        userService.update(id, dto);
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        userService.deleteById(id);
        return "redirect:/users";
    }
}
