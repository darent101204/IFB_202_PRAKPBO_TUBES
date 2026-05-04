package com.example.project.controller;

import com.example.project.dto.WasteCategoryDTO;
import com.example.project.service.WasteCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/waste-categories")
@RequiredArgsConstructor
public class WasteCategoryController {

    private final WasteCategoryService wasteCategoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", wasteCategoryService.findAll());
        return "admin/waste-categories/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("categoryDTO", new WasteCategoryDTO());
        return "admin/waste-categories/create";
    }

    @PostMapping("/create")
    public String save(@ModelAttribute WasteCategoryDTO dto) {
        wasteCategoryService.save(dto);
        return "redirect:/admin/waste-categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var category = wasteCategoryService.findById(id);
        WasteCategoryDTO dto = new WasteCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setUnit(category.getUnit());
        model.addAttribute("categoryDTO", dto);
        return "admin/waste-categories/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute WasteCategoryDTO dto) {
        wasteCategoryService.update(id, dto);
        return "redirect:/admin/waste-categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        wasteCategoryService.deleteById(id);
        return "redirect:/admin/waste-categories";
    }
}
