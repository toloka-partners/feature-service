package com.sivalabs.ft.features.domain.models;

public enum ReleaseStatus {
    DRAFT,
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    RELEASED;

    /**
     * Get valid transition states from current status
     */
    public ReleaseStatus[] getValidTransitions() {
        return switch (this) {
            case DRAFT -> new ReleaseStatus[] {PLANNED};
            case PLANNED -> new ReleaseStatus[] {IN_PROGRESS, DRAFT};
            case IN_PROGRESS -> new ReleaseStatus[] {COMPLETED, PLANNED};
            case COMPLETED -> new ReleaseStatus[] {RELEASED};
            case RELEASED -> new ReleaseStatus[] {};
        };
    }

    /**
     * Check if transition to target status is valid
     */
    public boolean canTransitionTo(ReleaseStatus targetStatus) {
        for (ReleaseStatus validStatus : getValidTransitions()) {
            if (validStatus == targetStatus) {
                return true;
            }
        }
        return false;
    }
}
