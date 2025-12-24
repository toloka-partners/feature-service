package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sivalabs.ft.features.TestcontainersConfiguration;
import com.sivalabs.ft.features.domain.models.GroupBy;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Sql(scripts = {"/test-data.sql"})
class ReportingServiceTest {

    @Autowired
    private ReportingService reportingService;

    @Test
    void testExportRoadmapToCsv() {
        ReportingService.ExportResult result =
                reportingService.exportRoadmap("CSV", null, null, null, null, null, null, null);

        assertThat(result).as("Export result should not be null").isNotNull();
        assertThat(result.fileName()).as("File name should end with .csv").endsWith(".csv");
        assertThat(result.fileName()).as("File name should start with Roadmap_").startsWith("Roadmap_");
        assertThat(result.contentType()).as("Content type should be text/csv").isEqualTo("text/csv");
        assertThat(result.content()).as("CSV content should not be empty").isNotEmpty();

        String csvContent = new String(result.content());
        assertThat(csvContent).as("CSV should contain header").contains("Product Code", "Release Code", "Feature Code");
    }

    @Test
    void testExportRoadmapToPdf() {
        ReportingService.ExportResult result =
                reportingService.exportRoadmap("PDF", null, null, null, null, null, null, null);

        assertThat(result).as("Export result should not be null").isNotNull();
        assertThat(result.fileName()).as("File name should end with .pdf").endsWith(".pdf");
        assertThat(result.fileName()).as("File name should start with Roadmap_").startsWith("Roadmap_");
        assertThat(result.contentType())
                .as("Content type should be application/pdf")
                .isEqualTo("application/pdf");
        assertThat(result.content()).as("PDF content should not be empty").isNotEmpty();

        String pdfHeader = new String(result.content(), 0, Math.min(result.content().length, 10));
        assertThat(pdfHeader).as("PDF content should start with PDF signature").startsWith("%PDF");
    }

    @Test
    void testExportRoadmapWithInvalidFormat() {
        assertThatThrownBy(() -> reportingService.exportRoadmap("INVALID", null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported format");
    }

    @Test
    void testExportRoadmapByProductCode() {
        ReportingService.ExportResult result =
                reportingService.exportRoadmap("CSV", "intellij", null, null, null, null, null, null);

        assertThat(result).as("Export result should not be null").isNotNull();
        assertThat(result.content()).as("CSV content should not be empty").isNotEmpty();

        String csvContent = new String(result.content());
        assertThat(csvContent).as("CSV should contain IntelliJ product data").contains("intellij", "IntelliJ IDEA");
    }

    @Test
    void testExportRoadmapByMultipleProductCodes() {
        List<String> productCodes = List.of("intellij", "goland");
        ReportingService.ExportResult result =
                reportingService.exportRoadmap("CSV", null, productCodes, null, null, null, null, null);

        assertThat(result).as("Export result should not be null").isNotNull();
        assertThat(result.content()).as("CSV content should not be empty").isNotEmpty();

        String csvContent = new String(result.content());
        assertThat(csvContent).as("CSV should contain both products").contains("intellij", "goland");
    }

    @Test
    void testExportRoadmapWithDateRange() {
        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);

        ReportingService.ExportResult result =
                reportingService.exportRoadmap("CSV", null, null, startDate, endDate, null, null, null);

        assertThat(result).as("Export result should not be null").isNotNull();
        assertThat(result.content()).as("CSV content should not be empty").isNotEmpty();
    }

    @Test
    void testExportRoadmapIncludeCompleted() {
        ReportingService.ExportResult result =
                reportingService.exportRoadmap("CSV", null, null, null, null, true, null, null);

        assertThat(result).as("Export result should not be null").isNotNull();
        assertThat(result.content()).as("CSV content should not be empty").isNotEmpty();

        String csvContent = new String(result.content());
        assertThat(csvContent).as("CSV should contain RELEASED status").contains("RELEASED");
    }

    @Test
    void testExportRoadmapExcludeCompleted() {
        ReportingService.ExportResult result =
                reportingService.exportRoadmap("CSV", null, null, null, null, false, null, null);

        assertThat(result).as("Export result should not be null").isNotNull();
        assertThat(result.content()).as("CSV content should not be empty").isNotEmpty();
    }

    @Test
    void testExportRoadmapWithGroupBy() {
        ReportingService.ExportResult result =
                reportingService.exportRoadmap("CSV", null, null, null, null, null, GroupBy.PRODUCT, null);

        assertThat(result).as("Export result should not be null").isNotNull();
        assertThat(result.content()).as("CSV content should not be empty").isNotEmpty();
    }

    @Test
    void testCsvExportFileNameFormat() {
        ReportingService.ExportResult result =
                reportingService.exportRoadmap("CSV", null, null, null, null, null, null, null);

        assertThat(result.fileName())
                .as("File name should match pattern Roadmap_yyyyMMddHHmmss.csv")
                .matches("Roadmap_\\d{14}\\.csv");
    }

    @Test
    void testPdfExportFileNameFormat() {
        ReportingService.ExportResult result =
                reportingService.exportRoadmap("PDF", null, null, null, null, null, null, null);

        assertThat(result.fileName())
                .as("File name should match pattern Roadmap_yyyyMMddHHmmss.pdf")
                .matches("Roadmap_\\d{14}\\.pdf");
    }

    @Test
    void testCsvExportContainsAllRequiredColumns() {
        ReportingService.ExportResult result =
                reportingService.exportRoadmap("CSV", "intellij", null, null, null, null, null, null);

        String csvContent = new String(result.content());
        String[] requiredColumns = {
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
            "Created At"
        };

        for (String column : requiredColumns) {
            assertThat(csvContent).as("CSV should contain column: " + column).contains(column);
        }
    }

    @Test
    void testExportForNonExistentProduct() {
        ReportingService.ExportResult result =
                reportingService.exportRoadmap("CSV", "non-existent", null, null, null, null, null, null);

        assertThat(result).as("Export result should not be null").isNotNull();
        assertThat(result.content())
                .as("CSV content should not be empty (at least header)")
                .isNotEmpty();

        String csvContent = new String(result.content());
        String[] lines = csvContent.split("\n");
        assertThat(lines.length)
                .as("CSV should only contain header for non-existent product")
                .isEqualTo(1);
    }

    @Test
    void testCsvAndPdfExportConsistency() {
        ReportingService.ExportResult csvResult =
                reportingService.exportRoadmap("CSV", "intellij", null, null, null, null, null, null);
        ReportingService.ExportResult pdfResult =
                reportingService.exportRoadmap("PDF", "intellij", null, null, null, null, null, null);

        assertThat(csvResult.content()).as("CSV result should not be empty").isNotEmpty();
        assertThat(pdfResult.content()).as("PDF result should not be empty").isNotEmpty();

        String csvFilenameTimestamp =
                csvResult.fileName().replace("Roadmap_", "").replace(".csv", "");
        String pdfFilenameTimestamp =
                pdfResult.fileName().replace("Roadmap_", "").replace(".pdf", "");

        assertThat(csvFilenameTimestamp)
                .as("CSV and PDF filenames should have similar timestamps")
                .hasSize(14);
        assertThat(pdfFilenameTimestamp)
                .as("CSV and PDF filenames should have similar timestamps")
                .hasSize(14);
    }
}
