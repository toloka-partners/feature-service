package com.sivalabs.ft.features.domain.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FeaturePlanningStatusTest {

    @Test
    void shouldAllowTransitionFromNotStartedToInProgress() {
        assertThat(FeaturePlanningStatus.NOT_STARTED.canTransitionTo(FeaturePlanningStatus.IN_PROGRESS))
                .isTrue();
    }

    @Test
    void shouldNotAllowTransitionFromNotStartedToBlocked() {
        assertThat(FeaturePlanningStatus.NOT_STARTED.canTransitionTo(FeaturePlanningStatus.BLOCKED))
                .isFalse();
    }

    @Test
    void shouldNotAllowTransitionFromNotStartedToDone() {
        assertThat(FeaturePlanningStatus.NOT_STARTED.canTransitionTo(FeaturePlanningStatus.DONE))
                .isFalse();
    }

    @Test
    void shouldAllowTransitionFromInProgressToBlocked() {
        assertThat(FeaturePlanningStatus.IN_PROGRESS.canTransitionTo(FeaturePlanningStatus.BLOCKED))
                .isTrue();
    }

    @Test
    void shouldAllowTransitionFromInProgressToDone() {
        assertThat(FeaturePlanningStatus.IN_PROGRESS.canTransitionTo(FeaturePlanningStatus.DONE))
                .isTrue();
    }

    @Test
    void shouldAllowTransitionFromInProgressToNotStarted() {
        assertThat(FeaturePlanningStatus.IN_PROGRESS.canTransitionTo(FeaturePlanningStatus.NOT_STARTED))
                .isTrue();
    }

    @Test
    void shouldAllowTransitionFromBlockedToInProgress() {
        assertThat(FeaturePlanningStatus.BLOCKED.canTransitionTo(FeaturePlanningStatus.IN_PROGRESS))
                .isTrue();
    }

    @Test
    void shouldAllowTransitionFromBlockedToNotStarted() {
        assertThat(FeaturePlanningStatus.BLOCKED.canTransitionTo(FeaturePlanningStatus.NOT_STARTED))
                .isTrue();
    }

    @Test
    void shouldNotAllowTransitionFromBlockedToDone() {
        assertThat(FeaturePlanningStatus.BLOCKED.canTransitionTo(FeaturePlanningStatus.DONE))
                .isFalse();
    }

    @Test
    void shouldAllowTransitionFromDoneToInProgress() {
        assertThat(FeaturePlanningStatus.DONE.canTransitionTo(FeaturePlanningStatus.IN_PROGRESS))
                .isTrue();
    }

    @Test
    void shouldNotAllowTransitionFromDoneToNotStarted() {
        assertThat(FeaturePlanningStatus.DONE.canTransitionTo(FeaturePlanningStatus.NOT_STARTED))
                .isFalse();
    }

    @Test
    void shouldNotAllowTransitionFromDoneToBlocked() {
        assertThat(FeaturePlanningStatus.DONE.canTransitionTo(FeaturePlanningStatus.BLOCKED))
                .isFalse();
    }

    @ParameterizedTest
    @EnumSource(FeaturePlanningStatus.class)
    void shouldAllowTransitionToSameStatus(FeaturePlanningStatus status) {
        assertThat(status.canTransitionTo(status)).isTrue();
    }

    @Test
    void shouldGetValidTransitionsForNotStarted() {
        var validTransitions = FeaturePlanningStatus.NOT_STARTED.getValidTransitions();
        assertThat(validTransitions)
                .containsExactlyInAnyOrder(FeaturePlanningStatus.NOT_STARTED, FeaturePlanningStatus.IN_PROGRESS);
    }

    @Test
    void shouldGetValidTransitionsForInProgress() {
        var validTransitions = FeaturePlanningStatus.IN_PROGRESS.getValidTransitions();
        assertThat(validTransitions)
                .containsExactlyInAnyOrder(
                        FeaturePlanningStatus.NOT_STARTED,
                        FeaturePlanningStatus.IN_PROGRESS,
                        FeaturePlanningStatus.BLOCKED,
                        FeaturePlanningStatus.DONE);
    }

    @Test
    void shouldGetValidTransitionsForBlocked() {
        var validTransitions = FeaturePlanningStatus.BLOCKED.getValidTransitions();
        assertThat(validTransitions)
                .containsExactlyInAnyOrder(
                        FeaturePlanningStatus.NOT_STARTED,
                        FeaturePlanningStatus.IN_PROGRESS,
                        FeaturePlanningStatus.BLOCKED);
    }

    @Test
    void shouldGetValidTransitionsForDone() {
        var validTransitions = FeaturePlanningStatus.DONE.getValidTransitions();
        assertThat(validTransitions)
                .containsExactlyInAnyOrder(FeaturePlanningStatus.IN_PROGRESS, FeaturePlanningStatus.DONE);
    }

    @Test
    void shouldValidateTransitionSuccessfully() {
        // Should not throw exception for valid transitions
        FeaturePlanningStatus.NOT_STARTED.validateTransition(FeaturePlanningStatus.IN_PROGRESS);
        FeaturePlanningStatus.IN_PROGRESS.validateTransition(FeaturePlanningStatus.DONE);
        FeaturePlanningStatus.BLOCKED.validateTransition(FeaturePlanningStatus.IN_PROGRESS);
        FeaturePlanningStatus.DONE.validateTransition(FeaturePlanningStatus.IN_PROGRESS);
    }

    @Test
    void shouldThrowExceptionForInvalidTransition() {
        assertThatThrownBy(() -> FeaturePlanningStatus.NOT_STARTED.validateTransition(FeaturePlanningStatus.BLOCKED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid planning status transition from NOT_STARTED to BLOCKED");
    }

    @Test
    void shouldThrowExceptionForInvalidTransitionFromDoneToNotStarted() {
        assertThatThrownBy(() -> FeaturePlanningStatus.DONE.validateTransition(FeaturePlanningStatus.NOT_STARTED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid planning status transition from DONE to NOT_STARTED");
    }
}
