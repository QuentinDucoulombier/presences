package fr.openent.presences.enums;

import fr.openent.presences.Presences;

public enum WorkflowActions {
    READ_PRESENCE(Presences.READ_PRESENCE),
    READ_PRESENCE_RESTRICTED(Presences.READ_PRESENCE_RESTRICTED),
    CREATE_REGISTER(Presences.CREATE_REGISTER),
    READ_REGISTER(Presences.READ_REGISTER),
    SEARCH(Presences.SEARCH),
    SEARCH_RESTRICTED(Presences.SEARCH_RESTRICTED),
    SEARCH_VIESCO(Presences.SEARCH_VIESCO),
    SEARCH_VIESCO_RESTRICTED(Presences.SEARCH_VIESCO_RESTRICTED),
    SEARCH_STUDENTS(Presences.SEARCH_STUDENTS),
    EXPORT(Presences.EXPORT),
    READ_EVENT(Presences.READ_EVENT),
    READ_EVENT_RESTRICTED(Presences.READ_EVENT_RESTRICTED),
    NOTIFY(Presences.NOTIFY),
    CREATE_EVENT(Presences.CREATE_EVENT),
    MANAGE_EXEMPTION(Presences.MANAGE_EXEMPTION),
    MANAGE_EXEMPTION_RESTRICTED(Presences.MANAGE_EXEMPTION_RESTRICTED),
    MANAGE(Presences.MANAGE),
    ALERT(Presences.ALERTS_WIDGET),
    ALERT_STUDENT_NUMBER(Presences.ALERTS_STUDENT_NUMBER),
    CREATE_PRESENCE(Presences.CREATE_PRESENCE),
    MANAGE_PRESENCE(Presences.MANAGE_PRESENCE),
    CREATE_ACTION(Presences.CREATE_ACTION),
    ABSENCES_WIDGET(Presences.ABSENCES_WIDGET),
    STUDENT_EVENTS_VIEW(Presences.STUDENT_EVENTS_VIEW),
    ABSENCE_STATEMENTS_VIEW(Presences.ABSENCE_STATEMENTS_VIEW),
    MANAGE_ABSENCE_STATEMENTS(Presences.MANAGE_ABSENCE_STATEMENTS),
    MANAGE_ABSENCE_STATEMENTS_RESTRICTED(Presences.MANAGE_ABSENCE_STATEMENTS_RESTRICTED),
    ABSENCE_STATEMENTS_CREATE(Presences.ABSENCE_STATEMENTS_CREATE),
    MANAGE_FORGOTTEN_NOTEBOOK(Presences.MANAGE_FORGOTTEN_NOTEBOOK),
    MANAGE_COLLECTIVE_ABSENCES(Presences.MANAGE_COLLECTIVE_ABSENCES),
    STATISTICS_ACCESS_DATA(Presences.STATISTICS_ACCESS_DATA),
    CALENDAR_VIEW(Presences.CALENDAR_VIEW),
    REGISTRY(Presences.REGISTRY),
    INIT_SETTINGS_1D(Presences.INIT_SETTINGS_1D),
    INIT_SETTINGS_2D(Presences.INIT_SETTINGS_2D),
    READ_EXEMPTION(Presences.READ_EXEMPTION),
    READ_EXEMPTION_RESTRICTED(Presences.READ_EXEMPTION_RESTRICTED),
    SETTINGS_GET(Presences.SETTINGS_GET),
    VIEW_STATISTICS(Presences.VIEW_STATISTICS),
    VIEW_STATISTICS_RESTRICTED(Presences.VIEW_STATISTICS_RESTRICTED);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return this.actionName;
    }
}
