package com.sivalabs.ft.features.domain.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReleaseStatusTest {

    @Test
    void shouldAllowValidTransitionsFromDraft() {
        assertThat(ReleaseStatus.DRAFT.canTransitionTo(ReleaseStatus.PLANNED)).isTrue();
        assertThat(ReleaseStatus.DRAFT.canTransitionTo(ReleaseStatus.CANCELLED)).isTrue();

        assertThat(ReleaseStatus.DRAFT.canTransitionTo(ReleaseStatus.IN_PROGRESS))
                .isFalse();
        assertThat(ReleaseStatus.DRAFT.canTransitionTo(ReleaseStatus.COMPLETED)).isFalse();
        assertThat(ReleaseStatus.DRAFT.canTransitionTo(ReleaseStatus.DELAYED)).isFalse();
        assertThat(ReleaseStatus.DRAFT.canTransitionTo(ReleaseStatus.RELEASED)).isFalse();
    }

    @Test
    void shouldAllowValidTransitionsFromPlanned() {
        assertThat(ReleaseStatus.PLANNED.canTransitionTo(ReleaseStatus.IN_PROGRESS))
                .isTrue();
        assertThat(ReleaseStatus.PLANNED.canTransitionTo(ReleaseStatus.DELAYED)).isTrue();
        assertThat(ReleaseStatus.PLANNED.canTransitionTo(ReleaseStatus.CANCELLED))
                .isTrue();

        assertThat(ReleaseStatus.PLANNED.canTransitionTo(ReleaseStatus.DRAFT)).isFalse();
        assertThat(ReleaseStatus.PLANNED.canTransitionTo(ReleaseStatus.COMPLETED))
                .isFalse();
        assertThat(ReleaseStatus.PLANNED.canTransitionTo(ReleaseStatus.RELEASED))
                .isFalse();
    }

    @Test
    void shouldAllowValidTransitionsFromInProgress() {
        assertThat(ReleaseStatus.IN_PROGRESS.canTransitionTo(ReleaseStatus.COMPLETED))
                .isTrue();
        assertThat(ReleaseStatus.IN_PROGRESS.canTransitionTo(ReleaseStatus.DELAYED))
                .isTrue();
        assertThat(ReleaseStatus.IN_PROGRESS.canTransitionTo(ReleaseStatus.CANCELLED))
                .isTrue();

        assertThat(ReleaseStatus.IN_PROGRESS.canTransitionTo(ReleaseStatus.DRAFT))
                .isFalse();
        assertThat(ReleaseStatus.IN_PROGRESS.canTransitionTo(ReleaseStatus.PLANNED))
                .isFalse();
        assertThat(ReleaseStatus.IN_PROGRESS.canTransitionTo(ReleaseStatus.RELEASED))
                .isFalse();
    }

    @Test
    void shouldAllowValidTransitionsFromCompleted() {
        assertThat(ReleaseStatus.COMPLETED.canTransitionTo(ReleaseStatus.RELEASED))
                .isTrue();
        assertThat(ReleaseStatus.COMPLETED.canTransitionTo(ReleaseStatus.DELAYED))
                .isTrue();

        assertThat(ReleaseStatus.COMPLETED.canTransitionTo(ReleaseStatus.DRAFT)).isFalse();
        assertThat(ReleaseStatus.COMPLETED.canTransitionTo(ReleaseStatus.PLANNED))
                .isFalse();
        assertThat(ReleaseStatus.COMPLETED.canTransitionTo(ReleaseStatus.IN_PROGRESS))
                .isFalse();
        assertThat(ReleaseStatus.COMPLETED.canTransitionTo(ReleaseStatus.CANCELLED))
                .isFalse();
    }

    @Test
    void shouldAllowValidTransitionsFromDelayed() {
        assertThat(ReleaseStatus.DELAYED.canTransitionTo(ReleaseStatus.IN_PROGRESS))
                .isTrue();
        assertThat(ReleaseStatus.DELAYED.canTransitionTo(ReleaseStatus.PLANNED)).isTrue();
        assertThat(ReleaseStatus.DELAYED.canTransitionTo(ReleaseStatus.CANCELLED))
                .isTrue();

        assertThat(ReleaseStatus.DELAYED.canTransitionTo(ReleaseStatus.DRAFT)).isFalse();
        assertThat(ReleaseStatus.DELAYED.canTransitionTo(ReleaseStatus.COMPLETED))
                .isFalse();
        assertThat(ReleaseStatus.DELAYED.canTransitionTo(ReleaseStatus.RELEASED))
                .isFalse();
    }

    @Test
    void shouldNotAllowTransitionsFromFinalStates() {
        assertThat(ReleaseStatus.CANCELLED.canTransitionTo(ReleaseStatus.DRAFT)).isFalse();
        assertThat(ReleaseStatus.CANCELLED.canTransitionTo(ReleaseStatus.PLANNED))
                .isFalse();
        assertThat(ReleaseStatus.CANCELLED.canTransitionTo(ReleaseStatus.IN_PROGRESS))
                .isFalse();
        assertThat(ReleaseStatus.CANCELLED.canTransitionTo(ReleaseStatus.COMPLETED))
                .isFalse();
        assertThat(ReleaseStatus.CANCELLED.canTransitionTo(ReleaseStatus.DELAYED))
                .isFalse();
        assertThat(ReleaseStatus.CANCELLED.canTransitionTo(ReleaseStatus.RELEASED))
                .isFalse();

        assertThat(ReleaseStatus.RELEASED.canTransitionTo(ReleaseStatus.DRAFT)).isFalse();
        assertThat(ReleaseStatus.RELEASED.canTransitionTo(ReleaseStatus.PLANNED))
                .isFalse();
        assertThat(ReleaseStatus.RELEASED.canTransitionTo(ReleaseStatus.IN_PROGRESS))
                .isFalse();
        assertThat(ReleaseStatus.RELEASED.canTransitionTo(ReleaseStatus.COMPLETED))
                .isFalse();
        assertThat(ReleaseStatus.RELEASED.canTransitionTo(ReleaseStatus.DELAYED))
                .isFalse();
        assertThat(ReleaseStatus.RELEASED.canTransitionTo(ReleaseStatus.CANCELLED))
                .isFalse();
    }
}
