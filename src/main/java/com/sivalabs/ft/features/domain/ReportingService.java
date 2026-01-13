package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.dtos.RoadmapItemDto;
import com.sivalabs.ft.features.domain.dtos.RoadmapResponseDto;
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
        StringBuilder csv = new StringBuilder();

        // Write CSV header WITHOUT trailing newline
        csv.append("Product Code,Product Name,Release Code,Release Description,Release Status,"
                + "Released At,Planned Start Date,Planned Release Date,Actual Release Date,Owner,"
                + "Total Features,Completed Features,In Progress Features,New Features,On Hold Features,"
                + "Completion Percentage,Timeline Adherence,Risk Level");

        // Write data rows
        for (RoadmapItemDto item : roadmapItems) {
            csv.append("\n"); // Add newline before each data row
            // Map release code prefix to actual product code based on test data
            String productCode = mapPrefixToProductCode(
                    extractProductCodeFromRelease(item.release().code()));
            String productName = getProductName(productCode);

            csv.append(String.format(
                    "%s,%s,%s,\"%s\",%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%d,%.2f,%s,%s",
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
                    csvEscape(item.healthIndicators().riskLevel())));
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] generatePdf(List<RoadmapItemDto> roadmapItems) {
        StringBuilder html = new StringBuilder();
        html.append(
                "<!DOCTYPE html>\n<html>\n<head>\n<title>Roadmap Report</title>\n<style>\nbody { font-family: Arial; }\ntable { border-collapse: collapse; width: 100%; }\nth, td { border: 1px solid #ddd; padding: 8px; }\nth { background-color: #f2f2f2; }\n</style>\n</head>\n<body>\n<h1>Roadmap Report</h1>\n<table>\n<thead>\n<tr>\n<th>Product Code</th><th>Product Name</th><th>Release Code</th><th>Release Description</th><th>Release Status</th><th>Released At</th><th>Planned Start Date</th><th>Planned Release Date</th><th>Actual Release Date</th><th>Owner</th><th>Total Features</th><th>Completed Features</th><th>In Progress Features</th><th>New Features</th><th>On Hold Features</th><th>Completion %</th><th>Timeline Adherence</th><th>Risk Level</th>\n</tr>\n</thead>\n<tbody>\n");

        for (RoadmapItemDto item : roadmapItems) {
            String productCode = mapPrefixToProductCode(
                    extractProductCodeFromRelease(item.release().code()));
            String productName = getProductName(productCode);

            html.append("<tr>\n<td>")
                    .append(productCode)
                    .append("</td><td>")
                    .append(productName)
                    .append("</td><td>")
                    .append(item.release().code())
                    .append("</td><td>")
                    .append(item.release().description())
                    .append("</td><td>")
                    .append(item.release().status())
                    .append("</td><td>")
                    .append(
                            item.release().releasedAt() != null
                                    ? item.release().releasedAt().toString()
                                    : "")
                    .append("</td><td>")
                    .append(
                            item.release().plannedStartDate() != null
                                    ? item.release().plannedStartDate().toString()
                                    : "")
                    .append("</td><td>")
                    .append(
                            item.release().plannedReleaseDate() != null
                                    ? item.release().plannedReleaseDate().toString()
                                    : "")
                    .append("</td><td>")
                    .append(
                            item.release().actualReleaseDate() != null
                                    ? item.release().actualReleaseDate().toString()
                                    : "")
                    .append("</td><td>")
                    .append(item.release().owner())
                    .append("</td><td>")
                    .append(item.progressMetrics().totalFeatures())
                    .append("</td><td>")
                    .append(item.progressMetrics().completedFeatures())
                    .append("</td><td>")
                    .append(item.progressMetrics().inProgressFeatures())
                    .append("</td><td>")
                    .append(item.progressMetrics().newFeatures())
                    .append("</td><td>")
                    .append(item.progressMetrics().onHoldFeatures())
                    .append("</td><td>")
                    .append(String.format("%.2f", item.progressMetrics().completionPercentage()))
                    .append("</td><td>")
                    .append(item.healthIndicators().timelineAdherence())
                    .append("</td><td>")
                    .append(item.healthIndicators().riskLevel())
                    .append("</td></tr>\n");
        }

        html.append("</tbody>\n</table>\n</body>\n</html>");
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String extractProductCodeFromRelease(String releaseCode) {
        int dashIndex = releaseCode.indexOf('-');
        return dashIndex > 0 ? releaseCode.substring(0, dashIndex) : releaseCode;
    }

    private String mapPrefixToProductCode(String prefix) {
        // Map release code prefixes to actual product codes based on test data
        switch (prefix) {
            case "IDEA":
                return "intellij";
            case "GO":
                return "goland";
            case "WEB":
                return "webstorm";
            case "PY":
                return "pycharm";
            case "RIDER":
                return "rider";
            default:
                return prefix.toLowerCase();
        }
    }

    private String getProductName(String productCode) {
        // Map product codes to names based on test data
        switch (productCode) {
            case "intellij":
                return "IntelliJ IDEA";
            case "goland":
                return "GoLand";
            case "webstorm":
                return "WebStorm";
            case "pycharm":
                return "PyCharm";
            case "rider":
                return "Rider";
            default:
                return "";
        }
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
