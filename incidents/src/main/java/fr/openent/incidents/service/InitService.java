package fr.openent.incidents.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.request.JsonHttpServerRequest;

public interface InitService {

    void getInitIncidentTypesStatement(JsonHttpServerRequest request, String structure, Handler<Either<String, JsonObject>> handler);

    void getInitIncidentPlacesStatement(JsonHttpServerRequest request, String structure, Handler<Either<String, JsonObject>> handler);

    void getInitIncidentProtagonistsStatement(JsonHttpServerRequest request, String structure, Handler<Either<String, JsonObject>> handler);

    void getInitIncidentSeriousnessStatement(JsonHttpServerRequest request, String structure, Handler<Either<String, JsonObject>> handler);

    void getInitIncidentPartnerStatement(JsonHttpServerRequest request, String structure, Handler<Either<String, JsonObject>> handler);
}
