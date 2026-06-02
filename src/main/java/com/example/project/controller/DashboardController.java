package com.example.project.controller;

import com.example.project.model.PickupRequest;
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

import java.util.List;

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

        // Pre-resolve lazy-loaded region/RT names while Hibernate session is active
        // to avoid LazyInitializationException in Thymeleaf templates
        String regionName = "Semua Wilayah";
        String rtName = "-";
        try {
            if (user.getRegion() != null && user.getRegion().getName() != null) {
                regionName = user.getRegion().getName();
            } else if (user.getRt() != null && user.getRt().getRegion() != null
                       && user.getRt().getRegion().getName() != null) {
                regionName = user.getRt().getRegion().getName();
            }
        } catch (Exception ignored) {
            // fallback to default if proxy fails
        }
        try {
            if (user.getRt() != null && user.getRt().getName() != null) {
                rtName = user.getRt().getName();
            }
        } catch (Exception ignored) {
            // fallback to default if proxy fails
        }
        model.addAttribute("regionName", regionName);
        model.addAttribute("rtName", rtName);

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

                // Total waste submitted (sum of all item quantities)
                double totalWaste = calculateTotalWaste(myRequests);
                model.addAttribute("totalWaste", String.format("%.1f", totalWaste));

                // Waste Category Distribution for charts
                var residentCategoryMap = myRequests.stream()
                        .filter(r -> r.getItems() != null)
                        .flatMap(r -> r.getItems().stream())
                        .filter(item -> item.getCategory() != null && item.getQuantity() != null)
                        .collect(java.util.stream.Collectors.groupingBy(
                                item -> item.getCategory().getName(),
                                java.util.stream.Collectors.summingDouble(item -> item.getQuantity())
                        ));
                model.addAttribute("residentCategoryLabels", new java.util.ArrayList<>(residentCategoryMap.keySet()));
                model.addAttribute("residentCategoryValues", new java.util.ArrayList<>(residentCategoryMap.values()));

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

                // Total waste in RT area (sum of all item quantities)
                double totalWaste = calculateTotalWaste(rtRequests);
                model.addAttribute("totalWaste", String.format("%.1f", totalWaste));

                // Request Status counts for charts
                long pendingApproval = rtRequests.stream().filter(r -> r.getStatus() == RequestStatus.PENDING_APPROVAL).count();
                long scheduled = rtRequests.stream().filter(r -> r.getStatus() == RequestStatus.SCHEDULED).count();
                long completed = rtRequests.stream().filter(r -> r.getStatus() == RequestStatus.COMPLETED).count();
                long rejected = rtRequests.stream().filter(r -> r.getStatus() == RequestStatus.REJECTED).count();

                model.addAttribute("rtStatusLabels", java.util.List.of("Pending Approval", "Scheduled", "Completed", "Rejected"));
                model.addAttribute("rtStatusValues", java.util.List.of(pendingApproval, scheduled, completed, rejected));

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

                // Total waste collected (sum of all assigned item quantities)
                double totalWaste = calculateTotalWaste(myTasks);
                model.addAttribute("totalWaste", String.format("%.1f", totalWaste));

                // Waste Category Distribution for charts
                var collectorCategoryMap = myTasks.stream()
                        .filter(r -> r.getItems() != null)
                        .flatMap(r -> r.getItems().stream())
                        .filter(item -> item.getCategory() != null && item.getQuantity() != null)
                        .collect(java.util.stream.Collectors.groupingBy(
                                item -> item.getCategory().getName(),
                                java.util.stream.Collectors.summingDouble(item -> item.getQuantity())
                        ));
                model.addAttribute("collectorCategoryLabels", new java.util.ArrayList<>(collectorCategoryMap.keySet()));
                model.addAttribute("collectorCategoryValues", new java.util.ArrayList<>(collectorCategoryMap.values()));

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

    /**
     * Calculate total waste quantity from a list of pickup requests.
     * Sums up all request item quantities.
     */
    private double calculateTotalWaste(List<PickupRequest> requests) {
        return requests.stream()
                .filter(r -> r.getItems() != null)
                .flatMap(r -> r.getItems().stream())
                .filter(item -> item.getQuantity() != null)
                .mapToDouble(item -> item.getQuantity())
                .sum();
    }
}

