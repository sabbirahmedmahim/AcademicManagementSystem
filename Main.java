import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Button.arc", 15);
            UIManager.put("Component.arc", 15);
            UIManager.put("TextComponent.arc", 15);
            java.awt.Font modernFont = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14);
            UIManager.put("defaultFont", modernFont);
            UIManager.put("Component.focusColor", new java.awt.Color(0, 150, 136));
            UIManager.put("Component.focusedBorderColor", new java.awt.Color(0, 150, 136));
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf. Falling back to default.");
        }
        SwingUtilities.invokeLater(() -> {
            MainSystemFrame frame = new MainSystemFrame();
            frame.setVisible(true);
        });
    }
}