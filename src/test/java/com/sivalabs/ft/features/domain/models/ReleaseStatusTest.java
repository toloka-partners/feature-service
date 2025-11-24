package com.sivalabs.ft.features.domain.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReleaseStatusTest {

    @Test
    void draftStatus_shouldAllowTransitionToPlanned() {
        // Given
        ReleaseStatus draft = ReleaseStatus.DRAFT;

        // When
        boolean canTransitionToPlanned = draft.canTransitionTo(ReleaseStatus.PLANNED);

        // Then
        assertThat(canTransitionToPlanned).isTrue();
    }

    @Test
    void draftStatus_shouldNotAllowTransitionToCompleted() {
        // Given
        ReleaseStatus draft = ReleaseStatus.DRAFT;

        // When
        boolean canTransitionToCompleted = draft.canTransitionTo(ReleaseStatus.COMPLETED);

        // Then
        assertThat(canTransitionToCompleted).isFalse();
    }

    @Test
    void plannedStatus_shouldAllowTransitionToInProgress() {
        // Given
        ReleaseStatus planned = ReleaseStatus.PLANNED;

        // When
        boolean canTransitionToInProgress = planned.canTransitionTo(ReleaseStatus.IN_PROGRESS);

        // Then
        assertThat(canTransitionToInProgress).isTrue();
    }

    @Test
    void plannedStatus_shouldAllowTransitionBackToDraft() {
        // Given
        ReleaseStatus planned = ReleaseStatus.PLANNED;

        // When
        boolean canTransitionToDraft = planned.canTransitionTo(ReleaseStatus.DRAFT);

        // Then
        assertThat(canTransitionToDraft).isTrue();
    }

    @Test
    void inProgressStatus_shouldAllowTransitionToCompleted() {
        // Given
        ReleaseStatus inProgress = ReleaseStatus.IN_PROGRESS;

        // When
        boolean canTransitionToCompleted = inProgress.canTransitionTo(ReleaseStatus.COMPLETED);

        // Then
        assertThat(canTransitionToCompleted).isTrue();
    }

    @Test
    void inProgressStatus_shouldAllowTransitionBackToPlanned() {
        // Given
        ReleaseStatus inProgress = ReleaseStatus.IN_PROGRESS;

        // When
        boolean canTransitionToPlanned = inProgress.canTransitionTo(ReleaseStatus.PLANNED);

        // Then
        assertThat(canTransitionToPlanned).isTrue();
    }

    @Test
    void completedStatus_shouldAllowTransitionToReleased() {
        // Given
        ReleaseStatus completed = ReleaseStatus.COMPLETED;

        // When
        boolean canTransitionToReleased = completed.canTransitionTo(ReleaseStatus.RELEASED);

        // Then
        assertThat(canTransitionToReleased).isTrue();
    }

    @Test
    void completedStatus_shouldNotAllowTransitionToInProgress() {
        // Given
        ReleaseStatus completed = ReleaseStatus.COMPLETED;

        // When
        boolean canTransitionToInProgress = completed.canTransitionTo(ReleaseStatus.IN_PROGRESS);

        // Then
        assertThat(canTransitionToInProgress).isFalse();
    }

    @Test
    void releasedStatus_shouldNotAllowAnyTransitions() {
        // Given
        ReleaseStatus released = ReleaseStatus.RELEASED;

        // When & Then
        assertThat(released.canTransitionTo(ReleaseStatus.DRAFT)).isFalse();
        assertThat(released.canTransitionTo(ReleaseStatus.PLANNED)).isFalse();
        assertThat(released.canTransitionTo(ReleaseStatus.IN_PROGRESS)).isFalse();
        assertThat(released.canTransitionTo(ReleaseStatus.COMPLETED)).isFalse();
        assertThat(released.canTransitionTo(ReleaseStatus.RELEASED)).isFalse();
    }

    @Test
    void getValidTransitions_shouldReturnCorrectTransitionsForDraft() {
        // Given
        ReleaseStatus draft = ReleaseStatus.DRAFT;

        // When
        ReleaseStatus[] validTransitions = draft.getValidTransitions();

        // Then
        assertThat(validTransitions).containsExactly(ReleaseStatus.PLANNED);
    }

    @Test
    void getValidTransitions_shouldReturnCorrectTransitionsForPlanned() {
        // Given
        ReleaseStatus planned = ReleaseStatus.PLANNED;

        // When
        ReleaseStatus[] validTransitions = planned.getValidTransitions();

        // Then
        assertThat(validTransitions).containsExactlyInAnyOrder(ReleaseStatus.IN_PROGRESS, ReleaseStatus.DRAFT);
    }

    @Test
    void getValidTransitions_shouldReturnCorrectTransitionsForInProgress() {
        // Given
        ReleaseStatus inProgress = ReleaseStatus.IN_PROGRESS;

        // When
        ReleaseStatus[] validTransitions = inProgress.getValidTransitions();

        // Then
        assertThat(validTransitions).containsExactlyInAnyOrder(ReleaseStatus.COMPLETED, ReleaseStatus.PLANNED);
    }

    @Test
    void getValidTransitions_shouldReturnCorrectTransitionsForCompleted() {
        // Given
        ReleaseStatus completed = ReleaseStatus.COMPLETED;

        // When
        ReleaseStatus[] validTransitions = completed.getValidTransitions();

        // Then
        assertThat(validTransitions).containsExactly(ReleaseStatus.RELEASED);
    }

    @Test
    void getValidTransitions_shouldReturnEmptyArrayForReleased() {
        // Given
        ReleaseStatus released = ReleaseStatus.RELEASED;

        // When
        ReleaseStatus[] validTransitions = released.getValidTransitions();

        // Then
        assertThat(validTransitions).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "DRAFT, DRAFT, false",
        "DRAFT, PLANNED, true",
        "DRAFT, IN_PROGRESS, false",
        "DRAFT, COMPLETED, false",
        "DRAFT, RELEASED, false",
        "PLANNED, DRAFT, true",
        "PLANNED, PLANNED, false",
        "PLANNED, IN_PROGRESS, true",
        "PLANNED, COMPLETED, false",
        "PLANNED, RELEASED, false",
        "IN_PROGRESS, DRAFT, false",
        "IN_PROGRESS, PLANNED, true",
        "IN_PROGRESS, IN_PROGRESS, false",
        "IN_PROGRESS, COMPLETED, true",
        "IN_PROGRESS, RELEASED, false",
        "COMPLETED, DRAFT, false",
        "COMPLETED, PLANNED, false",
        "COMPLETED, IN_PROGRESS, false",
        "COMPLETED, COMPLETED, false",
        "COMPLETED, RELEASED, true",
        "RELEASED, DRAFT, false",
        "RELEASED, PLANNED, false",
        "RELEASED, IN_PROGRESS, false",
        "RELEASED, COMPLETED, false",
        "RELEASED, RELEASED, false"
    })
    void canTransitionTo_shouldValidateAllPossibleTransitions(
            ReleaseStatus fromStatus, ReleaseStatus toStatus, boolean expected) {
        // When
        boolean canTransition = fromStatus.canTransitionTo(toStatus);

        // Then
        assertThat(canTransition).isEqualTo(expected);
    }

    @Test
    void statusTransition_shouldFollowValidWorkflow() {
        // Test a complete valid workflow
        ReleaseStatus current = ReleaseStatus.DRAFT;

        // DRAFT -> PLANNED
        assertThat(current.canTransitionTo(ReleaseStatus.PLANNED)).isTrue();
        current = ReleaseStatus.PLANNED;

        // PLANNED -> IN_PROGRESS
        assertThat(current.canTransitionTo(ReleaseStatus.IN_PROGRESS)).isTrue();
        current = ReleaseStatus.IN_PROGRESS;

        // IN_PROGRESS -> COMPLETED
        assertThat(current.canTransitionTo(ReleaseStatus.COMPLETED)).isTrue();
        current = ReleaseStatus.COMPLETED;

        // COMPLETED -> RELEASED
        assertThat(current.canTransitionTo(ReleaseStatus.RELEASED)).isTrue();
        current = ReleaseStatus.RELEASED;

        // RELEASED -> (no further transitions)
        assertThat(current.getValidTransitions()).isEmpty();
    }

    @Test
    void statusTransition_shouldAllowBackwardMovementWhenNeeded() {
        // Test backward movements that are allowed

        // PLANNED -> DRAFT (rollback)
        assertThat(ReleaseStatus.PLANNED.canTransitionTo(ReleaseStatus.DRAFT)).isTrue();

        // IN_PROGRESS -> PLANNED (rollback)
        assertThat(ReleaseStatus.IN_PROGRESS.canTransitionTo(ReleaseStatus.PLANNED))
                .isTrue();

        // But not from COMPLETED backwards
        assertThat(ReleaseStatus.COMPLETED.canTransitionTo(ReleaseStatus.IN_PROGRESS))
                .isFalse();
        assertThat(ReleaseStatus.COMPLETED.canTransitionTo(ReleaseStatus.PLANNED))
                .isFalse();
        assertThat(ReleaseStatus.COMPLETED.canTransitionTo(ReleaseStatus.DRAFT)).isFalse();
    }
}
