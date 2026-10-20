
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final List<CustomerQuery> queries =
            new ArrayList<>();

    private static RoundRobinScheduler scheduler;

    private static DefaultTableModel tableModel;
    private static JTable queryTable;

    private static JLabel totalValue;
    private static JLabel waitingValue;
    private static JLabel progressValue;
    private static JLabel resolvedValue;

    private static JSpinner quantumSpinner;
    private static JButton addButton;
    private static JButton startButton;

    private static GanttChartPanel ganttChart;
    private static ClassicGanttChartPanel classicGanttChart;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createDashboard);
    }

    private static void createDashboard() {
        JFrame frame = new JFrame("Customer Service Center");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1250, 900);
        frame.setMinimumSize(new Dimension(950, 650));
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(245, 247, 250));

        JLabel title = new JLabel(
                "Customer Service Center",
                SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(new Color(31, 41, 55));

        JLabel subtitle = new JLabel(
                "Round Robin Query Scheduling System",
                SwingConstants.CENTER);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 15));

        JPanel header = new JPanel(new GridLayout(2, 1, 5, 5));
        header.setOpaque(false);
        header.add(title);
        header.add(subtitle);

        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 12, 12));
        statsPanel.setOpaque(false);

        JPanel totalCard = createCard("Total Queries");
        JPanel waitingCard = createCard("Waiting");
        JPanel progressCard = createCard("In Progress");
        JPanel resolvedCard = createCard("Resolved");

        totalValue = getCardValue(totalCard);
        waitingValue = getCardValue(waitingCard);
        progressValue = getCardValue(progressCard);
        resolvedValue = getCardValue(resolvedCard);

        statsPanel.add(totalCard);
        statsPanel.add(waitingCard);
        statsPanel.add(progressCard);
        statsPanel.add(resolvedCard);

        String[] columns = {
                "Query ID",
                "Customer",
                "Issue Type",
                "Description",
                "Assigned Agent",
                "Required (min)",
                "Remaining (min)",
                "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        queryTable = new JTable(tableModel);
        queryTable.setRowHeight(28);
        queryTable.setFont(new Font("Arial", Font.PLAIN, 12));
        queryTable.getTableHeader().setFont(
                new Font("Arial", Font.BOLD, 12));
        queryTable.setAutoResizeMode(
                JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane tableScroll = new JScrollPane(queryTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder(
                "Customer Query Queue"));

        // Existing multi-agent chart.
        ganttChart = new GanttChartPanel();

        // New classic-style chart.
        classicGanttChart = new ClassicGanttChartPanel();

        // Show both charts in separate tabs.
        JTabbedPane chartTabs = new JTabbedPane();

        chartTabs.addTab(
                "Multi-Agent Gantt Chart",
                new JScrollPane(ganttChart));

        chartTabs.addTab(
                "Classic Gantt Chart",
                new JScrollPane(classicGanttChart));

        // Table above; chart tabs below.
        JSplitPane workArea = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                tableScroll,
                chartTabs);

        workArea.setResizeWeight(0.50);
        workArea.setOneTouchExpandable(true);
        workArea.setContinuousLayout(true);
        workArea.setDividerSize(8);
        workArea.setBorder(null);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(statsPanel, BorderLayout.NORTH);
        centerPanel.add(workArea, BorderLayout.CENTER);

        quantumSpinner = new JSpinner(
                new SpinnerNumberModel(2, 1, 120, 1));

        JPanel quantumPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT));
        quantumPanel.setOpaque(false);
        quantumPanel.add(
                new JLabel("Time Quantum (minutes):"));
        quantumPanel.add(quantumSpinner);

        addButton = new JButton("Add Query");
        startButton = new JButton("Start Scheduling");
        JButton resetButton = new JButton("Reset");

        addButton.addActionListener(
                e -> showAddQueryForm(frame));

        startButton.addActionListener(
                e -> startScheduling(frame));

        resetButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    frame,
                    "Clear all queries and reset the simulation?",
                    "Confirm Reset",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                queries.clear();
                tableModel.setRowCount(0);
                scheduler = null;

                ganttChart.clearSchedule();
                classicGanttChart.clearSchedule();

                addButton.setEnabled(true);
                startButton.setEnabled(true);
                quantumSpinner.setEnabled(true);

                updateStatistics();
            }
        });

        JPanel buttonsPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonsPanel.setOpaque(false);
        buttonsPanel.add(addButton);
        buttonsPanel.add(startButton);
        buttonsPanel.add(resetButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(quantumPanel, BorderLayout.WEST);
        bottomPanel.add(buttonsPanel, BorderLayout.EAST);

        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);

        updateStatistics();
        frame.setVisible(true);
    }

    private static JPanel createCard(String title) {
        JPanel card = new JPanel(new GridLayout(2, 1, 5, 5));
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        JLabel valueLabel = new JLabel("0");
        valueLabel.setFont(new Font("Arial", Font.BOLD, 26));
        valueLabel.setForeground(new Color(37, 99, 235));

        card.add(titleLabel);
        card.add(valueLabel);
        card.putClientProperty("valueLabel", valueLabel);

        return card;
    }

    private static JLabel getCardValue(JPanel card) {
        return (JLabel) card.getClientProperty("valueLabel");
    }

    private static void showAddQueryForm(JFrame parent) {
        JTextField nameField = new JTextField(20);

        String[] issueTypes = {
                "Delivery",
                "Payment",
                "Refund",
                "Order Cancellation",
                "Product Issue",
                "Other"
        };

        JComboBox<String> issueCombo =
                new JComboBox<>(issueTypes);

        JTextArea descriptionArea = new JTextArea(4, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        JScrollPane descriptionScroll =
                new JScrollPane(descriptionArea);

        JSpinner handlingSpinner = new JSpinner(
                new SpinnerNumberModel(5, 1, 120, 1));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(7, 7, 7, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        addFormRow(form, gbc, 0, "Customer Name:", nameField);
        addFormRow(form, gbc, 1, "Issue Type:", issueCombo);
        addFormRow(form, gbc, 2, "Description:", descriptionScroll);
        addFormRow(
                form, gbc, 3, "Handling Time (min):",
                handlingSpinner);

        int result = JOptionPane.showConfirmDialog(
                parent,
                form,
                "Add Customer Query",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText().trim();
        String issue = (String) issueCombo.getSelectedItem();
        String description = descriptionArea.getText().trim();
        int burstTime = (Integer) handlingSpinner.getValue();

        if (name.isEmpty() || description.isEmpty()) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Enter both the customer name and issue description.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            CustomerQuery query = new CustomerQuery(
                    name, issue, description, burstTime);

            queries.add(query);
            addQueryToTable(query);
            updateStatistics();

            JOptionPane.showMessageDialog(
                    parent,
                    "Query " + query.getQueryId()
                            + " added successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                    parent,
                    ex.getMessage(),
                    "Invalid Query",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private static void addFormRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String label,
            JComponent component) {

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);
    }

    private static void addQueryToTable(CustomerQuery query) {
        tableModel.addRow(new Object[]{
                query.getQueryId(),
                query.getCustomerName(),
                query.getIssueType(),
                query.getDescription(),
                query.getAssignedAgent(),
                query.getBurstTime(),
                query.getRemainingTime(),
                query.getStatus()
        });
    }

    private static void startScheduling(JFrame frame) {
        if (queries.isEmpty()) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Please add customer queries first.",
                    "No Queries",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int quantum = (Integer) quantumSpinner.getValue();

        scheduler = new RoundRobinScheduler(quantum);

        scheduler.addAgent(new Agent("A01", "Agent 1"));
        scheduler.addAgent(new Agent("A02", "Agent 2"));
        scheduler.addAgent(new Agent("A03", "Agent 3"));

        try {
            for (CustomerQuery query : queries) {
                query.reset();
                scheduler.addQuery(query);
            }

            // The scheduling algorithm remains unchanged.
            scheduler.runSimulation();

            refreshTable();
            updateStatistics();

            // Update both charts using the same execution log.
            List<String> log = scheduler.getExecutionLog();
            List<Agent> agents = scheduler.getAgents();
            int totalTime = scheduler.getCurrentTime();

            ganttChart.setSchedule(log, agents, totalTime);

           classicGanttChart.setSchedule(
    queries,
    scheduler.getTimeQuantum()
);
            addButton.setEnabled(false);
            startButton.setEnabled(false);
            quantumSpinner.setEnabled(false);

            showSchedulingReport(frame);

        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(
                    frame,
                    ex.getMessage(),
                    "Scheduling Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void refreshTable() {
        tableModel.setRowCount(0);

        for (CustomerQuery query : queries) {
            addQueryToTable(query);
        }
    }

    private static void updateStatistics() {
        int waiting = 0;
        int inProgress = 0;
        int resolved = 0;

        for (CustomerQuery query : queries) {
            switch (query.getStatus()) {
                case "Waiting":
                    waiting++;
                    break;
                case "In Progress":
                    inProgress++;
                    break;
                case "Resolved":
                    resolved++;
                    break;
                default:
                    break;
            }
        }

        totalValue.setText(String.valueOf(queries.size()));
        waitingValue.setText(String.valueOf(waiting));
        progressValue.setText(String.valueOf(inProgress));
        resolvedValue.setText(String.valueOf(resolved));
    }

    private static void showSchedulingReport(JFrame frame) {
        StringBuilder report = new StringBuilder();

        report.append("ROUND ROBIN SCHEDULING RESULTS\n");
        report.append("========================================\n");

        report.append("Time Quantum: ")
                .append(scheduler.getTimeQuantum())
                .append(" minutes\n");

        report.append("Number of Agents: ")
                .append(scheduler.getAgents().size())
                .append("\n");

        report.append("Total Simulated Time: ")
                .append(scheduler.getCurrentTime())
                .append(" minutes\n");

        report.append("Resolved Queries: ")
                .append(scheduler.getResolvedCount())
                .append("\n");

        report.append(String.format(
                "Average Waiting Time: %.2f minutes%n",
                scheduler.getAverageWaitingTime()));

        report.append(String.format(
                "Average Turnaround Time: %.2f minutes%n",
                scheduler.getAverageTurnaroundTime()));

        report.append("\nEXECUTION LOG\n");
        report.append("----------------------------------------\n");

        for (String entry : scheduler.getExecutionLog()) {
            report.append(entry).append("\n");
        }

        report.append("\nQUERY PERFORMANCE\n");
        report.append("----------------------------------------\n");

        for (CustomerQuery query : queries) {
            report.append(query.getQueryId())
                    .append(" | Waiting: ")
                    .append(scheduler.getWaitingTime(query))
                    .append(" min | Turnaround: ")
                    .append(scheduler.getTurnaroundTime(query))
                    .append(" min | Completion: ")
                    .append(scheduler.getCompletionTime(query))
                    .append(" min\n");
        }

        JTextArea reportArea =
                new JTextArea(report.toString(), 24, 70);

        reportArea.setEditable(false);
        reportArea.setFont(
                new Font("Monospaced", Font.PLAIN, 13));
        reportArea.setCaretPosition(0);

        JOptionPane.showMessageDialog(
                frame,
                new JScrollPane(reportArea),
                "Scheduling Report",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
