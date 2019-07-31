package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.enums.GroupType;
import fr.openent.presences.service.GroupService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.util.List;

public class DefaultGroupService implements GroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultGroupService.class);
    private EventBus eb;

    public DefaultGroupService(EventBus eb) {
        this.eb = eb;
    }


    @Override
    public void getGroupsId(String structureId, JsonArray groups, JsonArray classes, Handler<Either<String, JsonObject>> handler) {
        Future<JsonArray> groupFuture = Future.future();
        Future<JsonArray> classFuture = Future.future();
        CompositeFuture.all(groupFuture, classFuture).setHandler(event -> {
            if (event.failed()) {
                LOGGER.error(event.cause());
                handler.handle(new Either.Left<>(event.cause().toString()));
            } else {
                JsonObject res = new JsonObject()
                        .put("classes", classFuture.result())
                        .put("groups", groupFuture.result());

                handler.handle(new Either.Right<>(res));
            }
        });
        getIdsFromClassOrGroups(structureId, groups, GroupType.GROUP, FutureHelper.handlerJsonArray(groupFuture));
        getIdsFromClassOrGroups(structureId, classes, GroupType.CLASS, FutureHelper.handlerJsonArray(classFuture));
    }

    @Override
    public void getGroupUsers(String id, GroupType type, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject();
        if (type.equals(GroupType.GROUP)) {
            action.put("action", "groupe.listUsersByGroupeEnseignementId")
                    .put("groupEnseignementId", id)
                    .put("profile", "Student");
        } else {
            action.put("action", "classe.getEleveClasse")
                    .put("idClasse", id);
        }

        eb.send("viescolaire", action, event -> {
            JsonObject body = (JsonObject) event.result().body();
            if (event.failed() || "error".equals(body.getString("status"))) {
                String message = "[Presences@DefaultGroupService] Failed to retrieve users;";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                handler.handle(new Either.Right<>(body.getJsonArray("results")));
            }
        });
    }

    @Override
    public void getUserGroups(List<String> users, String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[:IN]->(g:FunctionalGroup)-[:DEPENDS]->(s:Structure {id:{structureId}}) WHERE u.id IN {users} return g.id as id, g.name as name" +
                " UNION " +
                "MATCH (u:User)-[:IN]->(g:Group)-[:DEPENDS]->(c:Class)-[:BELONGS]->(s:Structure {id:{structureId}}) WHERE u.id IN {users} return c.id as id, c.name as name";
        JsonObject params = new JsonObject()
                .put("users", users)
                .put("structureId", structureId);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getGroupStudents(String groupIdentifier, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (g:Group {id:{id}})<-[:IN]-(u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
                "WHERE u.profiles = ['Student'] " +
                "RETURN distinct u.id as id, (u.lastName + ' ' + u.firstName) as displayName, 'USER' as type, c.id as groupId, c.name as groupName " +
                "ORDER BY displayName " +
                "UNION  " +
                "MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class {id:{id}}) " +
                "WHERE u.profiles = ['Student'] " +
                "RETURN distinct u.id as id, (u.lastName + ' ' + u.firstName) as displayName, 'USER' as type, c.id as groupId, c.name as groupName " +
                "ORDER BY displayName";
        JsonObject params = new JsonObject()
                .put("id", groupIdentifier);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    /**
     * Retrieves identifiers based on name
     *
     * @param structureId structure identifier
     * @param objects     object list
     * @param type        group type
     * @param handler     Function handler returning data
     */
    private void getIdsFromClassOrGroups(String structureId, JsonArray objects, GroupType type, Handler<Either<String, JsonArray>> handler) {
        String query = type.equals(GroupType.GROUP)
                ? "MATCH (s:Structure {id:{structureId}})<-[:DEPENDS]-(c:Group) WHERE c.name IN {objects} RETURN c.id as id"
                : "MATCH (s:Structure {id:{structureId}})<-[:BELONGS]-(c:Class) WHERE c.name IN {objects} RETURN c.id as id";
        JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("objects", objects);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }
}
