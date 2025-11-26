package com.sivalabs.ft.features.domain.models;

import java.util.Set;

/**
 * Represents the planning status of a feature within a release.
 * Implements state machine logic for valid status transitions.
 */
public enum FeaturePlanningStatus {
    NOT_STARTED,
    IN_PROGRESS,
    BLOCKED,
    DONE;

    /**
     * Validates if a transition from the current status to the target status is allowed.
     *
     * @param targetStatus the desired target status
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(FeaturePlanningStatus targetStatus) {
        if (this == targetStatus) {
            return true; // Same status is always allowed
        }

        return switch (this) {
            case NOT_STARTED -> targetStatus == IN_PROGRESS;
            case IN_PROGRESS -> targetStatus == BLOCKED || targetStatus == DONE || targetStatus == NOT_STARTED;
            case BLOCKED -> targetStatus == IN_PROGRESS || targetStatus == NOT_STARTED;
            case DONE -> targetStatus == IN_PROGRESS; // Allow reopening
        };
    }

    /**
     * Gets all valid target statuses from the current status.
     *
     * @return set of valid target statuses
     */
    public Set<FeaturePlanningStatus> getValidTransitions() {
        return switch (this) {
            case NOT_STARTED -> Set.of(NOT_STARTED, IN_PROGRESS);
            case IN_PROGRESS -> Set.of(NOT_STARTED, IN_PROGRESS, BLOCKED, DONE);
            case BLOCKED -> Set.of(NOT_STARTED, IN_PROGRESS, BLOCKED);
            case DONE -> Set.of(IN_PROGRESS, DONE);
        };
    }

    /**
     * Validates a status transition and throws an exception if invalid.
     *
     * @param targetStatus the desired target status
     * @throws IllegalArgumentException if the transition is not valid
     */
    public void validateTransition(FeaturePlanningStatus targetStatus) {
        if (!canTransitionTo(targetStatus)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid planning status transition from %s to %s. Valid transitions: %s",
                            this, targetStatus, getValidTransitions()));
        }
    }
}
