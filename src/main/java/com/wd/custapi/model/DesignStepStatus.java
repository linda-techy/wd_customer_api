package com.wd.custapi.model;

public enum DesignStepStatus {
    NOT_STARTED("not_started"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed");

    private final String value;

    DesignStepStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
