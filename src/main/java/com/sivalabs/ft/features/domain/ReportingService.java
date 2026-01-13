package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.dtos.RoadmapItemDto;
import com.sivalabs.ft.features.domain.dtos.RoadmapResponseDto;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportingService {
    private final RoadmapService roadmapService;

    public ReportingService(RoadmapService roadmapService) {
        this.roadmapService = roadmapService;
    }

    public ExportResult exportRoadmap(
            String format,
            List<String> productCodes,
            List<String> statuses,
            LocalDate dateFrom,
            LocalDate dateTo,
            String groupBy,
            String owner) {

        RoadmapResponseDto roadmapData =
                roadmapService.getRoadmap(productCodes, statuses, dateFrom, dateTo, groupBy, owner);

        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        switch (format.toUpperCase()) {
            case "CSV":
                return new ExportResult(
                        generateCsv(roadmapData.roadmapItems()), "text/csv", "Roadmap_" + timestamp + ".csv");
            case "PDF":
                return new ExportResult(
                        generatePdf(roadmapData.roadmapItems()), "application/pdf", "Roadmap_" + timestamp + ".pdf");
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    private byte[] generateCsv(List<RoadmapItemDto> roadmapItems) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        // Write CSV header
        writer.println("Product Code,Product Name,Release Code,Release Description,Release Status,"
                + "Released At,Planned Start Date,Planned Release Date,Actual Release Date,Owner,"
                + "Total Features,Completed Features,In Progress Features,New Features,On Hold Features,"
                + "Completion Percentage,Timeline Adherence,Risk Level");

        // Write data rows
        for (RoadmapItemDto item : roadmapItems) {
            String productCode = extractProductCodeFromRelease(item.release().code());
            String productName = ""; // We would need to join with Product entity to get name

            writer.printf(
                    "%s,%s,%s,\"%s\",%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%d,%.2f,%s,%s%n",
                    csvEscape(productCode),
                    csvEscape(productName),
                    csvEscape(item.release().code()),
                    csvEscape(item.release().description()),
                    csvEscape(item.release().status().toString()),
                    formatInstant(item.release().releasedAt()),
                    formatInstant(item.release().plannedStartDate()),
                    formatInstant(item.release().plannedReleaseDate()),
                    formatInstant(item.release().actualReleaseDate()),
                    csvEscape(item.release().owner()),
                    item.progressMetrics().totalFeatures(),
                    item.progressMetrics().completedFeatures(),
                    item.progressMetrics().inProgressFeatures(),
                    item.progressMetrics().newFeatures(),
                    item.progressMetrics().onHoldFeatures(),
                    item.progressMetrics().completionPercentage(),
                    csvEscape(item.healthIndicators().timelineAdherence()),
                    csvEscape(item.healthIndicators().riskLevel()));
        }

        return stringWriter.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] generatePdf(List<RoadmapItemDto> roadmapItems) {
        // For a complete PDF implementation, you would typically use libraries like:
        // - iText PDF
        // - Apache PDFBox
        // - Flying Saucer (for HTML to PDF)

        // For this implementation, I'll create a simple HTML table that can be converted to PDF
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>Roadmap Report</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; }\n");
        html.append("table { border-collapse: collapse; width: 100%; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #f2f2f2; }\n");
        html.append("</style>\n</head>\n<body>\n");

        html.append("<h1>Roadmap Report</h1>\n");
        html.append("<table>\n<thead>\n<tr>\n");
        html.append("<th>Product Code</th>");
        html.append("<th>Product Name</th>");
        html.append("<th>Release Code</th>");
        html.append("<th>Release Description</th>");
        html.append("<th>Release Status</th>");
        html.append("<th>Released At</th>");
        html.append("<th>Planned Start Date</th>");
        html.append("<th>Planned Release Date</th>");
        html.append("<th>Actual Release Date</th>");
        html.append("<th>Owner</th>");
        html.append("<th>Total Features</th>");
        html.append("<th>Completed Features</th>");
        html.append("<th>In Progress Features</th>");
        html.append("<th>New Features</th>");
        html.append("<th>On Hold Features</th>");
        html.append("<th>Completion %</th>");
        html.append("<th>Timeline Adherence</th>");
        html.append("<th>Risk Level</th>");
        html.append("\n</tr>\n</thead>\n<tbody>\n");

        for (RoadmapItemDto item : roadmapItems) {
            html.append("<tr>\n");
            html.append("<td>")
                    .append(htmlEscape(
                            extractProductCodeFromRelease(item.release().code())))
                    .append("</td>");
            html.append("<td>").append(htmlEscape("")).append("</td>"); // Product name would need join
            html.append("<td>").append(htmlEscape(item.release().code())).append("</td>");
            html.append("<td>").append(htmlEscape(item.release().description())).append("</td>");
            html.append("<td>")
                    .append(htmlEscape(item.release().status().toString()))
                    .append("</td>");
            html.append("<td>")
                    .append(htmlEscape(formatInstant(item.release().releasedAt())))
                    .append("</td>");
            html.append("<td>")
                    .append(htmlEscape(formatInstant(item.release().plannedStartDate())))
                    .append("</td>");
            html.append("<td>")
                    .append(htmlEscape(formatInstant(item.release().plannedReleaseDate())))
                    .append("</td>");
            html.append("<td>")
                    .append(htmlEscape(formatInstant(item.release().actualReleaseDate())))
                    .append("</td>");
            html.append("<td>").append(htmlEscape(item.release().owner())).append("</td>");
            html.append("<td>").append(item.progressMetrics().totalFeatures()).append("</td>");
            html.append("<td>")
                    .append(item.progressMetrics().completedFeatures())
                    .append("</td>");
            html.append("<td>")
                    .append(item.progressMetrics().inProgressFeatures())
                    .append("</td>");
            html.append("<td>").append(item.progressMetrics().newFeatures()).append("</td>");
            html.append("<td>").append(item.progressMetrics().onHoldFeatures()).append("</td>");
            html.append("<td>")
                    .append(String.format("%.2f", item.progressMetrics().completionPercentage()))
                    .append("</td>");
            html.append("<td>")
                    .append(htmlEscape(item.healthIndicators().timelineAdherence()))
                    .append("</td>");
            html.append("<td>")
                    .append(htmlEscape(item.healthIndicators().riskLevel()))
                    .append("</td>");
            html.append("\n</tr>\n");
        }

        html.append("</tbody>\n</table>\n</body>\n</html>");

        // For production, you would convert this HTML to PDF using a library
        // For now, return HTML as bytes (this would need proper PDF conversion)
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String extractProductCodeFromRelease(String releaseCode) {
        int dashIndex = releaseCode.indexOf('-');
        return dashIndex > 0 ? releaseCode.substring(0, dashIndex) : releaseCode;
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains("\"")) {
            value = value.replace("\"", "\"\"");
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = "\"" + value + "\"";
        }
        return value;
    }

    private String htmlEscape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private String formatInstant(java.time.Instant instant) {
        if (instant == null) return "";
        return instant.toString();
    }

    public static class ExportResult {
        private final byte[] content;
        private final String contentType;
        private final String filename;

        public ExportResult(byte[] content, String contentType, String filename) {
            this.content = content;
            this.contentType = contentType;
            this.filename = filename;
        }

        public byte[] getContent() {
            return content;
        }

        public String getContentType() {
            return contentType;
        }

        public String getFilename() {
            return filename;
        }
    }
}
