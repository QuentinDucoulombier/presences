package fr.openent.presences;

import fr.openent.presences.controller.*;
import fr.openent.presences.cron.AbsenceRemovalTask;
import fr.wseduc.cron.CronTrigger;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.http.BaseServer;

import java.text.ParseException;

public class Presences extends BaseServer {

    public static String dbSchema;
    public static String ebViescoAddress = "viescolaire";

    public static final String READ_REGISTER = "presences.register.read";
    public static final String CREATE_REGISTER = "presences.register.create";
    public static final String SEARCH = "presences.search";
    public static final String EXPORT = "presences.export";
    public static final String NOTIFY = "presences.notify";
    public static final String CREATE_EVENT = "presences.event.create";
    public static final String READ_EXEMPTION = "presences.exemption.read";
    public static final String MANAGE_EXEMPTION = "presences.exemption.manage";

    // Widget rights
    public static final String ALERTS_WIDGET = "presences.widget.alerts";
    public static final String FORGOTTEN_REGISTERS_WIDGET = "presences.widget.forgotten_registers";
    public static final String STATEMENTS_WIDGET = "presences.widget.statements";
    public static final String REMARKS_WIDGET = "presences.widget.remarks";
    public static final String ABSENCES_WIDGET = "presences.widget.absences";
    public static final String DAY_COURSES_WIDGET = "presences.widget.day_courses";
    public static final String CURRENT_COURSE_WIDGET = "presences.widget.current_course";
    public static final String DAY_PRESENCES_WIDGET = "presences.widget.day_presences";

    public static Integer PAGE_SIZE = 20;

    @Override
    public void start() throws Exception {
        super.start();
        dbSchema = config.getString("db-schema");
        ebViescoAddress = "viescolaire";
        final EventBus eb = getEventBus(vertx);
        final String exportCron = config.getString("export-cron");


        addController(new PresencesController(eb));
        addController(new CourseController(eb));
        addController(new RegisterController(eb));
        addController(new AbsenceController(eb));
        addController(new EventController(eb));
        addController(new ExemptionController(eb));
        addController(new SearchController(eb));
        addController(new CalendarController(eb));

        // Controller that create fake rights for widgets
        addController(new FakeRight());

        try {
            new CronTrigger(vertx, exportCron).schedule(new AbsenceRemovalTask(vertx.eventBus()));
        } catch (ParseException e) {
            log.fatal(e.getMessage(), e);
        }

    }

}
