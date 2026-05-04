package com.example.project.controller;

import com.example.project.dto.RegionDTO;
import com.example.project.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("regions", regionService.findAll());
        return "admin/regions/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("regionDTO", new RegionDTO());
        return "admin/regions/create";
    }

    @PostMapping("/create")
    public String save(@ModelAttribute RegionDTO dto) {
        regionService.save(dto);
        return "redirect:/admin/regions";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var region = regionService.findById(id);
        RegionDTO dto = new RegionDTO();
        dto.setId(region.getId());
        dto.setName(region.getName());
        dto.setDescription(region.getDescription());
        model.addAttribute("regionDTO", dto);
        return "admin/regions/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute RegionDTO dto) {
        regionService.update(id, dto);
        return "redirect:/admin/regions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        regionService.deleteById(id);
        return "redirect:/admin/regions";
    }
}
