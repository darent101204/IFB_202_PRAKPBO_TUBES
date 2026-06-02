package com.example.project.service.impl;

import com.example.project.model.PickupRequest;
import com.example.project.model.RequestStatus;
import com.example.project.model.User;
import com.example.project.service.PickupRequestService;
import com.example.project.service.PdfReportService;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfReportServiceImpl implements PdfReportService {

    private final PickupRequestService pickupRequestService;

    @Override
    public void generateRtPdf(User rtUser, OutputStream out) {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(44, 62, 80));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(127, 140, 141));
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

            // Title
            Paragraph title = new Paragraph("SMART WASTE PICKUP REPORT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Subtitle
            String rtName = (rtUser.getRt() != null) ? rtUser.getRt().getName() : "-";
            Paragraph subtitle = new Paragraph("RT Report - " + rtName, subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            // Date
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss"));
            Paragraph date = new Paragraph("Generated Date: " + dateStr, FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
            date.setAlignment(Element.ALIGN_CENTER);
            document.add(date);

            document.add(new Paragraph(" "));

            // Fetch and aggregate data
            List<PickupRequest> rtRequests = pickupRequestService.findByRtUser(rtUser);
            if (rtRequests == null) rtRequests = java.util.Collections.emptyList();

            long totalRequests = rtRequests.size();
            long pendingApproval = rtRequests.stream().filter(r -> r.getStatus() == RequestStatus.PENDING_APPROVAL).count();
            long scheduled = rtRequests.stream().filter(r -> r.getStatus() == RequestStatus.SCHEDULED).count();
            long completed = rtRequests.stream().filter(r -> r.getStatus() == RequestStatus.COMPLETED).count();
            double totalWaste = calculateTotalWaste(rtRequests);

            // Statistics Table
            Paragraph statsTitle = new Paragraph("Statistics Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(44, 62, 80)));
            document.add(statsTitle);
            document.add(new Paragraph(" "));

            PdfPTable statsTable = new PdfPTable(2);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingAfter(15f);

            addStatsRow(statsTable, "Total Requests in RT Area", String.valueOf(totalRequests), boldFont, normalFont);
            addStatsRow(statsTable, "Pending Approval", String.valueOf(pendingApproval), boldFont, normalFont);
            addStatsRow(statsTable, "Scheduled / Approved", String.valueOf(scheduled), boldFont, normalFont);
            addStatsRow(statsTable, "Completed", String.valueOf(completed), boldFont, normalFont);
            addStatsRow(statsTable, "Total Waste Collected", String.format("%.1f Kg", totalWaste), boldFont, normalFont);

            document.add(statsTable);

            // Data Table Title
            Paragraph tableTitle = new Paragraph("Request Summary Table (Last 50 Requests)", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(44, 62, 80)));
            document.add(tableTitle);
            document.add(new Paragraph(" "));

            // Data Table
            PdfPTable dataTable = new PdfPTable(6);
            dataTable.setWidthPercentage(100);
            dataTable.setWidths(new float[] {1f, 2.5f, 3.5f, 2f, 2.5f, 2.5f});
            dataTable.setHeaderRows(1);

            String[] headers = {"ID", "Resident", "Category", "Quantity", "Status", "Pickup Date"};
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setBackgroundColor(new Color(44, 62, 80));
                headerCell.setPadding(6);
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                dataTable.addCell(headerCell);
            }

            List<PickupRequest> sortedRequests = rtRequests.stream()
                    .sorted((r1, r2) -> r2.getId().compareTo(r1.getId()))
                    .limit(50)
                    .collect(Collectors.toList());

            int rowIndex = 0;
            for (PickupRequest req : sortedRequests) {
                Color rowColor = (rowIndex % 2 == 0) ? new Color(248, 249, 250) : Color.WHITE;

                addCell(dataTable, "#" + req.getId(), normalFont, rowColor, Element.ALIGN_CENTER);

                String residentName = (req.getResident() != null) ? req.getResident().getName() : "-";
                addCell(dataTable, residentName, normalFont, rowColor, Element.ALIGN_LEFT);

                String categories = "-";
                String quantities = "-";
                if (req.getItems() != null && !req.getItems().isEmpty()) {
                    categories = req.getItems().stream()
                            .map(item -> item.getCategory() != null ? item.getCategory().getName() : "-")
                            .collect(Collectors.joining("\n"));
                    quantities = req.getItems().stream()
                            .map(item -> {
                                String qtyStr = item.getQuantity() != null ? String.format("%.1f", item.getQuantity()) : "-";
                                String unitStr = (item.getCategory() != null && item.getCategory().getUnit() != null) ? item.getCategory().getUnit() : "Kg";
                                return qtyStr + " " + unitStr;
                            })
                            .collect(Collectors.joining("\n"));
                }
                addCell(dataTable, categories, normalFont, rowColor, Element.ALIGN_LEFT);
                addCell(dataTable, quantities, normalFont, rowColor, Element.ALIGN_RIGHT);

                String statusStr = (req.getStatus() != null) ? req.getStatus().name() : "-";
                addCell(dataTable, statusStr, normalFont, rowColor, Element.ALIGN_CENTER);

                String pickupDateStr = (req.getScheduledDate() != null) ? req.getScheduledDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-";
                addCell(dataTable, pickupDateStr, normalFont, rowColor, Element.ALIGN_CENTER);

                rowIndex++;
            }

            document.add(dataTable);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            document.close();
        }
    }

    @Override
    public void generateCollectorPdf(User collectorUser, OutputStream out) {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(44, 62, 80));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(127, 140, 141));
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

            // Title
            Paragraph title = new Paragraph("SMART WASTE PICKUP REPORT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Subtitle
            Paragraph subtitle = new Paragraph("Collector Tasks Report", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            // Date
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss"));
            Paragraph date = new Paragraph("Generated Date: " + dateStr, FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
            date.setAlignment(Element.ALIGN_CENTER);
            document.add(date);

            document.add(new Paragraph(" "));

            // Fetch and aggregate data
            List<PickupRequest> myTasks = pickupRequestService.findByCollector(collectorUser);
            if (myTasks == null) myTasks = java.util.Collections.emptyList();

            long totalTasks = myTasks.size();
            long completedTasks = myTasks.stream().filter(r -> r.getStatus() == RequestStatus.COMPLETED).count();
            double totalWaste = calculateTotalWaste(myTasks);

            // Statistics Table
            Paragraph statsTitle = new Paragraph("Collector Information & Statistics", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(44, 62, 80)));
            document.add(statsTitle);
            document.add(new Paragraph(" "));

            PdfPTable statsTable = new PdfPTable(2);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingAfter(15f);

            addStatsRow(statsTable, "Collector Name", collectorUser.getName(), boldFont, normalFont);
            addStatsRow(statsTable, "Total Assigned Tasks", String.valueOf(totalTasks), boldFont, normalFont);
            addStatsRow(statsTable, "Completed Tasks", String.valueOf(completedTasks), boldFont, normalFont);
            addStatsRow(statsTable, "Total Waste Collected", String.format("%.1f Kg", totalWaste), boldFont, normalFont);

            document.add(statsTable);

            // Data Table Title
            Paragraph tableTitle = new Paragraph("Tasks Summary Table", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(44, 62, 80)));
            document.add(tableTitle);
            document.add(new Paragraph(" "));

            // Data Table
            PdfPTable dataTable = new PdfPTable(5);
            dataTable.setWidthPercentage(100);
            dataTable.setWidths(new float[] {1f, 3f, 4f, 2.5f, 2.5f});
            dataTable.setHeaderRows(1);

            String[] headers = {"ID", "Resident", "Category", "Quantity", "Status"};
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setBackgroundColor(new Color(44, 62, 80));
                headerCell.setPadding(6);
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                dataTable.addCell(headerCell);
            }

            int rowIndex = 0;
            for (PickupRequest req : myTasks) {
                Color rowColor = (rowIndex % 2 == 0) ? new Color(248, 249, 250) : Color.WHITE;

                addCell(dataTable, "#" + req.getId(), normalFont, rowColor, Element.ALIGN_CENTER);

                String residentName = (req.getResident() != null) ? req.getResident().getName() : "-";
                addCell(dataTable, residentName, normalFont, rowColor, Element.ALIGN_LEFT);

                String categories = "-";
                String quantities = "-";
                if (req.getItems() != null && !req.getItems().isEmpty()) {
                    categories = req.getItems().stream()
                            .map(item -> item.getCategory() != null ? item.getCategory().getName() : "-")
                            .collect(Collectors.joining("\n"));
                    quantities = req.getItems().stream()
                            .map(item -> {
                                String qtyStr = item.getQuantity() != null ? String.format("%.1f", item.getQuantity()) : "-";
                                String unitStr = (item.getCategory() != null && item.getCategory().getUnit() != null) ? item.getCategory().getUnit() : "Kg";
                                return qtyStr + " " + unitStr;
                            })
                            .collect(Collectors.joining("\n"));
                }
                addCell(dataTable, categories, normalFont, rowColor, Element.ALIGN_LEFT);
                addCell(dataTable, quantities, normalFont, rowColor, Element.ALIGN_RIGHT);

                String statusStr = (req.getStatus() != null) ? req.getStatus().name() : "-";
                addCell(dataTable, statusStr, normalFont, rowColor, Element.ALIGN_CENTER);

                rowIndex++;
            }

            document.add(dataTable);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            document.close();
        }
    }

    private void addStatsRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(PdfPCell.BOTTOM);
        labelCell.setBorderColor(new Color(220, 220, 220));
        labelCell.setPadding(5);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(PdfPCell.BOTTOM);
        valueCell.setBorderColor(new Color(220, 220, 220));
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addCell(PdfPTable table, String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(new Color(220, 220, 220));
        table.addCell(cell);
    }

    private double calculateTotalWaste(List<PickupRequest> requests) {
        return requests.stream()
                .filter(r -> r.getItems() != null)
                .flatMap(r -> r.getItems().stream())
                .filter(item -> item.getQuantity() != null)
                .mapToDouble(item -> item.getQuantity())
                .sum();
    }
}
