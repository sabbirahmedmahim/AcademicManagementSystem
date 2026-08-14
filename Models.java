import java.util.Date;
abstract class User {
    private int id;
    private String username, role, fullName;

    public User(int id, String username, String role, String fullName) {
        this.id = id; this.username = username; this.role = role; this.fullName = fullName;
    }
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
}

class Student extends User {
    public Student(int id, String username, String fullName) { super(id, username, "STUDENT", fullName); }
}

class Faculty extends User {
    public Faculty(int id, String username, String fullName) { super(id, username, "FACULTY", fullName); }
}

class Admin extends User {
    public Admin(int id, String username, String fullName) { super(id, username, "ADMIN", fullName); }
}

class Course {
    private int id; private String code, name; private int credits;
    public Course(int id, String code, String name, int credits) {
        this.id = id; this.code = code; this.name = name; this.credits = credits;
    }
    public int getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}

class Request {
    private int id, studentId, facultyId;
    private String type, details, status, response;
    public Request(int id, int sId, int fId, String type, String details, String status, String response) {
        this.id = id; this.studentId = sId; this.facultyId = fId; this.type = type;
        this.details = details; this.status = status; this.response = response;
    }
    public int getId() { return id; }
    public String getType() { return type; }
    public String getDetails() { return details; }
    public String getStatus() { return status; }
    public String getResponse() { return response; }
}

class PerformanceReport {
    public double averageMarks, cgpa, attendancePercentage;
    public String academicStatus, recommendation;
}
class ResultDTO {
    public String courseName, grade;
    public double midterm, finalMarks, total, gpa;

    public ResultDTO(String courseName, double midterm, double finalMarks, double total, double gpa, String grade) {
        this.courseName = courseName; this.midterm = midterm; this.finalMarks = finalMarks;
        this.total = total; this.gpa = gpa; this.grade = grade;
    }
}