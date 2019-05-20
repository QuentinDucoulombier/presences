import {_, idiom as lang, model, ng} from 'entcore';
import {
    Audiences,
    Exemption,
    Exemptions,
    Structure,
    Structures,
    Student,
    Students,
    Subjects
} from "../models";
import {Toast} from "../utilities";
import {DateUtils} from "@common/utils"
export * from '@common/directives/pagination';

interface ViewModel {
    structures: Structures;
    structure: Structure;
    filter: { start_date: any, end_date: any, students: any };
    translate: any;
    subjects: Subjects;
    audiences: Audiences;
    notifications: any[];
    createExemptionLightBox: Boolean;
    exemptions: Exemptions;
    students: Students;
    studentsFrom: Students;
    studentsSearching: Students;
    studentsFiltered: any[];
    audiencesFiltered: any[];
    form: any;
    formStudentSelected: any[];

    initData(): void;

    updateDate(): void;

    updateStructure(Structure): void;

    dateFormater(date): Date;

    searchByStudent(string): void;

    searchFormByStudent(searchText: string): void;

    selectFilterStudent(model: Student, option: Student): void;

    filters(student?, audience?): void;

    excludeAudienceFromFilter(audience): void;

    excludeStudentFromFilter(audience): void;

    selectStudentForm(model: Student, student): void;

    saveExemption(): void;

    deleteExemption(): void;

    createExemption(): void;

    editExemption(obj): void;

    closeCreateExemption(): void;

    excludeStudentFromForm(student): void;

}


export const exemptionsController = ng.controller('ExemptionsController', ['$scope', function ($scope) {
    const vm: ViewModel = this;

    console.log('ExemptionsController');
    /**
     * Init usefull var and function PART
     */
    vm.structures = new Structures();
    vm.structures.sync();
    vm.structure = vm.structures.first();
    vm.translate = lang.translate;
    const initData = () => {
        vm.notifications = [];
        vm.createExemptionLightBox = false;
        vm.filter = {
            start_date: DateUtils.add(new Date(), -30, "d"),
            end_date: new Date(),
            students: []
        };

        vm.exemptions = new Exemptions();
        vm.exemptions.eventer.on('loading::false', () => $scope.safeApply());

        vm.subjects = new Subjects();
        vm.subjects.sync(vm.structure.id);
        vm.audiences = new Audiences();
        vm.audiences.sync(vm.structure.id);

        vm.students = new Students();
        vm.studentsFrom = new Students();

        vm.studentsFiltered = [];
        vm.audiencesFiltered = [];
        vm.studentsSearching = new Students();
        loadExemptions();
        $scope.safeApply();

    };
    const loadExemptions = async (): Promise<void> => {
        await vm.exemptions.prepareSync(vm.structure.id, vm.filter.start_date, vm.filter.end_date, vm.studentsFiltered, vm.audiencesFiltered);
        $scope.safeApply();
    };

    initData();

    vm.updateStructure = (structure) => {
        vm.structure = structure;
        initData();
    };

    vm.updateDate = async () => {
        await loadExemptions();
        vm.filters();
    };

    vm.dateFormater = (date) => {
        return DateUtils.format(date, 'DD/MM/YYYY');
    };

    /**
     * Init & Manage main filter display PART
     */

    vm.searchByStudent = async (searchText: string) => {
        await vm.students.search(vm.structure.id, searchText);
        $scope.safeApply();
    }
    vm.searchFormByStudent = async (searchText: string) => {
        await vm.studentsFrom.search(vm.structure.id, searchText);
        $scope.safeApply();
    }

    vm.selectFilterStudent = function (model: Student, option: Student) {
        vm.filters(option);
    };

    vm.filters = (student?, audience?) => {
        if (audience && !_.find(vm.audiencesFiltered, audience)) {
            vm.audiencesFiltered.push(audience);
        }
        if (student && !_.find(vm.studentsFiltered, student)) {
            vm.studentsFiltered.push(student);
        }

        vm.exemptions.prepareSync(vm.structure.id, vm.filter.start_date, vm.filter.end_date, vm.studentsFiltered, vm.audiencesFiltered);
        $scope.safeApply();
    };
    vm.excludeStudentFromFilter = (student) => {
        vm.studentsFiltered = _.without(vm.studentsFiltered, _.findWhere(vm.studentsFiltered, student));
        vm.filters();
    };
    vm.excludeAudienceFromFilter = (audience) => {
        vm.audiencesFiltered = _.without(vm.audiencesFiltered, _.findWhere(vm.audiencesFiltered, audience));
        vm.filters();
    };


    /**
     * FORM PART
     */

    /**
     * Add student from multi select choice
     * @param student
     */
    vm.selectStudentForm = (model: Student, student) => {
        if (!_.find(vm.form.students, student)) {
            vm.form.students.push(student);
        }
    };

    const updateAfterSaveOrDelete = async (response, message) => {
        if (response.status == 200 || response.status == 201) {
            vm.closeCreateExemption();
            vm.notifications.push(new Toast(message, 'confirm'));
        } else {
            vm.notifications.push(new Toast(response.data.toString(), 'error'));
        }
        await loadExemptions();
        vm.filters()
        $scope.safeApply();
    };
    vm.saveExemption = async () => {
        let response = await vm.form.save();
        updateAfterSaveOrDelete(response, lang.translate('presences.exemptions.form.succeed'));
    };
    vm.deleteExemption = async () => {
        let response = await vm.form.delete();
        updateAfterSaveOrDelete(response, lang.translate('presences.exemptions.delete.succeed'));
    };


    /**
     * Manage Lightbox display PART
     */

    vm.createExemption = () => {
        vm.createExemptionLightBox = true;
        vm.form = new Exemption(vm.structure.id, true);
        $scope.safeApply()
    };

    vm.editExemption = (obj) => {
        if($scope.hasRight('manageExemption') != true)
            return;
        vm.createExemptionLightBox = true;
        vm.form = _.clone(obj);
        let studentTmp = new Student(obj.student);
        vm.form.students = [studentTmp];
        vm.formStudentSelected = [studentTmp];

        vm.form.subject = _.chain(vm.subjects.all)
            .filter((item) => {
                return item.id == obj.subjectId;
            })
            .first()
            .value();
        $scope.safeApply();
    };

    vm.closeCreateExemption = () => {
        vm.createExemptionLightBox = null;
    };


    /**
     * Exclude student from multi select choice
     * @param student
     */
    vm.excludeStudentFromForm = (student) => {
        vm.form.students = _.without(vm.form.students, _.findWhere(vm.form.students));
        vm.filters();
    };
}]);
