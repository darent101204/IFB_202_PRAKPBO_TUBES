package com.example.project.controller;

import com.example.project.dto.RtDTO;
import com.example.project.service.RegionService;
import com.example.project.service.RtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/rts")
@RequiredArgsConstructor
public class RtController {

    private final RtService rtService;
    private final RegionService regionService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rts", rtService.findAll());
        return "admin/rts/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("rtDTO", new RtDTO());
        model.addAttribute("regions", regionService.findAll());
        return "admin/rts/create";
    }

    @PostMapping("/create")
    public String save(@ModelAttribute RtDTO dto) {
        rtService.save(dto);
        return "redirect:/admin/rts";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var rt = rtService.findById(id);
        RtDTO dto = new RtDTO();
        dto.setId(rt.getId());
        dto.setName(rt.getName());
        dto.setRegionId(rt.getRegion().getId());
        dto.setContactPhone(rt.getContactPhone());
        dto.setAddress(rt.getAddress());
        model.addAttribute("rtDTO", dto);
        model.addAttribute("regions", regionService.findAll());
        return "admin/rts/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute RtDTO dto) {
        rtService.update(id, dto);
        return "redirect:/admin/rts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        rtService.deleteById(id);
        return "redirect:/admin/rts";
    }
}
