package fr.openent.statistics_presences.controller;

import fr.openent.presences.common.bus.BusResultHandler;
import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.indicator.Indicator;
import fr.openent.statistics_presences.model.StatisticsFilter;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import fr.openent.statistics_presences.service.StatisticsPresencesService;
import fr.openent.statistics_presences.service.impl.DefaultStatisticsPresencesService;
import fr.wseduc.bus.BusAddress;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import java.util.Arrays;
import java.util.List;

public class EventBusController extends ControllerHelper {

    private final StatisticsPresencesService statisticsService;

    public EventBusController(CommonServiceFactory commonServiceFactory) {
        statisticsService = new DefaultStatisticsPresencesService(commonServiceFactory);
    }

    @BusAddress("fr.openent.statistics.presences")
    @SuppressWarnings("unchecked")
    public void bus(final Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = body.getString("action");
        switch (action) {
            case "post-users":
                String structure = body.getString("structureId");
                List<String> student = body.getJsonArray("studentIds").getList();
                statisticsService.create(structure, student, BusResultHandler.busResponseHandler(message));
                break;
            case "get-statistics-graph":
                structure = body.getString("structureId");
                String indicatorName = body.getString(Field.INDICATOR);
                JsonObject filterJson = body.getJsonObject("filter");
                StatisticsFilter filter = new StatisticsFilter(structure, filterJson);
                Indicator indicator = StatisticsPresences.indicatorMap.get(indicatorName);
                indicator.searchGraph(filter, BusResultHandler.busResponseHandler(message));
                break;
            case "get-statistics":
                structure = body.getString("structureId");
                indicatorName = body.getString(Field.INDICATOR);
                filterJson = body.getJsonObject("filter");
                Integer page = body.getInteger(Field.PAGE);
                filter = new StatisticsFilter(structure, filterJson).setPage(page);
                indicator = StatisticsPresences.indicatorMap.get(indicatorName);
                indicator.search(filter, BusResultHandler.busResponseHandler(message));
                break;
            case "get-statistics-indicator":
                JsonArray indicatorJsonArray = new JsonArray(Arrays.asList(StatisticsPresences.indicatorMap.keySet().toArray()));
                JsonObject result = new JsonObject().put("indicator", indicatorJsonArray);
                message.reply(new JsonObject()
                        .put("status", "ok")
                        .put("result", result));
                break;
            default:
                message.reply(new JsonObject()
                        .put("status", "error")
                        .put("message", "Invalid action."));
        }
    }
}
