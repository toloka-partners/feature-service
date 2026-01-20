package com.sivalabs.ft.features.domain.models;

/**
 * Enum representing the status of a release.
 * Defines valid state transitions for release lifecycle management.
 */
public enum ReleaseStatus {
    DRAFT,
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    DELAYED,
    CANCELLED,
    RELEASED;

    /**
     * Validates if a transition from this status to the target status is allowed.
     *
     * @param targetStatus The status to transition to
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(ReleaseStatus targetStatus) {
        return switch (this) {
            case DRAFT -> targetStatus == PLANNED || targetStatus == CANCELLED;
            case PLANNED -> targetStatus == IN_PROGRESS || targetStatus == DELAYED || targetStatus == CANCELLED;
            case IN_PROGRESS -> targetStatus == COMPLETED || targetStatus == DELAYED || targetStatus == CANCELLED;
            case COMPLETED -> targetStatus == RELEASED || targetStatus == DELAYED;
            case DELAYED -> targetStatus == IN_PROGRESS || targetStatus == PLANNED || targetStatus == CANCELLED;
            case CANCELLED -> false; // Final state, no transitions allowed
            case RELEASED -> false; // Final state, no transitions allowed
        };
    }
}
