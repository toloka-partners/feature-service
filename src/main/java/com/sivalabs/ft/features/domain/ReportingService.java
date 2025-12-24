package com.sivalabs.ft.features.domain;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sivalabs.ft.features.domain.dtos.FeatureDto;
import com.sivalabs.ft.features.domain.dtos.RoadmapItemDto;
import com.sivalabs.ft.features.domain.dtos.RoadmapResponseDto;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.models.GroupBy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingService {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RoadmapService roadmapService;
    private final ProductRepository productRepository;

    ReportingService(RoadmapService roadmapService, ProductRepository productRepository) {
        this.roadmapService = roadmapService;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public ExportResult exportRoadmap(
            String format,
            String productCode,
            List<String> productCodes,
            LocalDate startDate,
            LocalDate endDate,
            Boolean includeCompleted,
            GroupBy groupBy,
            String username) {
        if ("CSV".equalsIgnoreCase(format)) {
            return exportToCsv(productCode, productCodes, startDate, endDate, includeCompleted, groupBy, username);
        } else if ("PDF".equalsIgnoreCase(format)) {
            return exportToPdf(productCode, productCodes, startDate, endDate, includeCompleted, groupBy, username);
        } else {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    private ExportResult exportToCsv(
            String productCode,
            List<String> productCodes,
            LocalDate startDate,
            LocalDate endDate,
            Boolean includeCompleted,
            GroupBy groupBy,
            String username) {
        try {
            StringWriter writer = new StringWriter();
            CSVPrinter csvPrinter = new CSVPrinter(
                    writer,
                    CSVFormat.DEFAULT
                            .builder()
                            .setHeader(
                                    "Product Code",
                                    "Product Name",
                                    "Release Code",
                                    "Release Description",
                                    "Release Status",
                                    "Released At",
                                    "Total Features",
                                    "Completed Features",
                                    "In Progress Features",
                                    "New Features",
                                    "On Hold Features",
                                    "Completion Percentage",
                                    "Timeline Adherence",
                                    "Risk Level",
                                    "Blocked Features",
                                    "Feature Code",
                                    "Feature Title",
                                    "Feature Status",
                                    "Assigned To",
                                    "Created At")
                            .build());

            List<ProductRoadmapData> productRoadmapData = getProductRoadmapData(
                    productCode, productCodes, startDate, endDate, includeCompleted, groupBy, username);

            for (ProductRoadmapData data : productRoadmapData) {
                for (RoadmapItemDto item : data.roadmapItems()) {
                    if (item.features().isEmpty()) {
                        csvPrinter.printRecord(
                                data.productCode(),
                                data.productName(),
                                item.release().code(),
                                item.release().description(),
                                item.release().status(),
                                item.release().releasedAt() != null
                                        ? DATE_FORMATTER.format(
                                                item.release().releasedAt().atZone(java.time.ZoneId.systemDefault()))
                                        : "",
                                item.progressMetrics().totalFeatures(),
                                item.progressMetrics().completedFeatures(),
                                item.progressMetrics().inProgressFeatures(),
                                item.progressMetrics().newFeatures(),
                                item.progressMetrics().onHoldFeatures(),
                                String.format("%.2f", item.progressMetrics().completionPercentage()),
                                item.healthIndicators().timelineAdherence(),
                                item.healthIndicators().riskLevel(),
                                item.healthIndicators().blockedFeatures(),
                                "",
                                "",
                                "",
                                "",
                                "");
                    } else {
                        for (FeatureDto feature : item.features()) {
                            csvPrinter.printRecord(
                                    data.productCode(),
                                    data.productName(),
                                    item.release().code(),
                                    item.release().description(),
                                    item.release().status(),
                                    item.release().releasedAt() != null
                                            ? DATE_FORMATTER.format(item.release()
                                                    .releasedAt()
                                                    .atZone(java.time.ZoneId.systemDefault()))
                                            : "",
                                    item.progressMetrics().totalFeatures(),
                                    item.progressMetrics().completedFeatures(),
                                    item.progressMetrics().inProgressFeatures(),
                                    item.progressMetrics().newFeatures(),
                                    item.progressMetrics().onHoldFeatures(),
                                    String.format("%.2f", item.progressMetrics().completionPercentage()),
                                    item.healthIndicators().timelineAdherence(),
                                    item.healthIndicators().riskLevel(),
                                    item.healthIndicators().blockedFeatures(),
                                    feature.code(),
                                    feature.title(),
                                    feature.status(),
                                    feature.assignedTo() != null ? feature.assignedTo() : "",
                                    feature.createdAt() != null
                                            ? DATE_FORMATTER.format(
                                                    feature.createdAt().atZone(java.time.ZoneId.systemDefault()))
                                            : "");
                        }
                    }
                }
            }

            csvPrinter.flush();
            String timestamp = java.time.LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String fileName = "Roadmap_" + timestamp + ".csv";
            return new ExportResult(fileName, writer.toString().getBytes(), "text/csv");
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }

    private ExportResult exportToPdf(
            String productCode,
            List<String> productCodes,
            LocalDate startDate,
            LocalDate endDate,
            Boolean includeCompleted,
            GroupBy groupBy,
            String username) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            Paragraph title = new Paragraph("Product Roadmap Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            List<ProductRoadmapData> productRoadmapData = getProductRoadmapData(
                    productCode, productCodes, startDate, endDate, includeCompleted, groupBy, username);

            for (ProductRoadmapData data : productRoadmapData) {
                Paragraph productHeader =
                        new Paragraph(data.productName() + " (" + data.productCode() + ")", headerFont);
                productHeader.setSpacingBefore(10);
                productHeader.setSpacingAfter(10);
                document.add(productHeader);

                PdfPTable table = new PdfPTable(10);
                table.setWidthPercentage(100);
                table.setWidths(new float[] {2f, 2f, 1f, 1f, 1f, 1f, 1.5f, 1f, 2f, 1f});

                addTableHeader(table, headerFont);

                for (RoadmapItemDto item : data.roadmapItems()) {
                    if (item.features().isEmpty()) {
                        addReleaseRow(table, item, null, cellFont);
                    } else {
                        for (FeatureDto feature : item.features()) {
                            addReleaseRow(table, item, feature, cellFont);
                        }
                    }
                }

                document.add(table);
            }

            document.close();

            String timestamp = java.time.LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String fileName = "Roadmap_" + timestamp + ".pdf";
            return new ExportResult(fileName, baos.toByteArray(), "application/pdf");
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void addTableHeader(PdfPTable table, Font headerFont) {
        String[] headers = {
            "Release Code",
            "Release Status",
            "Total",
            "Completed",
            "In Progress",
            "Completion %",
            "Timeline",
            "Risk",
            "Feature Code",
            "Feature Status"
        };

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new java.awt.Color(200, 200, 200));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addReleaseRow(PdfPTable table, RoadmapItemDto item, FeatureDto feature, Font cellFont) {
        table.addCell(new Phrase(item.release().code(), cellFont));
        table.addCell(new Phrase(item.release().status().toString(), cellFont));
        table.addCell(new Phrase(item.progressMetrics().totalFeatures().toString(), cellFont));
        table.addCell(new Phrase(item.progressMetrics().completedFeatures().toString(), cellFont));
        table.addCell(new Phrase(item.progressMetrics().inProgressFeatures().toString(), cellFont));
        table.addCell(new Phrase(String.format("%.1f", item.progressMetrics().completionPercentage()), cellFont));
        table.addCell(new Phrase(item.healthIndicators().timelineAdherence().toString(), cellFont));
        table.addCell(new Phrase(item.healthIndicators().riskLevel().toString(), cellFont));
        table.addCell(new Phrase(feature != null ? feature.code() : "", cellFont));
        table.addCell(new Phrase(feature != null ? feature.status().toString() : "", cellFont));
    }

    private List<ProductRoadmapData> getProductRoadmapData(
            String productCode,
            List<String> productCodes,
            LocalDate startDate,
            LocalDate endDate,
            Boolean includeCompleted,
            GroupBy groupBy,
            String username) {
        if (productCodes != null && !productCodes.isEmpty()) {
            return productCodes.stream()
                    .map(code -> {
                        Product product = productRepository.findByCode(code).orElse(null);
                        if (product == null) {
                            return null;
                        }
                        RoadmapResponseDto roadmap = roadmapService.getRoadmap(
                                code, startDate, endDate, includeCompleted, groupBy, null, username);
                        return new ProductRoadmapData(product.getCode(), product.getName(), roadmap.roadmapItems());
                    })
                    .filter(data -> data != null)
                    .toList();
        } else if (productCode != null) {
            Product product = productRepository.findByCode(productCode).orElse(null);
            if (product == null) {
                return List.of();
            }
            RoadmapResponseDto roadmap = roadmapService.getRoadmap(
                    productCode, startDate, endDate, includeCompleted, groupBy, null, username);
            return List.of(new ProductRoadmapData(product.getCode(), product.getName(), roadmap.roadmapItems()));
        } else {
            List<Product> allProducts = productRepository.findAll();
            return allProducts.stream()
                    .map(product -> {
                        RoadmapResponseDto roadmap = roadmapService.getRoadmap(
                                product.getCode(), startDate, endDate, includeCompleted, groupBy, null, username);
                        return new ProductRoadmapData(product.getCode(), product.getName(), roadmap.roadmapItems());
                    })
                    .toList();
        }
    }

    public record ExportResult(String fileName, byte[] content, String contentType) {}

    private record ProductRoadmapData(String productCode, String productName, List<RoadmapItemDto> roadmapItems) {}
}
