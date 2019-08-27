import {_, idiom as lang, model, moment, toasts} from 'entcore';
import {Exemption, Student, Students, Subjects} from '../models';
import rights from "../rights";
import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from '@common/model'

console.log("ExemptionForm sniplet");

export enum EXEMPTIONS_FORM_EVENTS {
    EDIT = 'exemptions-form:edit',
}

declare let window: any;

interface ViewModel {
    createExemptionLightBox: boolean;
    subjects: Subjects;
    studentsFrom: Students;
    formStudentSelected: any[];
    form: any;

    createExemption(): void;

    setFormParams(obj: any): void;

    editExemption(obj): void;

    closeCreateExemption(): void;

    selectStudentForm(model: Student, student): void;

    excludeStudentFromForm(student): void;

    searchFormByStudent(searchText: string): void;

    saveExemption(): void;

    updateAfterSaveOrDelete(response: any, message: string): Promise<void>;

    deleteExemption(): void;

    isValidDate(): boolean;

    safeApply(fn?: () => void): void;
}

const vm: ViewModel = {
    form: null,
    safeApply: null,
    createExemptionLightBox: false,
    formStudentSelected: [],
    subjects: new Subjects(),
    studentsFrom: new Students(),
    updateAfterSaveOrDelete: async function (response, message) {
        if (response.status == 200 || response.status == 201) {
            vm.closeCreateExemption();
            toasts.confirm(message);
        } else {
            toasts.warning(response.data.toString());
        }
        exemptionForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.FILTER);
        vm.safeApply();
    },
    createExemption: () => {
        vm.createExemptionLightBox = true;
        vm.form = new Exemption(window.structure.id, true);
        vm.form.subject = vm.subjects.findEPS();
        if (!vm.form.subject) {
            vm.form.subject = vm.subjects.all[0];
        }
        exemptionForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.CREATION);
        vm.safeApply()
    },
    setFormParams: ({student, start_date, end_date}) => {
        if (vm.form) {
            vm.selectStudentForm(null, student);
            vm.form.startDate = moment(start_date).toDate();
            vm.form.endDate = moment(end_date).toDate();
            vm.safeApply();
        }
    },
    isValidDate: () => moment(vm.form.startDate).toDate() <= moment(vm.form.endDate).toDate(),
    editExemption: (obj) => {
        if (!model.me.hasWorkflow(rights.workflow['manageExemption'])) {
            return;
        }
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
        vm.safeApply();
    },
    selectStudentForm: (model: Student, student) => {
        if (!_.find(vm.form.students, student)) {
            vm.form.students.push(student);
        }

        vm.studentsFrom.all = [];
    },
    searchFormByStudent: async (searchText: string) => {
        await vm.studentsFrom.search(window.structure.id, searchText);
        vm.safeApply();
    },
    excludeStudentFromForm: (student) => {
        vm.form.students = _.without(vm.form.students, student);
    },
    deleteExemption: async () => {
        let response = await vm.form.delete();
        vm.updateAfterSaveOrDelete(response, lang.translate('presences.exemptions.delete.succeed'));
    },
    saveExemption: async () => {
        let response = await vm.form.save();
        if (vm.form.id) {
            vm.updateAfterSaveOrDelete(response, lang.translate('presences.exemptions.form.edit.succeed'));
        } else {
            vm.updateAfterSaveOrDelete(response, lang.translate('presences.exemptions.form.create.succeed'));
        }
    },
    closeCreateExemption: () => {
        vm.createExemptionLightBox = false;
    }
};

export const exemptionForm = {
    title: 'presences.exemptions.form.sniplet.title',
    public: false,
    that: null,
    controller: {
        init: function () {
            this.vm = vm;
            this.setHandler();
            exemptionForm.that = this;
            vm.safeApply = this.safeApply;
            vm.subjects.sync(window.structure.id);
        },
        setHandler: function () {
            this.$on(EXEMPTIONS_FORM_EVENTS.EDIT, (event, arg) => vm.editExemption(arg));
            this.$on(SNIPLET_FORM_EVENTS.SET_PARAMS, (event, arg) => vm.setFormParams(arg));
        }
    }
};