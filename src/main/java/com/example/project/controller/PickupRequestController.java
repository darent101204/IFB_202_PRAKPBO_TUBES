package com.example.project.controller;

import com.example.project.dto.PickupRequestDTO;
import com.example.project.model.PickupRequest;
import com.example.project.model.RequestStatus;
import com.example.project.model.User;
import com.example.project.service.PickupRequestService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/requests")
@RequiredArgsConstructor
public class PickupRequestController {

    private final PickupRequestService pickupRequestService;

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("requestDTO", new PickupRequestDTO());
        return "requests/create";
    }

    @PostMapping("/create")
    public String doCreate(@ModelAttribute PickupRequestDTO dto, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        pickupRequestService.createRequest(dto, user);
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        pickupRequestService.approveRequest(id, user);
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        pickupRequestService.rejectRequest(id, user);
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/start")
    public String startTask(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        pickupRequestService.updateStatus(id, RequestStatus.ON_PROGRESS, user);
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/complete")
    public String completeTask(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        pickupRequestService.updateStatus(id, RequestStatus.COMPLETED, user);
        return "redirect:/dashboard";
    }

    @PostMapping("/collective")
    public String makeCollective(@RequestParam Long masterId, @RequestParam List<Long> childIds, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        pickupRequestService.makeCollective(masterId, childIds, user);
        return "redirect:/dashboard";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        PickupRequest req = pickupRequestService.findById(id);

        PickupRequestDTO dto = new PickupRequestDTO();
        dto.setNotes(req.getNotes());
        dto.setScheduledDate(req.getScheduledDate());

        model.addAttribute("requestDTO", dto);
        model.addAttribute("id", id);

        return "requests/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                        @ModelAttribute PickupRequestDTO dto,
                        HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        pickupRequestService.updateRequest(id, dto, user);

        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        pickupRequestService.deleteById(id);
        return "redirect:/dashboard";
    }

}
