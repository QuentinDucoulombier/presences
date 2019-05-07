package fr.openent.presences.enums;

import fr.openent.presences.Presences;

public enum WorkflowActions {
    CREATE_REGISTER(Presences.CREATE_REGISTER),
    READ_REGISTER(Presences.READ_REGISTER),
    SEARCH(Presences.SEARCH),
    CREATE_EVENT(Presences.CREATE_EVENT);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return this.actionName;
    }
}
