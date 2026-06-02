package com.example.project.controller;

import com.example.project.dto.PickupRequestDTO;
import com.example.project.dto.RequestItemDTO;
import com.example.project.exception.BusinessException;
import com.example.project.model.PickupRequest;
import com.example.project.model.RequestStatus;
import com.example.project.model.Role;
import com.example.project.model.User;
import com.example.project.service.PickupRequestService;
import com.example.project.service.WasteCategoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/requests")
@RequiredArgsConstructor
public class PickupRequestController {

    private final PickupRequestService pickupRequestService;
    private final WasteCategoryService wasteCategoryService;

    @GetMapping("/create")
    public String createForm(Model model) {
        PickupRequestDTO dto = new PickupRequestDTO();
        List<RequestItemDTO> items = new ArrayList<>();
        items.add(new RequestItemDTO());
        dto.setItems(items);
        model.addAttribute("requestDTO", dto);
        model.addAttribute("categories", wasteCategoryService.findAll());
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
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != Role.RESIDENT) {
            return "redirect:/login";
        }
        
        PickupRequest request = pickupRequestService.findById(id);
        if (request.getResident() == null || !request.getResident().getId().equals(user.getId())) {
            throw new BusinessException("Anda tidak memiliki akses ke request ini");
        }
        if (request.getStatus() != RequestStatus.PENDING_APPROVAL) {
            throw new BusinessException("Request yang sudah diproses tidak dapat diedit");
        }
        
        PickupRequestDTO dto = new PickupRequestDTO();
        dto.setId(request.getId());
        dto.setRtId(request.getRt() != null ? request.getRt().getId() : null);
        dto.setNotes(request.getNotes());
        dto.setScheduledDate(request.getScheduledDate());
        
        List<RequestItemDTO> itemDTOs = new ArrayList<>();
        if (request.getItems() != null) {
            for (com.example.project.model.RequestItem item : request.getItems()) {
                RequestItemDTO itemDTO = new RequestItemDTO();
                itemDTO.setCategoryId(item.getCategory() != null ? item.getCategory().getId() : null);
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setNotes(item.getNotes());
                itemDTOs.add(itemDTO);
            }
        }
        if (itemDTOs.isEmpty()) {
            itemDTOs.add(new RequestItemDTO());
        }
        dto.setItems(itemDTOs);
        
        model.addAttribute("request", request);
        model.addAttribute("requestDTO", dto);
        model.addAttribute("categories", wasteCategoryService.findAll());
        return "requests/edit";
    }

    @PostMapping("/{id}/edit")
    public String doEdit(@PathVariable Long id, @ModelAttribute PickupRequestDTO dto, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != Role.RESIDENT) {
            return "redirect:/login";
        }
        pickupRequestService.updateRequest(id, dto, user);
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/delete")
    public String doDelete(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != Role.RESIDENT) {
            return "redirect:/login";
        }
        try {
            pickupRequestService.deleteRequest(id, user);
        } catch (BusinessException e) {
            // Silently redirect; the service already logs the error
        }
        return "redirect:/dashboard";
    }
}
