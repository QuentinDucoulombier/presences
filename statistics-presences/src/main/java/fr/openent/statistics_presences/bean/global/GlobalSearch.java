package fr.openent.statistics_presences.bean.global;

import fr.openent.statistics_presences.bean.Audience;
import fr.openent.statistics_presences.bean.User;
import fr.openent.statistics_presences.filter.Filter;
import fr.openent.statistics_presences.indicator.impl.Global;
import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GlobalSearch {
    List<String> totalAbsenceTypes = Arrays.asList(EventType.NO_REASON.name(), EventType.UNREGULARIZED.name(), EventType.REGULARIZED.name());

    private final Filter filter;
    private List<Audience> audiences = new ArrayList<>();
    private List<User> users = new LinkedList<>();
    private Map<String, List<JsonObject>> statisticsMapped;
    private Map<String, Number> totalAbsMap;
    private String recoveryMethod;
    private String halfDay;
    private static final String HALF_DAY = "HALF_DAY";
    private static final String HOUR = "HOUR";
    private static final String DAY = "DAY";

    public GlobalSearch(Filter filter) {
        this.filter = filter;
    }

    public Filter filter() {
        return this.filter;
    }

    public GlobalSearch setAudiences(List<Audience> audiences) {
        this.audiences = audiences;
        return this;
    }

    public List<Audience> audiences() {
        return this.audiences;
    }

    public GlobalSearch setUsers(List<User> users) {
        this.users = users;
        return this;
    }

    public Boolean containsAbsence() {
        return this.filter().types().stream().anyMatch(type -> totalAbsenceTypes.contains(type));
    }

    public List<User> users() {
        return this.users;
    }

    public GlobalSearch setTotalAbsMap(Map<String, Number> map) {
        this.totalAbsMap = map;
        return this;
    }

    public Number totalAbs(String user) {
        return this.totalAbsMap.get(user);
    }

    public GlobalSearch setStatistics(Map<String, List<JsonObject>> statistics) {
        this.statisticsMapped = statistics;
        return this;
    }

    public Map<String, List<JsonObject>> statistics() {
        return this.statisticsMapped;
    }

    public GlobalSearch setRecoveryMethod(String recoveryMethod) {
        this.recoveryMethod = recoveryMethod;
        return this;
    }

    public GlobalSearch setHalfDay(String halfDay) {
        this.halfDay = halfDay;
        return this;
    }

    public JsonArray prefetchUserPipeline() {
        JsonArray pipeline = new JsonArray()
                .add(match())
                .add(group())
                .add(fromToMatcher())
                .add(prefetchDistinct())
                .add(prefetchUserProject())
                .add(prefetchUserSort());

        if (this.filter().page() != null) {
            pipeline.add(skip())
                    .add(limit());
        }

        return pipeline;
    }

    private JsonObject prefetchUserProject() {
        JsonObject project = new JsonObject()
                .put("_id", "$_id.user")
                .put("name", "$_id.name")
                .put("class_name", "$_id.class_name");

        return new JsonObject()
                .put("$project", project);
    }

    private JsonObject prefetchUserSort() {
        JsonObject sort = new JsonObject()
                .put("class_name", 1)
                .put("name", 1);

        return new JsonObject()
                .put("$sort", sort);
    }

    private JsonObject prefetchDistinct() {
        JsonObject id = new JsonObject()
                .put("user", "$_id.user")
                .put("name", "$_id.name")
                .put("class_name", "$_id.class_name");

        return group(id(id));
    }

    public JsonArray totalAbsenceGlobalPipeline() {
        JsonArray pipeline = initAbsencePipeline()
                .add(match(totalAbsenceTypes, true));

        JsonObject group = groupCountAbsences();
        if (group != null && !group.isEmpty()) pipeline.add(group);
        pipeline.add(totalGlobalAbsenceGroup());

        if (this.filter().from() != null || this.filter().to() != null) pipeline.add(fromToMatcher());
        pipeline.add(projectAbsenceCount());

        return pipeline;
    }

    public JsonArray totalAbsenceUserPipeline() {
        JsonArray pipeline = initAbsencePipeline()
                .add(match(totalAbsenceTypes, true));

        JsonObject group = groupCountAbsences();
        if (group != null && !group.isEmpty()) {
            pipeline.add(group);
            pipeline.add(countTotalUser("$_id.user"));
        } else {
            pipeline.add(countTotalUser("$user"));
        }

        if (this.filter().from() != null || this.filter().to() != null) pipeline.add(fromToMatcher());

        pipeline.add(projectTotalUser());

        return pipeline;
    }

    public JsonArray countBasicEventTypedPipeline() {
        List<String> types = typesIn(type -> !totalAbsenceTypes.contains(type));
        if (types == null || types.isEmpty()) return new JsonArray();

        JsonArray pipeline = new JsonArray()
                .add(match(types))
                .add(group());

        if (this.filter().from() != null || this.filter().to() != null) pipeline.add(fromToMatcher());

        pipeline.add(countGroup())
                .add(projectCount());

        return pipeline;
    }

    public JsonArray countAbsencesPipeline() {
        List<String> types = typesIn(type -> totalAbsenceTypes.contains(type));
        if (types == null || types.isEmpty()) return new JsonArray();

        JsonArray pipeline = initAbsencePipeline()
                .add(match(types))
                .addAll(groupCountAbsencesTotal());

        if (this.filter().from() != null || this.filter().to() != null) pipeline.add(fromToMatcher());

        pipeline.add(projectCount());
        return pipeline;
    }

    private JsonObject totalGlobalAbsenceGroup() {
        JsonObject group = new JsonObject()
                .put("_id", "ABSENCE_TOTAL")
                .put("count", sum());

        return group(group);
    }

    private JsonObject countTotalUser(String userParameter) {
        JsonObject group = new JsonObject()
                .put("_id", userParameter);

        group.put("count", sum());

        return new JsonObject().put("$group", group);
    }

    private JsonObject projectTotalUser() {
        JsonObject project = new JsonObject()
                .put("_id", 0)
                .put("user", "$_id")
                .put("count", "$count");

        return new JsonObject().put("$project", project);
    }

    private JsonObject countGroup() {
        JsonObject group = new JsonObject()
                .put("_id", "$_id.type");

        JsonObject count = new JsonObject()
                .put("$sum", "$count");
        group.put("count", count);

        return group(group);
    }

    private JsonObject projectCount() {
        JsonObject project = new JsonObject()
                .put("_id", 0)
                .put("type", "$_id")
                .put("count", "$count");

        return new JsonObject().put("$project", project);
    }

    private JsonObject projectAbsenceCount() {
        JsonObject project = new JsonObject()
                .put("_id", 0)
                .put("type", "$_id")
                .put("count", sum("$count"));

        return new JsonObject().put("$project", project);
    }

    public JsonArray searchBasicEventTypedPipeline() {
        List<String> types = typesIn(type -> !totalAbsenceTypes.contains(type));
        if (types == null || types.isEmpty()) return new JsonArray();

        JsonArray pipeline = new JsonArray()
                .add(match(types))
                .add(group())
                .add(project());

        if (this.filter().from() != null || this.filter().to() != null) pipeline.add(fromToMatcher());

        return pipeline;
    }

    public JsonArray searchAbsencesPipeline() {
        List<String> types = typesIn(type -> totalAbsenceTypes.contains(type));
        if (types == null || types.isEmpty()) return new JsonArray();

        JsonArray pipeline = initAbsencePipeline()
                .add(match(types))
                .addAll(groupAbsences())
                .add(projectAbsence());

        if (this.filter().from() != null || this.filter().to() != null) pipeline.add(fromToMatcher());

        return pipeline;
    }

    private JsonArray initAbsencePipeline() {
        JsonArray pipeline = new JsonArray();
        if (!HOUR.equals(recoveryMethod)) pipeline.add(addStartAtField());
        return pipeline;
    }

    /**
     * add a start_at field at the beginning of the pipeline to have start_date in mongo date format (and so handle it)
     *
     * @return field start_at
     */
    private JsonObject addStartAtField() {
        JsonObject dateString = new JsonObject().put("dateString", "$start_date");

        JsonObject dateFromString = new JsonObject()
                .put("$dateFromString", dateString);

        JsonObject statAtField = new JsonObject()
                .put("start_at", dateFromString);

        return new JsonObject()
                .put("$addFields", statAtField);
    }

    private JsonObject fromToMatcher() {
        JsonObject filterMatcher = new JsonObject();
        if (this.filter().from() != null) {
            filterMatcher.put("$gte", this.filter().from());
        }

        if (this.filter().to() != null) {
            filterMatcher.put("$lte", this.filter().to());
        }

        JsonObject count = new JsonObject()
                .put("count", filterMatcher);

        return new JsonObject()
                .put("$match", count);
    }

    private JsonObject match(List<String> types, boolean isTotalAbsences) {
        List<String> userIdentifiers = !this.filter().users().isEmpty()
                ? this.filter().users()
                : this.users().stream().map(User::id).collect(Collectors.toList());

        JsonObject matcher = new JsonObject()
                .put("structure", this.filter.structure())
                .put("$or", filterType(types, isTotalAbsences))
                .put("start_date", this.startDateFilter())
                .put("end_date", this.endDateFilter());

        if (!this.filter.audiences().isEmpty() && userIdentifiers.isEmpty()) {
            matcher.put("audiences", audienceFilter());
        }

        if (!userIdentifiers.isEmpty()) {
            matcher.put("user", usersFilter(userIdentifiers));
        }

        return new JsonObject()
                .put("$match", matcher);
    }


    private JsonObject match(List<String> types) {
        return match(types, false);
    }

    private JsonObject match() {
        return match(this.filter().types());
    }

    private JsonArray filterType(List<String> types, boolean isTotalAbsences) {
        List<String> typesToUse = types != null ? types : this.filter().types();
        JsonArray filters = new JsonArray();
        for (String type : typesToUse) {
            JsonObject filterType = new JsonObject()
                    .put("type", type);

            if (!isTotalAbsences) {
                EventType eventType = EventType.valueOf(type);
                switch (eventType) {
                    case UNREGULARIZED:
                    case REGULARIZED:
                        JsonObject inFilterReasons = new JsonObject()
                                .put("$in", this.filter().reasons());
                        filterType.put("reason", inFilterReasons);
                        break;
                    case SANCTION:
                        JsonObject inFilterSanctionTypes = new JsonObject()
                                .put("$in", this.filter.sanctionTypes());
                        filterType.put("punishment_type", inFilterSanctionTypes);
                        break;
                    case PUNISHMENT:
                        JsonObject inFilterPunishmentTypes = new JsonObject()
                                .put("$in", this.filter.punishmentTypes());
                        filterType.put("punishment_type", inFilterPunishmentTypes);
                        break;
                    default:
                        break;
                }
            }

            filters.add(filterType);
        }

        return filters;
    }

    private JsonObject audienceFilter() {
        return new JsonObject()
                .put("$in", this.filter().audiences());
    }

    private JsonObject usersFilter(List<String> users) {
        return new JsonObject()
                .put("$in", users);
    }

    private JsonObject startDateFilter() {
        return new JsonObject()
                .put("$gte", this.filter().start());
    }

    private JsonObject endDateFilter() {
        return new JsonObject()
                .put("$lte", this.filter().end());
    }

    private JsonObject group() {
        JsonObject id = new JsonObject()
                .put("user", "$user")
                .put("type", "$type")
                .put("name", "$name")
                .put("class_name", "$class_name");

        JsonObject slots = new JsonObject()
                .put("$sum", "$slot");

        JsonObject group = id(id)
                .put("count", sum())
                .put("slots", slots);

        return group(group);
    }

    private JsonObject project() {
        JsonObject project = new JsonObject()
                .put("_id", 0)
                .put("user", "$_id.user")
                .put("type", "$_id.type")
                .put("count", "$count")
                .put("slots", "$slots");

        return new JsonObject()
                .put("$project", project);
    }

    private JsonArray groupAbsences() {
        JsonArray groups;
        switch (recoveryMethod) {
            case DAY:
                groups = groupByCount(idByDay());
                break;
            case HALF_DAY:
                groups = groupByCount(idByHalfDay());
                break;
            default:
                groups = new JsonArray().add(groupByHour());
        }
        return groups;
    }

    private JsonObject groupCountAbsences() {
        JsonObject group;
        switch (recoveryMethod) {
            case DAY:
                group = group(id(idByDay()));
                break;
            case HALF_DAY:
                group = group(id(idByHalfDay()));
                break;
            default:
                group = new JsonObject();
        }
        return group;
    }

    private JsonArray groupCountAbsencesTotal() {
        JsonArray groups;
        switch (recoveryMethod) {
            case DAY:
                groups = groupByCountTotal(idByDay());
                break;
            case HALF_DAY:
                groups = groupByCountTotal(idByHalfDay());
                break;
            default:
                groups = new JsonArray().add(groupByTypeTotal());
        }
        return groups;
    }

    private JsonObject groupByHour() {
        JsonObject id = new JsonObject()
                .put("user", "$user")
                .put("type", "$type")
                .put("name", "$name")
                .put("class_name", "$class_name");

        JsonObject group = new JsonObject()
                .put("_id", id)
                .put("user", first("$user"))
                .put("type", first("$type"))
                .put("count", sum())
                .put("slots", sum());

        return group(group);
    }

    private JsonObject idByDay() {
        return new JsonObject()
                .put("user", "$user")
                .put("type", "$type")
                .put("name", "$name")
                .put("class_name", "$class_name")
                .put("day", day())
                .put("month", month())
                .put("year", year());
    }

    private JsonObject idByHalfDay() {
        return new JsonObject()
                .put("user", "$user")
                .put("type", "$type")
                .put("name", "$name")
                .put("class_name", "$class_name")
                .put("day", day())
                .put("month", month())
                .put("year", year())
                .put("is_before_halfday", isBeforeHalfday());
    }

    private JsonArray groupByCount(JsonObject id) {
        JsonArray groups = new JsonArray();
        JsonObject group = id(id)
                .put("slots", sum());
        groups.add(group(group));

        id = new JsonObject()
                .put("user", "$_id.user")
                .put("type", "$_id.type");

        group = id(id)
                .put("count", sum())
                .put("slots", sum("$slots"));
        groups.add(group(group));

        return groups;
    }

    private JsonArray groupByCountTotal(JsonObject id) {
        JsonArray groups = new JsonArray();
        JsonObject group = id(id)
                .put("type", first("$type"));
        groups.add(group(group))
                .add(groupByTypeTotal());
        return groups;
    }

    private JsonObject groupByTypeTotal() {
        return group(new JsonObject()
                .put("_id", "$type")
                .put("count", sum()));
    }


    private JsonObject isBeforeHalfday() {
        JsonObject dateToString = new JsonObject()
                .put("format", "%H:%M:%S")
                .put("date", "$start_at");


        JsonArray lt = new JsonArray()
                .add(new JsonObject().put("$dateToString", dateToString))
                .add(halfDay);

        return new JsonObject()
                .put("$lt", lt);
    }

    private JsonObject projectAbsence() {
        JsonObject project = new JsonObject()
                .put("_id", 0)
                .put("user", "$_id.user")
                .put("type", "$_id.type")
                .put("count", 1)
                .put("slots", 1);

        return new JsonObject()
                .put("$project", project);
    }

    /*
    UTILITIES
     */

    private List<String> typesIn(Predicate<String> inTypes) {
        return this.filter().types().stream()
                .filter(inTypes)
                .collect(Collectors.toList());
    }

    private JsonObject day() {
        return new JsonObject()
                .put("$dayOfMonth", "$start_at");
    }

    private JsonObject month() {
        return new JsonObject()
                .put("$month", "$start_at");
    }

    private JsonObject year() {
        return new JsonObject()
                .put("$year", "$start_at");
    }

    private JsonObject limit() {
        return new JsonObject()
                .put("$limit", Global.PAGE_SIZE);
    }

    private JsonObject skip() {
        return new JsonObject()
                .put("$skip", this.filter().page() * Global.PAGE_SIZE);
    }

    private JsonObject first(String value) {
        return new JsonObject()
                .put("$first", value);
    }

    private JsonObject sum(String value) {
        return new JsonObject().put("$sum", value);
    }

    private JsonObject sum() {
        return new JsonObject().put("$sum", 1);
    }

    private JsonObject id(JsonObject value) {
        return new JsonObject()
                .put("_id", value);
    }

    private JsonObject group(JsonObject value) {
        return new JsonObject()
                .put("$group", value);
    }
}
