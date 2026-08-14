import java.sql.*;

class PerformanceAnalyzer {


    public static PerformanceReport analyze(int studentId) {
        PerformanceReport report = new PerformanceReport();
        double totalMarks = 0, totalGpa = 0;
        int courseCount = 0;
        int presentCount = 0, totalClasses = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String resQuery = "SELECT * FROM results r JOIN enrollments e ON r.enrollment_id = e.id WHERE e.student_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(resQuery)) {
                stmt.setInt(1, studentId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    totalMarks += rs.getDouble("total_marks");
                    totalGpa += rs.getDouble("gpa");
                    courseCount++;
                }
            }


            String attQuery = "SELECT a.status FROM attendance a JOIN enrollments e ON a.enrollment_id = e.id WHERE e.student_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(attQuery)) {
                stmt.setInt(1, studentId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    totalClasses++;
                    if ("PRESENT".equalsIgnoreCase(rs.getString("status"))) presentCount++;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        report.averageMarks = courseCount > 0 ? (totalMarks / courseCount) : 0;
        report.cgpa = courseCount > 0 ? (totalGpa / courseCount) : 0;
        report.attendancePercentage = totalClasses > 0 ? ((double) presentCount / totalClasses) * 100 : 0;


        if (report.cgpa >= 3.75) {
            report.academicStatus = "EXCELLENT";
            report.recommendation = "Outstanding performance. Consider competitive programming or teaching assistant roles.";
        } else if (report.cgpa >= 3.00) {
            report.academicStatus = "GOOD";
            report.recommendation = "Solid foundation. Focus on core algorithms to push to the next tier.";
        } else if (report.cgpa >= 2.00) {
            report.academicStatus = "AVERAGE";
            report.recommendation = "Requires focus. Utilize study flashcards and review past concepts.";
        } else {
            report.academicStatus = "AT_RISK";
            report.recommendation = "Critical. Please schedule a meeting with your academic advisor immediately.";
        }

        if (report.attendancePercentage < 70 && totalClasses > 0) {
            report.recommendation += " WARNING: Attendance is below the 70% threshold.";
        }

        return report;
    }
}