import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Utility methods for consistent UI styling.
 */
public class UIUtils {

    public static Border createStyledTitledBorder(String title) {
        Border lineBorder = BorderFactory.createLineBorder(new Color(150, 180, 210), 1, true);
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                lineBorder, title, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), new Color(40, 80, 120));
        titledBorder.setTitleJustification(TitledBorder.LEFT);
        return BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                titledBorder);
    }

    public static void styleButton(JButton button, Color bg) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }

    public static void styleSmallButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(new Color(100, 150, 200));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
    }
}