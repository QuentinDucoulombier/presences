package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.GroupService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;

public class DefaultAbsenceService implements AbsenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAbsenceService.class);

    private EventBus eb;
    private GroupService groupService;

    public DefaultAbsenceService(EventBus eb) {
        this.eb = eb;
        this.groupService = new DefaultGroupService(eb);

    }

    @Override
    public void get(Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject absenceBody, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "INSERT INTO " + Presences.dbSchema + ".absence(start_date, end_date, student_id) " +
                "VALUES (?, ?, ?) RETURNING id;";
        JsonArray params = new JsonArray()
                .add(absenceBody.getString("start_date"))
                .add(absenceBody.getString("end_date"))
                .add(absenceBody.getString("student_id"));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(absenceResult -> {
            if (absenceResult.isLeft()) {
                String message = "[Presences@DefaultAbsenceService] failed to create absence";
                LOGGER.error(message, absenceResult.left().getValue());
                handler.handle(new Either.Left<>(message));
            } else {
                List<String> users = new ArrayList<>();
                users.add(absenceBody.getString("student_id"));
                groupService.getUserGroups(users, absenceBody.getString("structure_id"), groupEvent -> {
                    if (groupEvent.isLeft()) {
                        String message = "[Presences@DefaultAbsenceService] failed to retrieve user info";
                        LOGGER.error(message, groupEvent.left().getValue());
                        handler.handle(new Either.Left<>(message));
                    } else {
                        matchEventsWithAbsents(absenceBody, groupEvent.right().getValue().getJsonObject(0).getString("id"), event -> {
                            if (event.isLeft()) {
                                String message = "[Presences@DefaultAbsenceService] failed to retrieve user info";
                                LOGGER.error(message, event.left().getValue());
                                handler.handle(new Either.Left<>(message));
                            } else {
                                handler.handle(new Either.Right<>(event.right().getValue()));
                            }
                        });
                    }
                });
            }
        }));
    }

    @Override
    public void absenceRemovalTask(Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Presences.dbSchema + ".absence where start_date <= NOW() - interval '72 hour'";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    private void matchEventsWithAbsents(JsonObject absenceBody, String id, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH register as ( " +
                " SELECT register.id, register.start_date, register.end_date FROM " + Presences.dbSchema + ".register " +
                " INNER JOIN " + Presences.dbSchema + ".rel_group_register as rgr ON (rgr.register_id = register.id) " +
                " WHERE rgr.group_id = ? " +
                " AND register.start_date >= ? " +
                " AND register.end_date <= ? " +
                "AND register.id NOT IN (SELECT event.register_id FROM " + Presences.dbSchema + ".event " +
                "WHERE event.type_id = 1 and event.register_id = register.id and event.student_id = ? ))" +
                "INSERT INTO " + Presences.dbSchema + ".event (start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, owner)" +
               "(SELECT  register.start_date, register.end_date, '', false, ?, register.id, 1, ? FROM register) returning register_id";

        JsonArray params = new JsonArray()
                .add(id)
                .add(absenceBody.getString("start_date"))
                .add(absenceBody.getString("end_date"))
                .add(absenceBody.getString("student_id"))
                .add(absenceBody.getString("student_id"))
                .add(absenceBody.getString("owner"));

       Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
