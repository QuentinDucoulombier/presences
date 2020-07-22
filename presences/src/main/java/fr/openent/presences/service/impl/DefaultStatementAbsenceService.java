package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.enums.WorkflowActions;
import fr.openent.presences.model.StatementAbsence;
import fr.openent.presences.service.StatementAbsenceService;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DefaultStatementAbsenceService implements StatementAbsenceService {

    private static final Logger log = LoggerFactory.getLogger(DefaultStatementAbsenceService.class);

    private Storage storage;

    public DefaultStatementAbsenceService(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void get(UserInfos user, MultiMap body, Handler<AsyncResult<JsonObject>> handler) {
        String structure_id = body.get("structure_id");
        String id = body.get("id");
        String end_at = body.get("end_at");
        String start_at = body.get("start_at");
        List<String> student_ids = body.getAll("student_id");
        Boolean is_treated = body.get("is_treated") != null ? Boolean.getBoolean(body.get("is_treated")) : null;
        Integer page = body.get("page") != null ? Integer.parseInt(body.get("page")) : null;

        Future<JsonArray> listResultsFuture = Future.future();
        Future<Long> countResultsFuture = Future.future();

        getRequest(page, structure_id, id, start_at, end_at, student_ids, is_treated, listResultsFuture);
        countRequest(structure_id, id, start_at, end_at, student_ids, is_treated, countResultsFuture);

        CompositeFuture.all(listResultsFuture, countResultsFuture).setHandler(eventResult -> {
            if (eventResult.failed()) {
                handler.handle(Future.failedFuture(eventResult.cause().getMessage()));
                return;
            }

            JsonObject result = new JsonObject()
                    .put("page", page)
                    .put("page_count", countResultsFuture.result())
                    .put("all", listResultsFuture.result());

            handler.handle(Future.succeededFuture(result));
        });
    }

    @Override
    public void create(JsonObject body, HttpServerRequest request, Handler<AsyncResult<JsonObject>> handler) {
        StatementAbsence statementAbsence = new StatementAbsence();
        statementAbsence.setFromJson(body);
        statementAbsence.create(handler);
    }

    @Override
    public void validate(UserInfos user, JsonObject body, Handler<AsyncResult<JsonObject>> handler) {
        StatementAbsence statementAbsence = new StatementAbsence();
        body.put("treated_at", body.getBoolean("is_treated") ? new Date().toString() : null);
        body.put("validator_id", user.getUserId());
        statementAbsence.setFromJson(body);
        statementAbsence.update(handler);
    }

    @Override
    public void getFile(UserInfos user, MultiMap body, Handler<AsyncResult<JsonObject>> handler) {
        List<String> student_ids = null;
        if (!WorkflowHelper.hasRight(user, WorkflowActions.MANAGE_ABSENCE_STATEMENTS.toString())) {
            student_ids = body.getAll("student_id");
            boolean isValid = student_ids.size() > 0;
            for (String id : student_ids) {
                if (!(user.getUserId().equals(id) || user.getChildrenIds().contains(id))) isValid = false;
            }
            if (!isValid) {
                String message = "[Presences@DefaultStatementAbsenceService:getFile] You are not authorized to get this file.";
                log.error(message);
                handler.handle(Future.failedFuture(message));
                return;
            }
        }

        getRequest(null, body.get("structure_id"), body.get("idStatement"), null, null, student_ids, null, result -> {
            if (result.failed()) {
                String message = "[Presences@DefaultStatementAbsenceService:getFile] Failed to retrieve absence statements.";
                log.error(message + " " + result.cause().getMessage());
                handler.handle(Future.failedFuture(message));
                return;
            }

            if (result.result().size() != 1) {
                String message = "[Presences@DefaultStatementAbsenceService:getFile] You are not authorized to get this file.";
                log.error(message);
                handler.handle(Future.failedFuture(message));
                return;
            }

            handler.handle(Future.succeededFuture(result.result().getJsonObject(0)));
        });
    }


    private String queryGetter(Boolean isCountQuery, Integer page, String structure_id, String id, String start_at, String end_at, List<String> student_ids, Boolean is_treated, JsonArray params) {
        String query = (isCountQuery ? "SELECT COUNT(*) " : "SELECT * ") + " FROM " + Presences.dbSchema + ".statement_absence WHERE structure_id = ? ";

        params.add(structure_id);

        if (id != null) {
            query += "AND id = ? ";
            params.add(id);
        }

        if (end_at != null) {
            query += "AND start_at <= ? ";
            params.add(end_at);
        }

        if (start_at != null) {
            query += "AND end_at >= ? ";
            params.add(start_at);
        }

        if (student_ids != null && student_ids.size() > 0) {
            query += "AND student_id IN " + Sql.listPrepared(student_ids) + " ";
            params.addAll(new JsonArray(student_ids));
        }

        if (is_treated != null) {
            query += "AND treated_at " + (is_treated ? "IS NOT NULL " : "IS NULL ");
        }

        if (page != null) {
            query += "LIMIT " + Presences.PAGE_SIZE;
            query += "OFFSET " + page * Presences.PAGE_SIZE;
        }

        return query;
    }


    private void getRequest(Integer page, String structure_id, String id, String start_at, String end_at,
                            List<String> student_ids, Boolean is_treated, Handler<AsyncResult<JsonArray>> handler) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(queryGetter(false, page, structure_id, id, start_at, end_at, student_ids, is_treated, params),
                params, SqlResult.validResultHandler(eventResult -> {
                    if (eventResult.isLeft()) {
                        String message = "[Presences@DefaultStatementAbsenceService:getRequest] Failed to retrieve absence statements.";
                        log.error(message + " " + eventResult.left().getValue());
                        handler.handle(Future.failedFuture(message));
                        return;
                    }

                    handler.handle(Future.succeededFuture(eventResult.right().getValue()));
                }));
    }

    private void getRequest(Integer page, String structure_id, String id, String start_at, String end_at,
                            List<String> student_ids, Boolean is_treated, Future<JsonArray> future) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(queryGetter(false, page, structure_id, id, start_at, end_at, student_ids, is_treated, params),
                params, SqlResult.validResultHandler(eventResult -> {
                    if (eventResult.isLeft()) {
                        String message = "[Presences@DefaultStatementAbsenceService:getRequest] Failed to retrieve absence statements.";
                        log.error(message + " " + eventResult.left().getValue());
                        future.fail(message);
                        return;
                    }

                    future.complete(eventResult.right().getValue());
                }));
    }

    private void countRequest(String structure_id, String id, String start_at, String end_at,
                              List<String> student_ids, Boolean is_treated, Future<Long> future) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(queryGetter(true, null, structure_id, id, start_at, end_at, student_ids, is_treated, params),
                params, SqlResult.validUniqueResultHandler(eventResult -> {
                    if (eventResult.isLeft()) {
                        String message = "[Presences@DefaultStatementAbsenceService:countRequest] Failed to count absence statements.";
                        log.error(message + " " + eventResult.left().getValue());
                        future.fail(message);
                        return;
                    }

                    future.complete(eventResult.right().getValue().getLong("count") / Presences.PAGE_SIZE);
                }));
    }

}
