import javax.swing.*;
import java.awt.*;

/**
 * Full-screen frame for secondary display.
 */
public class DisplayFrame extends JFrame {
    public DisplayFrame(Rectangle bounds) {
        setUndecorated(true); // No title bar
        setBackground(new Color(20, 30, 45));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        add(new DisplayPanel());
        setBounds(bounds);
        setAlwaysOnTop(true);
        setVisible(true);
    }
}

