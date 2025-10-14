package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.dtos.ReleaseDto;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class ReleaseControllerTests extends AbstractIT {

    @Test
    void shouldGetReleasesByProductCode() {
        var result =
                mvc.get().uri("/api/releases?productCode={code}", "intellij").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    void shouldGetReleaseByCode() {
        String code = "IDEA-2023.3.8";
        var result = mvc.get().uri("/api/releases/{code}", code).exchange();
        assertThat(result).hasStatusOk().bodyJson().convertTo(ReleaseDto.class).satisfies(dto -> {
            assertThat(dto.code()).isEqualTo(code);
        });
    }

    @Test
    void shouldReturn404WhenReleaseNotFound() {
        var result = mvc.get().uri("/api/releases/{code}", "INVALID_CODE").exchange();
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldCreateNewRelease() {
        var payload =
                """
            {
                "productCode": "intellij",
                "code": "IDEA-2025.1",
                "description": "IntelliJ IDEA 2025.1"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldCreateReleaseWithPlanningFields() {
        var payload =
                """
            {
                "productCode": "intellij",
                "code": "IDEA-2025.2",
                "description": "IntelliJ IDEA 2025.2 with planning",
                "plannedStartDate": "2025-01-15T10:00:00Z",
                "plannedReleaseDate": "2025-03-15T16:00:00Z",
                "owner": "john.doe",
                "notes": "This release includes new UI improvements"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);

        // Verify the created release includes planning fields
        var createdRelease =
                mvc.get().uri("/api/releases/{code}", "IDEA-IDEA-2025.2").exchange();
        assertThat(createdRelease)
                .hasStatusOk()
                .bodyJson()
                .convertTo(ReleaseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.code()).isEqualTo("IDEA-IDEA-2025.2");
                    assertThat(dto.description()).isEqualTo("IntelliJ IDEA 2025.2 with planning");
                    assertThat(dto.plannedStartDate()).isNotNull();
                    assertThat(dto.plannedReleaseDate()).isNotNull();
                    assertThat(dto.owner()).isEqualTo("john.doe");
                    assertThat(dto.notes()).isEqualTo("This release includes new UI improvements");
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldCreateReleaseWithNullPlanningFields() {
        var payload =
                """
            {
                "productCode": "intellij",
                "code": "IDEA-2025.3",
                "description": "IntelliJ IDEA 2025.3 without planning"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);

        // Verify the created release handles null planning fields
        var createdRelease =
                mvc.get().uri("/api/releases/{code}", "IDEA-IDEA-2025.3").exchange();
        assertThat(createdRelease)
                .hasStatusOk()
                .bodyJson()
                .convertTo(ReleaseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.plannedStartDate()).isNull();
                    assertThat(dto.plannedReleaseDate()).isNull();
                    assertThat(dto.owner()).isNull();
                    assertThat(dto.notes()).isNull();
                    assertThat(dto.actualReleaseDate()).isNull();
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldUpdateRelease() {
        var payload =
                """
            {
                "description": "Updated description",
                "status": "RELEASED",
                "releasedAt": "2023-12-01T10:00:00Z"
            }
            """;

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatusOk();

        // Verify the update
        var updatedRelease =
                mvc.get().uri("/api/releases/{code}", "IDEA-2023.3.8").exchange();
        assertThat(updatedRelease)
                .hasStatusOk()
                .bodyJson()
                .convertTo(ReleaseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.description()).isEqualTo("Updated description");
                    assertThat(dto.status()).isEqualTo(ReleaseStatus.RELEASED);
                    assertThat(dto.releasedAt()).isNotNull();
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldUpdateReleaseWithPlanningFields() {
        var payload =
                """
            {
                "description": "Updated with planning fields",
                "status": "PLANNED",
                "plannedStartDate": "2025-02-01T09:00:00Z",
                "plannedReleaseDate": "2025-04-01T17:00:00Z",
                "actualReleaseDate": "2025-04-05T18:30:00Z",
                "owner": "jane.smith",
                "notes": "Updated release notes with detailed planning information"
            }
            """;

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatusOk();

        // Verify the update includes planning fields
        var updatedRelease =
                mvc.get().uri("/api/releases/{code}", "IDEA-2023.3.8").exchange();
        assertThat(updatedRelease)
                .hasStatusOk()
                .bodyJson()
                .convertTo(ReleaseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.description()).isEqualTo("Updated with planning fields");
                    assertThat(dto.status()).isEqualTo(ReleaseStatus.PLANNED);
                    assertThat(dto.plannedStartDate()).isNotNull();
                    assertThat(dto.plannedReleaseDate()).isNotNull();
                    assertThat(dto.actualReleaseDate()).isNotNull();
                    assertThat(dto.owner()).isEqualTo("jane.smith");
                    assertThat(dto.notes()).isEqualTo("Updated release notes with detailed planning information");
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldUpdateReleaseStatusWithValidTransition() {
        // First set to DRAFT
        var draftPayload = """
            {
                "status": "DRAFT"
            }
            """;

        mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(draftPayload)
                .exchange();

        // Then transition to PLANNED (valid transition)
        var plannedPayload = """
            {
                "status": "PLANNED"
            }
            """;

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(plannedPayload)
                .exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldRejectInvalidStatusTransition() {
        // First set to DRAFT
        var draftPayload = """
            {
                "status": "DRAFT"
            }
            """;

        mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(draftPayload)
                .exchange();

        // Try to transition directly to COMPLETED (invalid transition)
        var completedPayload = """
            {
                "status": "COMPLETED"
            }
            """;

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(completedPayload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldRejectInvalidPlanningDates() {
        var payload =
                """
            {
                "productCode": "intellij",
                "code": "IDEA-INVALID-DATES",
                "description": "Release with invalid dates",
                "plannedStartDate": "2025-03-15T10:00:00Z",
                "plannedReleaseDate": "2025-01-15T16:00:00Z"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldDeleteRelease() {
        var result = mvc.delete().uri("/api/releases/{code}", "RIDER-2024.2.6").exchange();
        assertThat(result).hasStatusOk();

        // Verify deletion
        var getResult = mvc.get().uri("/api/releases/{code}", "RIDER-2024.2.6").exchange();
        assertThat(getResult).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldGetReleasesWithPlanningFieldsInResponse() {
        var result =
                mvc.get().uri("/api/releases?productCode={code}", "intellij").exchange();
        assertThat(result).hasStatusOk().bodyJson().extractingPath("$[0]").satisfies(release -> {
            // Verify that the response includes all planning fields in the structure
            assertThat(release).hasFieldOrProperty("plannedStartDate");
            assertThat(release).hasFieldOrProperty("plannedReleaseDate");
            assertThat(release).hasFieldOrProperty("actualReleaseDate");
            assertThat(release).hasFieldOrProperty("owner");
            assertThat(release).hasFieldOrProperty("notes");
        });
    }
}
