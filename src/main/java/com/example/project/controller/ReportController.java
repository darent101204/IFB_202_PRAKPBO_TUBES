package com.example.project.controller;

import com.example.project.model.Role;
import com.example.project.model.User;
import com.example.project.service.PdfReportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final PdfReportService pdfReportService;

    @GetMapping("/rt/pdf")
    public void exportRtPdf(HttpSession session, HttpServletResponse response) throws IOException {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != Role.RT) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"rt-report.pdf\"");

        pdfReportService.generateRtPdf(user, response.getOutputStream());
    }

    @GetMapping("/collector/pdf")
    public void exportCollectorPdf(HttpSession session, HttpServletResponse response) throws IOException {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != Role.COLLECTOR) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"collector-report.pdf\"");

        pdfReportService.generateCollectorPdf(user, response.getOutputStream());
    }
}
