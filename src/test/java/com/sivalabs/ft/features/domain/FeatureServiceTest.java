package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sivalabs.ft.features.TestcontainersConfiguration;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.models.FeaturePlanningStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Sql(scripts = {"/test-data.sql"})
class FeatureServiceTest {

    @Autowired
    private FeatureService featureService;

    @Test
    void testValidatePlanningStatusTransition_AllowedTransitions() {
        // NOT_STARTED to IN_PROGRESS
        featureService.validatePlanningStatusTransition(
                FeaturePlanningStatus.NOT_STARTED, FeaturePlanningStatus.IN_PROGRESS);

        // NOT_STARTED to BLOCKED
        featureService.validatePlanningStatusTransition(
                FeaturePlanningStatus.NOT_STARTED, FeaturePlanningStatus.BLOCKED);

        // IN_PROGRESS to DONE
        featureService.validatePlanningStatusTransition(FeaturePlanningStatus.IN_PROGRESS, FeaturePlanningStatus.DONE);

        // IN_PROGRESS to BLOCKED
        featureService.validatePlanningStatusTransition(
                FeaturePlanningStatus.IN_PROGRESS, FeaturePlanningStatus.BLOCKED);

        // IN_PROGRESS to NOT_STARTED
        featureService.validatePlanningStatusTransition(
                FeaturePlanningStatus.IN_PROGRESS, FeaturePlanningStatus.NOT_STARTED);

        // BLOCKED to IN_PROGRESS
        featureService.validatePlanningStatusTransition(
                FeaturePlanningStatus.BLOCKED, FeaturePlanningStatus.IN_PROGRESS);

        // BLOCKED to NOT_STARTED
        featureService.validatePlanningStatusTransition(
                FeaturePlanningStatus.BLOCKED, FeaturePlanningStatus.NOT_STARTED);

        // DONE to NOT_STARTED
        featureService.validatePlanningStatusTransition(FeaturePlanningStatus.DONE, FeaturePlanningStatus.NOT_STARTED);

        // DONE to IN_PROGRESS
        featureService.validatePlanningStatusTransition(FeaturePlanningStatus.DONE, FeaturePlanningStatus.IN_PROGRESS);
    }

    @Test
    void testValidatePlanningStatusTransition_InvalidTransitions() {
        // NOT_STARTED to DONE (invalid)
        assertThatThrownBy(() -> featureService.validatePlanningStatusTransition(
                        FeaturePlanningStatus.NOT_STARTED, FeaturePlanningStatus.DONE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");

        // BLOCKED to DONE (invalid)
        assertThatThrownBy(() -> featureService.validatePlanningStatusTransition(
                        FeaturePlanningStatus.BLOCKED, FeaturePlanningStatus.DONE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");

        // DONE to BLOCKED (invalid)
        assertThatThrownBy(() -> featureService.validatePlanningStatusTransition(
                        FeaturePlanningStatus.DONE, FeaturePlanningStatus.BLOCKED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void testValidatePlanningStatusTransition_NullCurrentStatus() {
        // Should not throw exception when current status is null
        featureService.validatePlanningStatusTransition(null, FeaturePlanningStatus.IN_PROGRESS);
    }

    @Test
    void testValidatePlanningStatusTransition_SameStatus() {
        // Should not throw exception when transitioning to the same status
        featureService.validatePlanningStatusTransition(
                FeaturePlanningStatus.IN_PROGRESS, FeaturePlanningStatus.IN_PROGRESS);
    }
}
