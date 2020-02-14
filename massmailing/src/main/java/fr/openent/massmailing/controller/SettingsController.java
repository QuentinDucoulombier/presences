package fr.openent.massmailing.controller;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.actions.Action;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.security.Manage;
import fr.openent.massmailing.service.SettingsService;
import fr.openent.massmailing.service.impl.DefaultSettingsService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class SettingsController extends ControllerHelper {
    private EventBus eb;
    private SettingsService settingsService;

    public SettingsController(EventBus eb) {
        this.settingsService = new DefaultSettingsService();
        this.eb = eb;
    }

    @Get("/settings/templates/:type")
    @SecuredAction(Massmailing.MANAGE)
    @ApiDoc("Get all templates for given types")
    public void getTemplates(HttpServerRequest request) {
        String mailingType = request.getParam("type");
        String structure = request.getParam("structure");

        if (structure == null || !MailingType.MAIL.toString().equals(mailingType)) {
            badRequest(request);
            return;
        }

        MailingType type = MailingType.valueOf(mailingType);
        settingsService.getTemplates(type, structure, arrayResponseHandler(request));
    }

    @Post("/settings/templates")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(Manage.class)
    @Trace(Action.MASSMAILING_TEMPLATE_CREATION)
    @ApiDoc("Create given template")
    public void createTemplate(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "template", body -> {
            UserUtils.getUserInfos(eb, request, user -> settingsService.createTemplate(body, user.getUserId(), defaultResponseHandler(request, 201)));
        });
    }

    @Put("/settings/templates/:id")
    @ApiDoc("Update given template")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(Manage.class)
    @Trace(Action.MASSMAILING_TEMPLATE_UPDATE)
    public void updateTemplate(HttpServerRequest request) {
        try {
            Integer id = Integer.parseInt(request.getParam("id"));
            RequestUtils.bodyToJson(request, pathPrefix + "template", template -> settingsService.updateTemplate(id, template, defaultResponseHandler(request)));
        } catch (NumberFormatException e) {
            log.error("[Masmailing@SettingsController] Failed to parse template identifier", e);
            badRequest(request);
        }
    }

    @Delete("/settings/templates/:id")
    @ApiDoc("Delete given template")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(Manage.class)
    @Trace(Action.MASSMAILING_TEMPLATE_DELETION)
    public void deleteTemplate(HttpServerRequest request) {
        try {
            Integer id = Integer.parseInt(request.getParam("id"));
            settingsService.deleteTemplate(id, defaultResponseHandler(request, 204));
        } catch (NumberFormatException e) {
            log.error("[Massmailing@SettingsController] Failed to parse template identifier", e);
            badRequest(request);
        }
    }
}
