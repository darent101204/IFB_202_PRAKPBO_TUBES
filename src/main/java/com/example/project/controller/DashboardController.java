package com.example.project.controller;

import com.example.project.model.RequestStatus;
import com.example.project.model.Role;
import com.example.project.model.User;
import com.example.project.service.PickupRequestService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final PickupRequestService pickupRequestService;

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        model.addAttribute("currentUser", user);

        switch (user.getRole()) {
            case RESIDENT -> {
                var myRequests = pickupRequestService.findByResident(user);
                if (myRequests == null) myRequests = java.util.Collections.emptyList();
                
                model.addAttribute("myRequests", myRequests);
                model.addAttribute("totalRequests", myRequests.size());
                model.addAttribute("pendingCount", myRequests.stream()
                        .filter(r -> r.getStatus() == RequestStatus.PENDING_APPROVAL).count());
                model.addAttribute("scheduledCount", myRequests.stream()
                        .filter(r -> r.getStatus() == RequestStatus.SCHEDULED).count());
                model.addAttribute("completedCount", myRequests.stream()
                        .filter(r -> r.getStatus() == RequestStatus.COMPLETED).count());
                return "dashboard/resident";
            }
            case RT -> {
                var rtRequests = pickupRequestService.findByRtUser(user);
                if (rtRequests == null) rtRequests = java.util.Collections.emptyList();
                
                model.addAttribute("rtRequests", rtRequests);
                model.addAttribute("totalRequests", rtRequests.size());
                model.addAttribute("pendingApprovalCount", rtRequests.stream()
                        .filter(r -> r.getStatus() == RequestStatus.PENDING_APPROVAL).count());
                model.addAttribute("scheduledCount", rtRequests.stream()
                        .filter(r -> r.getStatus() == RequestStatus.SCHEDULED).count());
                model.addAttribute("completedCount", rtRequests.stream()
                        .filter(r -> r.getStatus() == RequestStatus.COMPLETED).count());
                return "dashboard/rt";
            }
            case COLLECTOR -> {
                var myTasks = pickupRequestService.findByCollector(user);
                if (myTasks == null) myTasks = java.util.Collections.emptyList();
                
                model.addAttribute("myTasks", myTasks);
                model.addAttribute("totalTasks", myTasks.size());
                model.addAttribute("scheduledCount", myTasks.stream()
                        .filter(r -> r.getStatus() == RequestStatus.SCHEDULED).count());
                model.addAttribute("onProgressCount", myTasks.stream()
                        .filter(r -> r.getStatus() == RequestStatus.ON_PROGRESS).count());
                model.addAttribute("completedCount", myTasks.stream()
                        .filter(r -> r.getStatus() == RequestStatus.COMPLETED).count());
                return "dashboard/collector";
            }
            case ADMIN -> {
                var allRequests = pickupRequestService.findAll();
                if (allRequests == null) allRequests = java.util.Collections.emptyList();
                
                model.addAttribute("allRequests", allRequests);
                model.addAttribute("totalRequests", allRequests.size());
                
                for (RequestStatus status : RequestStatus.values()) {
                    long count = allRequests.stream().filter(r -> r.getStatus() == status).count();
                    model.addAttribute(status.name().toLowerCase() + "Count", count);
                }
                return "dashboard/admin";
            }
            default -> { 
                return "redirect:/login"; 
            }
        }
    }
}
