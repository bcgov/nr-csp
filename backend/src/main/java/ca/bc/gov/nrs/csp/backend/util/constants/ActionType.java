package ca.bc.gov.nrs.csp.backend.util.constants;

public enum ActionType {
    SAVE("save"),
    SUBMIT("submit"),
    DELETE("delete"),
    OTHER("");

    private final String description;

    ActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
