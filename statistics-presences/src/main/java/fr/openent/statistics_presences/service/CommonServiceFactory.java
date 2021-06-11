package fr.openent.statistics_presences.service;

import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.statistics_presences.service.impl.DefaultStatisticsPresencesService;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

public class CommonServiceFactory {
    private final Vertx vertx;

    public CommonServiceFactory(Vertx vertx) {
        this.vertx = vertx;
    }

    public UserService userService() {
        return new DefaultUserService();
    }

    public StatisticsPresencesService statisticsPresencesService() {
        return new DefaultStatisticsPresencesService(this);
    }

    public EventBus eventBus() {
        return this.vertx.eventBus();
    }

    public Vertx vertx() {
        return this.vertx;
    }
}
