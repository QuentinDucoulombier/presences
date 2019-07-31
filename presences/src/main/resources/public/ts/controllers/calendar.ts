import {model, ng} from 'entcore';
import {
    CalendarService,
    Course,
    GroupService,
    SearchItem,
    SearchService,
    StructureService,
    TimeSlot
} from '../services';
import {Scope} from './main';
import {User} from '../models';
import {DateUtils} from '@common/utils';
import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from "@common/model";

declare let window: any;

interface ViewModel {
    show: { loader: boolean, exemption: { start_date: string, end_date: string } };
    courses: {
        list: Array<Course>
    };
    slots: { list: Array<TimeSlot> };
    filter: {
        search: {
            item: string,
            items: Array<SearchItem>
        },
        student: User,
        students: Array<User>
    };

    selectItem(model: any, student: any): void;

    searchItem(value: string): void;

    loadCourses(): Promise<void>;

    formatExemptionDate(date: any): string;
}

interface CalendarScope extends Scope {
    hoverExemption($event, exemption: { start_date: string, end_date: string }): void;

    hoverOutExemption(): void;
}

export const calendarController = ng.controller('CalendarController', ['$scope', 'route', '$location', 'StructureService', 'CalendarService', 'GroupService', 'SearchService',
    function ($scope: CalendarScope, route, $location, StructureService: StructureService, CalendarService: CalendarService, GroupService: GroupService, SearchService: SearchService) {
        const vm: ViewModel = this;
        vm.show = {
            loader: true,
            exemption: null
        };
        vm.filter = {
            search: {
                item: '',
                items: null
            },
            student: null,
            students: null
        };
        vm.courses = {list: []};
        vm.slots = {list: []};

        model.calendar.eventer.on('calendar.create-item', () => {
            console.info(model.calendar.newItem);
            console.info(model.calendar.newItem.beginning.format());
            console.info(model.calendar.newItem.beginning.toString());
            console.info(model.calendar.newItem.beginning.isValid());
            console.info(model.calendar.newItem.beginning.toLocaleString());
        });

        $scope.$watch(() => window.structure, async () => {
            const structure_slots = await StructureService.getSlotProfile(window.structure.id);
            if (Object.keys(structure_slots).length > 0) vm.slots.list = structure_slots.slots;
            else vm.slots.list = null;
            $scope.safeApply();
        });

        model.calendar.on('date-change', initCalendar);

        async function initCalendar() {
            vm.show.loader = true;
            $scope.safeApply();
            const {item, structure} = window;
            if (item === null || structure === null) {
                $location.path('/');
                return;
            }

            vm.filter.student = item;
            vm.filter.students = await CalendarService.getStudentsGroup(item.groupId);
            if (item.type === 'GROUP' && vm.filter.students.length > 0) {
                vm.filter.student = vm.filter.students[0];
                window.item = vm.filter.student;
                $location.path(`/calendar/${vm.filter.student.id}`);
            }
            await vm.loadCourses();
        }

        vm.loadCourses = async function (student = vm.filter.student) {
            vm.show.loader = true;
            if (vm.filter.student.id !== student.id) {
                vm.filter.student = student;
                window.item = student;
            }
            const {structure} = window;
            const start = DateUtils.format(model.calendar.firstDay, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
            const end = DateUtils.format(DateUtils.add(model.calendar.firstDay, 1, 'w'), DateUtils.FORMAT["YEAR-MONTH-DAY"]);
            vm.courses.list = await CalendarService.getCourses(structure.id, student.id, start, end);
            vm.show.loader = false;
            $scope.safeApply();
        };

        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.CREATION, () => {
            let diff = 7;
            if (!model.calendar.display.saturday) diff--;
            if (!model.calendar.display.synday) diff--;
            $scope.$broadcast(SNIPLET_FORM_EVENTS.SET_PARAMS, {
                student: window.item,
                start_date: model.calendar.firstDay,
                end_date: DateUtils.add(model.calendar.firstDay, diff)
            });
        });

        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.FILTER, initCalendar);

        vm.selectItem = function (model, item) {
            const needsToLoadGroup = (window.item.groupId !== item.groupId) || item.type === 'GROUP';
            window.item = item;
            vm.filter.search.items = undefined;
            vm.filter.search.item = '';
            if (needsToLoadGroup) {
                initCalendar();
            } else {
                vm.filter.student = item;
                vm.loadCourses();
            }
        };

        vm.searchItem = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.filter.search.items = await SearchService.search(structureId, value);
            } catch (err) {
                vm.filter.search.items = [];
            } finally {
                $scope.safeApply();
            }
        };

        $scope.hoverExemption = function ($event, exemption) {
            const {width, height} = getComputedStyle(hover);
            let {x, y} = $event.target.closest('.exemption-label').getBoundingClientRect();
            hover.style.top = `${y - parseInt(height)}px`;
            hover.style.left = `${x - (parseInt(width) / 4)}px`;
            hover.style.display = 'flex';
            vm.show.exemption = exemption;
            $scope.safeApply();
        };

        $scope.hoverOutExemption = function () {
            hover.style.display = 'none';
        };

        vm.formatExemptionDate = function (date) {
            return DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
        };

        const hover = document.getElementById('exemption-hover');
    }]);