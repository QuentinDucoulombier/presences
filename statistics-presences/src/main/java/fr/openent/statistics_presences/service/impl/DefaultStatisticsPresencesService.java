package fr.openent.statistics_presences.service.impl;


import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.db.DBService;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import fr.openent.statistics_presences.service.StatisticsPresencesService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.SqlResult;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultStatisticsPresencesService extends DBService implements StatisticsPresencesService {
    private final CommonServiceFactory commonServiceFactory;

    public DefaultStatisticsPresencesService(CommonServiceFactory commonServiceFactory) {
        this.commonServiceFactory = commonServiceFactory;
    }

    @Override
    public void create(String structureId, List<String> studentIds, Handler<AsyncResult<JsonObject>> handler) {
        JsonArray statements = new JsonArray(studentIds.stream().map(studentId -> createStatisticsUserStatement(structureId, studentId)).collect(Collectors.toList()));
        sql.transaction(statements, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(handler)));
    }

    @Override
    public Future<JsonObject> processStatisticsPrefetch(List<String> structures) {
        Promise<JsonObject> promise = Promise.promise();
        if (structures.isEmpty()) {
            promise.fail("No structure(s) identifier given");
        } else {
            JsonObject param = new JsonObject().put("structure", structures);
            StatisticsPresences.launchProcessingStatistics(commonServiceFactory.eventBus(), param);
            promise.complete(new JsonObject().put("status", "ok"));
        }
        return promise.future();
    }

    /**
     * Get statement that create statistic user
     *
     * @param structureId structure identifier
     * @param studentId   user student identifier
     * @return Statement
     */
    private JsonObject createStatisticsUserStatement(String structureId, String studentId) {

        String query = " INSERT INTO " + StatisticsPresences.DB_SCHEMA + ".user(id, structure) " +
                " VALUES (?, ?) " +
                " ON CONFLICT (id, structure) DO NOTHING";

        JsonArray values = new JsonArray()
                .add(studentId)
                .add(structureId);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }


}
