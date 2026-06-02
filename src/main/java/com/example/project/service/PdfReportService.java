package com.example.project.service;

import com.example.project.model.User;
import java.io.OutputStream;

public interface PdfReportService {
    void generateRtPdf(User rtUser, OutputStream out);
    void generateCollectorPdf(User collectorUser, OutputStream out);
}
