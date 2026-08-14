import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

class MainSystemFrame extends JFrame {
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private User currentUser;

    public MainSystemFrame() {
        setTitle("Academic Manager");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        cardPanel.add(createLoginPanel(), "LOGIN");
        add(cardPanel);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField txtUser = new JTextField(15);
        JPasswordField txtPass = new JPasswordField(15);
        JButton btnLogin = new JButton("Login");

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; panel.add(txtUser, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; panel.add(txtPass, gbc);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(btnLogin, gbc);

        btnLogin.addActionListener(e -> {
            try {
                currentUser = UserDAO.authenticate(txtUser.getText(), new String(txtPass.getPassword()));
                if (currentUser != null) {
                    setupDashboard();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
            }
        });
        return panel;
    }

    private void setupDashboard() {
        JPanel dashPanel = new JPanel(new BorderLayout());

        dashPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(new JLabel("  Welcome, " + currentUser.getFullName() + " | Role: " + currentUser.getRole() + "  "));
        toolbar.addSeparator();

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> { currentUser = null; cardLayout.show(cardPanel, "LOGIN"); });
        toolbar.add(btnLogout);
        dashPanel.add(toolbar, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        if (currentUser instanceof Student) {
            tabbedPane.addTab("My Results", createMyResultsPanel());
            tabbedPane.addTab("Performance Analyzer", createPerformancePanel());
            tabbedPane.addTab("My Requests", createStudentRequestPanel());
        } else if (currentUser instanceof Faculty) {
            tabbedPane.addTab("Mark Entry", createMarkEntryPanel());
            tabbedPane.addTab("Manage Requests", createFacultyRequestPanel());
        } else if (currentUser instanceof Admin) {
            tabbedPane.addTab("User Management", createAdminPanel());
        }

        dashPanel.add(tabbedPane, BorderLayout.CENTER);
        cardPanel.add(dashPanel, "DASHBOARD");
        cardLayout.show(cardPanel, "DASHBOARD");
    }

    private JPanel createPerformancePanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JButton btnRefresh = new JButton("Generate Report");
        JTextArea txtReport = new JTextArea();
        txtReport.setEditable(false);

        btnRefresh.addActionListener(e -> {
            PerformanceReport rep = PerformanceAnalyzer.analyze(currentUser.getId());
            txtReport.setText(String.format(
                    "CGPA: %.2f\nAverage Marks: %.2f\nAttendance: %.1f%%\n\nAcademic Status: %s\n\nRecommendation:\n%s",
                    rep.cgpa, rep.averageMarks, rep.attendancePercentage, rep.academicStatus, rep.recommendation
            ));
        });
        panel.add(btnRefresh);
        panel.add(new JScrollPane(txtReport));
        return panel;
    }

    private JPanel createStudentRequestPanel() {
        JPanel p = new JPanel(new BorderLayout());
        String[] cols = {"Type", "Details", "Status", "Response"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);

        JButton btnNew = new JButton("New Request");
        btnNew.addActionListener(e -> {
            try {
                String details = JOptionPane.showInputDialog("Enter request details:");
                if(details != null && !details.isEmpty()) {
                    List<User> facs = UserDAO.getFacultyList();
                    int facId = facs.isEmpty() ? 2 : facs.get(0).getId();
                    AcademicDAO.submitRequest(currentUser.getId(), facId, "General", details);
                    JOptionPane.showMessageDialog(this, "Submitted!");
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        p.add(btnNew, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        try {
            for (Request r : AcademicDAO.getRequestsForStudent(currentUser.getId())) {
                model.addRow(new Object[]{r.getType(), r.getDetails(), r.getStatus(), r.getResponse()});
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return p;
    }

    private JPanel createFacultyRequestPanel() {
        JPanel p = new JPanel(new BorderLayout());
        String[] cols = {"ID", "Type", "Details", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);

        try {
            for (Request r : AcademicDAO.getRequestsForFaculty(currentUser.getId())) {
                model.addRow(new Object[]{r.getId(), r.getType(), r.getDetails(), r.getStatus()});
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        JButton btnRespond = new JButton("Respond to Selected");
        btnRespond.addActionListener(e -> {
            int row = table.getSelectedRow();
            if(row >= 0) {
                int reqId = (int) model.getValueAt(row, 0);
                String resp = JOptionPane.showInputDialog("Enter response:");
                String[] options = {"APPROVED", "REJECTED"};
                int choice = JOptionPane.showOptionDialog(this, "Approve or Reject?", "Status",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

                try {
                    AcademicDAO.updateRequestStatus(reqId, options[choice], resp);
                    JOptionPane.showMessageDialog(this, "Updated! Please re-login to refresh (demo behavior).");
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        });

        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(btnRespond, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createAdminPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField(15);
        JTextField txtUser = new JTextField(15);
        JPasswordField txtPass = new JPasswordField(15);
        JComboBox<String> comboRole = new JComboBox<>(new String[]{"STUDENT", "FACULTY"});
        JButton btnAddUser = new JButton("Add New User");

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1; panel.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; panel.add(txtUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; panel.add(txtPass, gbc);

        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1; panel.add(comboRole, gbc);

        gbc.gridx = 1; gbc.gridy = 4; panel.add(btnAddUser, gbc);

        btnAddUser.addActionListener(e -> {
            try {
                boolean success = UserDAO.addUser(txtUser.getText(), new String(txtPass.getPassword()),
                        comboRole.getSelectedItem().toString(), txtName.getText());
                if(success) {
                    JOptionPane.showMessageDialog(panel, "User Added Successfully!");
                    txtName.setText(""); txtUser.setText(""); txtPass.setText("");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createMyResultsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        String[] cols = {"Course Name", "Midterm", "Final", "Total", "Grade", "GPA"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);

        try {
            for (ResultDTO r : AcademicDAO.getStudentResults(currentUser.getId())) {
                model.addRow(new Object[]{r.courseName, r.midterm, r.finalMarks, r.total, r.grade, r.gpa});
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private JPanel createMarkEntryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JTextField txtStudentId = new JTextField(10);
        JTextField txtCourseId = new JTextField(10);
        JTextField txtMid = new JTextField(10);
        JTextField txtFinal = new JTextField(10);
        JButton btnSubmit = new JButton("Submit Marks");

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Student ID:"), gbc);
        gbc.gridx = 1; panel.add(txtStudentId, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Course ID:"), gbc);
        gbc.gridx = 1; panel.add(txtCourseId, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Midterm Marks:"), gbc);
        gbc.gridx = 1; panel.add(txtMid, gbc);

        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Final Marks:"), gbc);
        gbc.gridx = 1; panel.add(txtFinal, gbc);

        gbc.gridx = 1; gbc.gridy = 4; panel.add(btnSubmit, gbc);

        btnSubmit.addActionListener(e -> {
            try {
                int sId = Integer.parseInt(txtStudentId.getText());
                int cId = Integer.parseInt(txtCourseId.getText());
                double mid = Double.parseDouble(txtMid.getText());
                double fin = Double.parseDouble(txtFinal.getText());

                boolean success = AcademicDAO.enterResult(sId, cId, mid, fin);
                if(success) {
                    JOptionPane.showMessageDialog(panel, "Marks successfully saved!");
                    txtStudentId.setText(""); txtCourseId.setText(""); txtMid.setText(""); txtFinal.setText("");
                } else {
                    JOptionPane.showMessageDialog(panel, "Error: This student is NOT enrolled in this course!", "Enrollment Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Please enter valid numbers!", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return panel;
    }
}