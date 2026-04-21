import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel for live queue operations: current number, waiting list, and action buttons.
 */
public class LiveQueuePanel extends JPanel {
    private JLabel numberDisplay;
    private JButton serveBtn;
    private DefaultListModel<String> waitingModel;
    private DefaultListModel<String> nextModel;
    private JList<String> waitingList;
    private JList<String> nextList;
    private Timer refreshTimer;
    private String currentQueueNum = "-";

    public LiveQueuePanel() {
        setBackground(new Color(250, 252, 255));
        setBorder(UIUtils.createStyledTitledBorder("Live Queue"));

        setLayout(new BorderLayout(20, 20));

        // ===== NORTH: Current Number Display (WIDENED) =====
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(250, 252, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel currentLabel = new JLabel("NOW SERVING");
        currentLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        currentLabel.setForeground(new Color(70, 130, 200));
        gbc.gridx = 0; gbc.gridy = 0;
        centerPanel.add(currentLabel, gbc);

        numberDisplay = new JLabel("-");
        numberDisplay.setFont(new Font("Segoe UI", Font.BOLD, 88));
        numberDisplay.setForeground(new Color(30, 60, 90));
        numberDisplay.setBackground(Color.WHITE);
        numberDisplay.setOpaque(true);
        numberDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        numberDisplay.setPreferredSize(new Dimension(340, 200));
        numberDisplay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(170, 190, 210), 4),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        gbc.gridy = 1;
        centerPanel.add(numberDisplay, gbc);

        serveBtn = new JButton("[SERVE] Done");
        UIUtils.styleButton(serveBtn, new Color(0, 150, 136));
        serveBtn.setEnabled(false);
        serveBtn.addActionListener(e -> onServe());
        gbc.gridy = 2;
        gbc.insets = new Insets(5, 10, 10, 10);
        centerPanel.add(serveBtn, gbc);
        add(centerPanel, BorderLayout.NORTH);

        // ===== CENTER: Lists =====
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(new Color(250, 252, 255));
        rightPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(170, 190, 210)),
                "Current Waiting (Max 5)",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(40, 80, 120)));

        waitingModel = new DefaultListModel<>();
        waitingList = new JList<>(waitingModel);
        waitingList.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        waitingList.setFixedCellHeight(30);
        waitingList.setBorder(new EmptyBorder(5, 10, 5, 10));
        JScrollPane listScroll = new JScrollPane(waitingList);
        listScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 220)));
        listScroll.getViewport().setBackground(Color.WHITE);
        listScroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        rightPanel.add(listScroll, BorderLayout.CENTER);

        JPanel nextBatchPanel = new JPanel(new BorderLayout(10, 10));
        nextBatchPanel.setBackground(new Color(250, 252, 255));
        nextBatchPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(170, 190, 210)),
                "Next Batch (Standby)",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(40, 80, 120)));

        nextModel = new DefaultListModel<>();
        nextList = new JList<>(nextModel);
        nextList.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        nextList.setFixedCellHeight(30);
        nextList.setBorder(new EmptyBorder(5, 10, 5, 10));
        JScrollPane nextScroll = new JScrollPane(nextList);
        nextScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 220)));
        nextScroll.getViewport().setBackground(Color.WHITE);
        nextScroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        nextBatchPanel.add(nextScroll, BorderLayout.CENTER);

        JPanel queueListsContainer = new JPanel(new GridLayout(1, 2, 10, 0));
        queueListsContainer.setBackground(new Color(250, 252, 255));
        queueListsContainer.add(rightPanel);
        queueListsContainer.add(nextBatchPanel);
        add(queueListsContainer, BorderLayout.CENTER);

        // ===== SOUTH: Action Buttons =====
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(new Color(250, 252, 255));

        JButton generateBtn = new JButton("Generate Number");
        UIUtils.styleButton(generateBtn, new Color(70, 130, 200));
        generateBtn.addActionListener(e -> onGenerate());

        JButton callNextBtn = new JButton("Call Next");
        UIUtils.styleButton(callNextBtn, new Color(46, 125, 50));
        callNextBtn.addActionListener(e -> onCallNext());

        JButton resetBtn = new JButton("Reset Queue");
        UIUtils.styleButton(resetBtn, new Color(200, 70, 70));
        resetBtn.addActionListener(e -> onReset());

        bottomPanel.add(generateBtn);
        bottomPanel.add(callNextBtn);
        bottomPanel.add(resetBtn);


add(bottomPanel, BorderLayout.SOUTH);

        // Refreshers
        refreshTimer = new Timer(3000, e -> refreshData());
        refreshTimer.start();
        refreshData(); // Initial
    }

    private void refreshData() {
        QueueService service = QueueService.getInstance();
        if (!service.isConnected()) {
            numberDisplay.setText("DB Offline");
            waitingModel.clear();
            nextModel.clear();
            return;
        }

        // Update lists
        waitingModel.clear();
        List<String> waiting = service.getWaitingQueue();
        for (String num : waiting) {
            waitingModel.addElement(num);
        }

        nextModel.clear();
        List<String> nextBatch = service.getNextBatch(5);
        for (String num : nextBatch) {
            nextModel.addElement(num);
        }

        // Current calling
        List<String> calling = service.getCallingQueue();
        if (!calling.isEmpty()) {
            currentQueueNum = calling.get(0);
            numberDisplay.setText(currentQueueNum);
            numberDisplay.setForeground(new Color(46, 125, 50)); // Green
        } else {
            currentQueueNum = "-";
            numberDisplay.setText(currentQueueNum);
            numberDisplay.setForeground(new Color(30, 60, 90));
        }
        serveBtn.setEnabled(!currentQueueNum.equals("-"));

    }

    private void onGenerate() {
        QueueService service = QueueService.getInstance();
        if (service.isConnected()) {
            service.generateQueueNum();
            refreshData();
        }
    }

    private void onCallNext() {
        QueueService service = QueueService.getInstance();
        if (service.isConnected()) {
            service.callNext();
            refreshData();
        }
    }

    private void onReset() {
        QueueService service = QueueService.getInstance();
        service.resetQueue();
        refreshData();
    }

    private void onServe() {
        if (currentQueueNum.equals("-")) return;
        QueueService service = QueueService.getInstance();
        service.serveCurrent(currentQueueNum);
        refreshData();
    }
}


