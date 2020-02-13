import {User} from "@common/model/User";
import {SearchService} from "@common/services/SearchService";
import {AutoCompleteUtils} from "../autocomplete";

/**
 * ⚠ This class is used for the directive async-autocomplete
 * use it only for students's info purpose
 */

export class StudentsSearch extends AutoCompleteUtils {

    private students: Array<User>;
    private selectedStudents: Array<{}>;

    public student: string;

    constructor(structureId: string, searchService: SearchService) {
        super(structureId, searchService);
        this.students = [];
        this.selectedStudents = [];
    }

    public getStudents() {
        return this.students;
    }

    public getSelectedStudents() {
        return this.selectedStudents;
    }

    public removeSelectedStudents(studentItem) {
        this.selectedStudents.splice(this.selectedStudents.indexOf(studentItem), 1);
    }

    public resetStudents() {
        this.students = [];
    }

    public resetSelectedStudents() {
        this.selectedStudents = [];
    }

    public selectStudents(valueInput, studentItem) {
        if (this.selectedStudents.find(student => student["id"] === studentItem.id) === undefined) {
            this.selectedStudents.push(studentItem);
        }
    };

    public selectStudent(valueInput, studentItem) {
        this.selectedStudents = [];
        this.selectedStudents.push(studentItem);
    }

    public async searchStudents(valueInput: string) {
        try {
            this.students = await this.searchService.searchUser(this.structureId, valueInput, 'Student');
        } catch (err) {
            this.students = [];
            throw err;
        }
    };

    public searchStudentsFromArray(valueInput: string, studentsArray) {
        this.students = [];
        try {
            studentsArray.forEach(student => {
                let user: User = {} as User;
                user.id = student.id;
                user.displayName = student.name;
                user.toString = () => student.name;
                this.students.push(user);
            });
            this.students = this.students.filter(
                student =>
                    student.displayName.toUpperCase().indexOf(valueInput) > -1 ||
                    student.displayName.toLowerCase().indexOf(valueInput) > -1
            );
        } catch (err) {
            this.students = [];
            throw err;
        }
    }
}