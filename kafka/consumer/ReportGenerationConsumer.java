package com.company.compliance.kafka.consumer;

import com.company.compliance.config.AppProperties;
import com.company.compliance.domain.entity.Report;
import com.company.compliance.event.ReportRequestEvent;
import com.company.compliance.exception.ResourceNotFoundException;
import com.company.compliance.repository.ComplianceViolationRepository;
import com.company.compliance.repository.ReportRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Kafka consumer that generates compliance reports asynchronously.
 *
 * <p>Workflow per message:
 * <ol>
 *   <li>Load the Report entity and mark it GENERATING</li>
 *   <li>Gather data (violations, risk scores) for the reporting period</li>
 *   <li>Generate the file (PDF / Excel / CSV / JSON)</li>
 *   <li>Save to storage path and mark the Report COMPLETED</li>
 *   <li>On any failure: mark the Report FAILED with the error message</li>
 * </ol>
 *
 * <p>File: {@code src/main/java/com/company/compliance/kafka/consumer/ReportGenerationConsumer.java}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportGenerationConsumer {

    private final ReportRepository              reportRepository;
    private final ComplianceViolationRepository violationRepository;
    private final AppProperties                 appProperties;
    private final MeterRegistry                 meterRegistry;

    @KafkaListener(
            topics           = "#{@appProperties.kafka.reportRequests}",
            groupId          = "compliance-report-consumer",
            concurrency      = "2",              // limited concurrency — reports are CPU-heavy
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, ReportRequestEvent> record, Acknowledgment ack) {
        ReportRequestEvent event = record.value();
        log.info("Starting report generation: reportId={} template={} format={}",
                event.getReportId(), event.getTemplateId(), event.getFormat());

        Timer.Sample timerSample = Timer.start(meterRegistry);

        Report report = reportRepository.findByIdAndOrganizationId(
                        event.getReportId(), event.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Report", event.getReportId()));

        try {
            report.markStarted();
            reportRepository.save(report);

            // Ensure storage directory exists
            Path storageDir = Paths.get(appProperties.getReports().getStoragePath());
            Files.createDirectories(storageDir);

            // Generate file
            String filePath = generateReport(event, storageDir);
            long   fileSize = new File(filePath).length();

            // Build summary stats
            Map<String, Object> summary = buildSummary(event);

            report.markCompleted(filePath, fileSize, summary);
            reportRepository.save(report);

            timerSample.stop(Timer.builder("report.generation.duration")
                    .tag("template", event.getTemplateId())
                    .tag("format",   event.getFormat())
                    .register(meterRegistry));

            meterRegistry.counter("reports.generated",
                    "template", event.getTemplateId(),
                    "format",   event.getFormat()).increment();

            log.info("Report generation completed: reportId={} size={}B path={}",
                    event.getReportId(), fileSize, filePath);
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Report generation failed: reportId={} error={}",
                    event.getReportId(), ex.getMessage(), ex);

            report.markFailed(ex.getMessage());
            reportRepository.save(report);

            meterRegistry.counter("reports.generation.failed",
                    "template", event.getTemplateId()).increment();

            // Acknowledge to prevent infinite retry — the Report row is already FAILED
            ack.acknowledge();
        }
    }

    // ── DLT handler ───────────────────────────────────────────────

    @KafkaListener(
            topics  = "#{@appProperties.kafka.reportRequests}.DLT",
            groupId = "compliance-report-dlt-consumer"
    )
    public void consumeDlt(ConsumerRecord<String, ReportRequestEvent> record, Acknowledgment ack) {
        ReportRequestEvent event = record.value();
        log.error("[DLT] Report generation event unprocessable: reportId={}", event.getReportId());
        meterRegistry.counter("reports.dlt").increment();
        ack.acknowledge();
    }

    // ── Report generation routing ─────────────────────────────────

    private String generateReport(ReportRequestEvent event, Path storageDir) throws Exception {
        String fileName = event.getTemplateId()
                + "_" + event.getPeriodStart()
                + "_" + event.getPeriodEnd()
                + "_" + event.getReportId()
                + "." + extension(event.getFormat());

        String filePath = storageDir.resolve(fileName).toString();

        switch (event.getFormat().toUpperCase()) {
            case "PDF"   -> generatePdf(event, filePath);
            case "EXCEL" -> generateExcel(event, filePath);
            case "CSV"   -> generateCsv(event, filePath);
            case "JSON"  -> generateJson(event, filePath);
            default      -> throw new IllegalArgumentException(
                    "Unsupported format: " + event.getFormat());
        }

        return filePath;
    }

    // ── PDF generation (iText 8) ──────────────────────────────────

    private void generatePdf(ReportRequestEvent event, String filePath) throws Exception {
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(filePath));
             Document document = new Document(pdf)) {

            document.add(new Paragraph("Compliance Report")
                    .setFontSize(20).setBold());
            document.add(new Paragraph("Template: " + event.getTemplateId()));
            document.add(new Paragraph("Organisation: " + event.getOrganizationId()));
            document.add(new Paragraph("Reporting Period: "
                    + event.getPeriodStart() + " – " + event.getPeriodEnd()));
            document.add(new Paragraph("Generated: " + OffsetDateTime.now()));
            document.add(new Paragraph("\n--- Violations Summary ---\n"));

            // Fetch open violations for the period
            var violations = violationRepository
                    .findOpenByOrganization(event.getOrganizationId());
            document.add(new Paragraph("Total Open Violations: " + violations.size()));

            violations.forEach(v -> document.add(
                    new Paragraph("• [" + v.getSeverity() + "] "
                            + v.getTitle() + " — " + v.getDetectedAt())));
        }
    }

    // ── Excel generation (Apache POI) ─────────────────────────────

    private void generateExcel(ReportRequestEvent event, String filePath) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath)) {

            Sheet sheet = workbook.createSheet("Violations");

            // Header row
            Row header = sheet.createRow(0);
            String[] columns = {"ID","Policy","Severity","Status","Title","Detected At"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // Data rows
            var violations = violationRepository.findOpenByOrganization(event.getOrganizationId());
            int rowNum = 1;
            for (var v : violations) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(v.getId().toString());
                row.createCell(1).setCellValue(v.getPolicy().getName());
                row.createCell(2).setCellValue(v.getSeverity().getValue());
                row.createCell(3).setCellValue(v.getStatus().getValue());
                row.createCell(4).setCellValue(v.getTitle());
                row.createCell(5).setCellValue(v.getDetectedAt().toString());
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

            workbook.write(fos);
        }
    }

    // ── CSV generation ────────────────────────────────────────────

    private void generateCsv(ReportRequestEvent event, String filePath) throws Exception {
        var violations = violationRepository.findOpenByOrganization(event.getOrganizationId());
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Policy,Severity,Status,Title,DetectedAt\n");
        for (var v : violations) {
            sb.append(v.getId()).append(",")
              .append(escapeCsv(v.getPolicy().getName())).append(",")
              .append(v.getSeverity().getValue()).append(",")
              .append(v.getStatus().getValue()).append(",")
              .append(escapeCsv(v.getTitle())).append(",")
              .append(v.getDetectedAt()).append("\n");
        }
        Files.writeString(Paths.get(filePath), sb.toString());
    }

    // ── JSON generation ───────────────────────────────────────────

    private void generateJson(ReportRequestEvent event, String filePath) throws Exception {
        var violations = violationRepository.findOpenByOrganization(event.getOrganizationId());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var v : violations) {
            rows.add(Map.of(
                    "id",         v.getId().toString(),
                    "policy",     v.getPolicy().getName(),
                    "severity",   v.getSeverity().getValue(),
                    "status",     v.getStatus().getValue(),
                    "title",      v.getTitle(),
                    "detectedAt", v.getDetectedAt().toString()
            ));
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportId",       event.getReportId());
        report.put("template",       event.getTemplateId());
        report.put("organizationId", event.getOrganizationId());
        report.put("periodStart",    event.getPeriodStart());
        report.put("periodEnd",      event.getPeriodEnd());
        report.put("generatedAt",    OffsetDateTime.now().toString());
        report.put("violations",     rows);

        // Simple JSON serialisation (ObjectMapper injected in production)
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        Files.writeString(Paths.get(filePath),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Map<String, Object> buildSummary(ReportRequestEvent event) {
        var violations = violationRepository.findOpenByOrganization(event.getOrganizationId());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalOpenViolations", violations.size());
        summary.put("critical", violations.stream()
                .filter(v -> "CRITICAL".equals(v.getSeverity().getValue())).count());
        summary.put("high", violations.stream()
                .filter(v -> "HIGH".equals(v.getSeverity().getValue())).count());
        summary.put("periodStart", event.getPeriodStart().toString());
        summary.put("periodEnd",   event.getPeriodEnd().toString());
        return summary;
    }

    private String extension(String format) {
        return switch (format.toUpperCase()) {
            case "PDF"   -> "pdf";
            case "EXCEL" -> "xlsx";
            case "CSV"   -> "csv";
            case "JSON"  -> "json";
            default      -> "bin";
        };
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
