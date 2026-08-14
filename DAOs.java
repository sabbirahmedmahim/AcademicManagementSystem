import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class UserDAO {
    public static User authenticate(String username, String password) throws SQLException {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                String role = rs.getString("role");
                String name = rs.getString("full_name");
                return switch (role) {
                    case "ADMIN" -> new Admin(id, username, name);
                    case "FACULTY" -> new Faculty(id, username, name);
                    case "STUDENT" -> new Student(id, username, name);
                    default -> null;
                };
            }
        }
        return null;
    }


    public static boolean addUser(String username, String password, String role, String fullName) throws SQLException {
        String query = "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.setString(4, fullName);
            return stmt.executeUpdate() > 0;
        }
    }
    public static List<User> getFacultyList() throws SQLException {
        List<User> faculties = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE role='FACULTY'")) {
            while (rs.next()) faculties.add(new Faculty(rs.getInt("id"), rs.getString("username"), rs.getString("full_name")));
        }
        return faculties;
    }
}

class AcademicDAO {
    public static void submitRequest(int studentId, int facultyId, String type, String details) throws SQLException {
        String query = "INSERT INTO requests (student_id, faculty_id, type, details) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, studentId); stmt.setInt(2, facultyId); stmt.setString(3, type); stmt.setString(4, details);
            stmt.executeUpdate();
        }
    }

    public static List<Request> getRequestsForStudent(int studentId) throws SQLException {
        List<Request> reqs = new ArrayList<>();
        String query = "SELECT * FROM requests WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reqs.add(new Request(rs.getInt("id"), rs.getInt("student_id"), rs.getInt("faculty_id"),
                        rs.getString("type"), rs.getString("details"), rs.getString("status"), rs.getString("faculty_response")));
            }
        }
        return reqs;
    }

    public static List<Request> getRequestsForFaculty(int facultyId) throws SQLException {
        List<Request> reqs = new ArrayList<>();
        String query = "SELECT * FROM requests WHERE faculty_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, facultyId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) reqs.add(new Request(rs.getInt("id"), rs.getInt("student_id"), rs.getInt("faculty_id"),
                    rs.getString("type"), rs.getString("details"), rs.getString("status"), rs.getString("faculty_response")));
        }
        return reqs;
    }

    public static void updateRequestStatus(int reqId, String status, String response) throws SQLException {
        String query = "UPDATE requests SET status = ?, faculty_response = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, status); stmt.setString(2, response); stmt.setInt(3, reqId);
            stmt.executeUpdate();
        }
    }

    public static List<ResultDTO> getStudentResults(int studentId) throws SQLException {
        List<ResultDTO> list = new ArrayList<>();
        String query = "SELECT c.course_name, r.midterm_marks, r.final_marks, r.total_marks, r.gpa, r.grade " +
                "FROM results r JOIN enrollments e ON r.enrollment_id = e.id " +
                "JOIN courses c ON e.course_id = c.id WHERE e.student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                list.add(new ResultDTO(rs.getString("course_name"), rs.getDouble("midterm_marks"),
                        rs.getDouble("final_marks"), rs.getDouble("total_marks"),
                        rs.getDouble("gpa"), rs.getString("grade")));
            }
        }
        return list;
    }


    public static boolean enterResult(int studentId, int courseId, double mid, double fin) throws SQLException {
        int enrollmentId = -1;
        String enrQuery = "SELECT id FROM enrollments WHERE student_id = ? AND course_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(enrQuery)) {
            stmt.setInt(1, studentId); stmt.setInt(2, courseId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) enrollmentId = rs.getInt("id");
        }

        if (enrollmentId == -1) return false;


        double total = mid + fin;
        double gpa = 0.0; String grade = "F";
        if(total >= 80) { gpa = 4.0; grade = "A+"; }
        else if(total >= 75) { gpa = 3.75; grade = "A"; }
        else if(total >= 70) { gpa = 3.50; grade = "A-"; }
        else if(total >= 65) { gpa = 3.25; grade = "B+"; }
        else if(total >= 60) { gpa = 3.00; grade = "B"; }
        else if(total >= 50) { gpa = 2.50; grade = "C"; }
        else if(total >= 40) { gpa = 2.00; grade = "D"; }


        String checkQuery = "SELECT id FROM results WHERE enrollment_id = ?";
        boolean exists = false;
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
            stmt.setInt(1, enrollmentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) exists = true;
        }

        String finalQuery = exists ?
                "UPDATE results SET midterm_marks=?, final_marks=?, total_marks=?, gpa=?, grade=? WHERE enrollment_id=?" :
                "INSERT INTO results (midterm_marks, final_marks, total_marks, gpa, grade, enrollment_id) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(finalQuery)) {
            stmt.setDouble(1, mid); stmt.setDouble(2, fin); stmt.setDouble(3, total);
            stmt.setDouble(4, gpa); stmt.setString(5, grade); stmt.setInt(6, enrollmentId);
            stmt.executeUpdate();
        }
        return true;
    }
}