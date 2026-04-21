import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import javax.swing.Timer;

/**
 * Panel showing history of queue numbers (generated, called, etc.).
 */
public class QueueHistoryPanel extends JPanel {
    private DefaultTableModel model;
    private JTextField dateField;
    private Timer refreshTimer;

    public QueueHistoryPanel() {
        setBackground(new Color(250, 252, 255));
        setBorder(UIUtils.createStyledTitledBorder("Queue History"));
        setLayout(new BorderLayout(15, 15));

        // Top: filter options
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBackground(new Color(250, 252, 255));
        filterPanel.add(new JLabel(" Date:"));
        dateField = new JTextField("YYYY-MM-DD", 10);
        dateField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filterPanel.add(dateField);
        JButton filterBtn = new JButton("[SEARCH] Filter");
        UIUtils.styleSmallButton(filterBtn);
        filterBtn.addActionListener(e -> refreshTable(dateField.getText()));
        filterPanel.add(filterBtn);
        add(filterPanel, BorderLayout.NORTH);

        // Center: history table
        String[] columns = {"Queue #", "Generated Time", "Called Time", "Status"};
        model = new DefaultTableModel(columns, 0);
        JTable historyTable = new JTable(model);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTable.setRowHeight(28);
        historyTable.setGridColor(new Color(200, 210, 220));
        historyTable.setShowHorizontalLines(true);
        historyTable.setShowVerticalLines(false);

        JTableHeader header = historyTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(180, 200, 230));

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(170, 190, 210), 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // Auto-refresh 10s
        refreshTimer = new Timer(10000, e -> refreshTable(""));
        refreshTimer.start();
        refreshTable(""); // Initial load
    }

    private void refreshTable(String dateFilter) {
        model.setRowCount(0);
        QueueService service = QueueService.getInstance();
        if (!service.isConnected()) {
            model.addRow(new Object[]{"DB Offline", "-", "-", "-"});
            return;
        }
        List<Object[]> history = service.getHistory();
        for (Object[] row : history) {
            model.addRow(row);
        }
    }
}
