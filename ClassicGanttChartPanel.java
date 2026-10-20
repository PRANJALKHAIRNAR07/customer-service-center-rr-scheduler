
import javax.swing.*;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ClassicGanttChartPanel extends JPanel {

    private static final int LEFT = 25;
    private static final int RIGHT = 25;

    private static final Color[] COLORS = {
        new Color(37, 99, 235),
        new Color(13, 148, 136),
        new Color(147, 51, 234),
        new Color(234, 88, 12),
        new Color(219, 39, 119),
        new Color(22, 163, 74),
        new Color(79, 70, 229),
        new Color(8, 145, 178)
    };

    private final List<ProcessInfo> processes = new ArrayList<>();
    private final List<Slice> slices = new ArrayList<>();
    private final Map<String, Color> colors = new LinkedHashMap<>();

    private int timeQuantum;
    private int totalTime;
    private boolean hasSchedule;

    public ClassicGanttChartPanel() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder(
            "Classic Round Robin Gantt Chart"));

        setPreferredSize(new Dimension(1000, 300));
        setMinimumSize(new Dimension(500, 230));
    }

    /**
     * Builds a traditional, single-server Round Robin schedule.
     * This visualization does not modify the main multi-agent scheduler.
     */
    public void setSchedule(
            List<CustomerQuery> queries,
            int timeQuantum) {

        processes.clear();
        slices.clear();
        colors.clear();

        if (queries == null || queries.isEmpty()
                || timeQuantum <= 0) {
            hasSchedule = false;
            repaint();
            return;
        }

        this.timeQuantum = timeQuantum;

        Queue<ProcessInfo> readyQueue = new ArrayDeque<>();

        // Preserve the order in which queries were added.
        for (CustomerQuery query : queries) {
            if (query == null || query.getBurstTime() <= 0) {
                continue;
            }

            ProcessInfo process = new ProcessInfo(
                query.getQueryId(),
                query.getBurstTime()
            );

            processes.add(process);
            readyQueue.offer(process);

            colors.put(
                process.id,
                COLORS[(colors.size()) % COLORS.length]
            );
        }

        int clock = 0;

        // Simulate the traditional single-server Round Robin algorithm.
        while (!readyQueue.isEmpty()) {
            ProcessInfo process = readyQueue.poll();

            int duration = Math.min(
                timeQuantum,
                process.remaining
            );

            int start = clock;
            clock += duration;

            slices.add(new Slice(
                process.id,
                start,
                clock,
                duration
            ));

            process.remaining -= duration;

            if (process.remaining > 0) {
                readyQueue.offer(process);
            }
        }

        totalTime = clock;
        hasSchedule = !slices.isEmpty();

        // Keep blocks readable when there are many time slices.
        int minimumWidth = Math.max(
            1000,
            LEFT + totalTime * 35 + RIGHT
        );

        setPreferredSize(new Dimension(minimumWidth, 300));

        revalidate();
        repaint();
    }

    public void clearSchedule() {
        processes.clear();
        slices.clear();
        colors.clear();

        totalTime = 0;
        timeQuantum = 0;
        hasSchedule = false;

        setPreferredSize(new Dimension(1000, 300));

        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        if (!hasSchedule) {
            g.setColor(Color.GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.drawString(
                "Run scheduling to display the classic Gantt chart.",
                LEFT, 45
            );
            g.dispose();
            return;
        }

        int width = getWidth() - LEFT - RIGHT;

        if (width <= 0 || totalTime <= 0) {
            g.dispose();
            return;
        }

        double scale = width / (double) totalTime;

        drawProcessTable(g);
        drawGanttChart(g, scale, width);

        g.dispose();
    }

    private void drawProcessTable(Graphics2D g) {
        int x = LEFT;
        int y = 28;
        int tableWidth = getWidth() - LEFT - RIGHT;
        int rowHeight = 24;
        int columnWidth = tableWidth / 2;

        g.setFont(new Font("Arial", Font.BOLD, 12));

        g.setColor(new Color(255, 245, 245));
        g.fillRect(x, y, tableWidth, rowHeight);

        g.setColor(new Color(180, 180, 180));
        g.drawRect(x, y, tableWidth, rowHeight);

        g.setColor(new Color(160, 30, 30));
        g.drawString("Process", x + 12, y + 16);
        g.drawString("Burst Time", x + columnWidth + 12, y + 16);

        g.drawLine(
            x + columnWidth, y,
            x + columnWidth, y + rowHeight
        );

        for (int i = 0; i < processes.size(); i++) {
            ProcessInfo process = processes.get(i);
            int rowY = y + rowHeight * (i + 1);

            g.setColor(i % 2 == 0
                ? Color.WHITE
                : new Color(250, 250, 250));

            g.fillRect(x, rowY, tableWidth, rowHeight);

            g.setColor(new Color(190, 190, 190));
            g.drawRect(x, rowY, tableWidth, rowHeight);

            g.drawLine(
                x + columnWidth, rowY,
                x + columnWidth, rowY + rowHeight
            );

            g.setColor(new Color(130, 35, 35));
            g.setFont(new Font("Arial", Font.PLAIN, 12));

            g.drawString(process.id, x + 12, rowY + 16);

            g.drawString(
                String.valueOf(process.burst),
                x + columnWidth + 12,
                rowY + 16
            );
        }
    }

    private void drawGanttChart(
            Graphics2D g,
            double scale,
            int chartWidth) {

        int tableBottom = 28 + 24 * (processes.size() + 1);
        int titleY = tableBottom + 25;
        int barY = titleY + 25;
        int barHeight = 38;
        int axisY = barY + barHeight + 18;

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString(
            "Round Robin Scheduling - Gantt Chart",
            LEFT, titleY
        );

        // Draw each execution slice as a separate block.
        for (Slice slice : slices) {
            int x1 = LEFT + (int) Math.round(slice.start * scale);
            int x2 = LEFT + (int) Math.round(slice.end * scale);
            int blockWidth = Math.max(1, x2 - x1);

            g.setColor(colors.get(slice.processId));
            g.fillRect(x1, barY, blockWidth, barHeight);

            g.setColor(Color.WHITE);
            g.drawRect(x1, barY, blockWidth, barHeight);

            String label = slice.processId;

            g.setFont(new Font("Arial", Font.PLAIN, 11));
            FontMetrics fm = g.getFontMetrics();

            if (blockWidth >= fm.stringWidth(label) + 8) {
                g.drawString(
                    label,
                    x1 + (blockWidth - fm.stringWidth(label)) / 2,
                    barY + 23
                );
            }
        }

        // Draw the horizontal time axis.
        g.setColor(Color.BLACK);
        g.drawLine(
            LEFT, axisY,
            LEFT + chartWidth, axisY
        );

        // Put a time label at every execution boundary.
        g.setFont(new Font("Arial", Font.PLAIN, 11));

        for (Slice slice : slices) {
            drawBoundary(g, slice.start, axisY, scale);
        }

        drawBoundary(g, totalTime, axisY, scale);

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString("0", LEFT - 3, axisY + 17);

        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString(
            "Time quantum: " + timeQuantum
                + " minutes     Total sequential time: "
                + totalTime + " minutes",
            LEFT, axisY + 38
        );
    }

    private void drawBoundary(
            Graphics2D g,
            int time,
            int axisY,
            double scale) {

        int x = LEFT + (int) Math.round(time * scale);

        g.setColor(Color.BLACK);
        g.drawLine(x, axisY - 4, x, axisY + 4);

        String label = String.valueOf(time);
        FontMetrics fm = g.getFontMetrics();

        g.drawString(
            label,
            x - fm.stringWidth(label) / 2,
            axisY + 17
        );
    }

    private static class ProcessInfo {
        final String id;
        final int burst;
        int remaining;

        ProcessInfo(String id, int burst) {
            this.id = id;
            this.burst = burst;
            this.remaining = burst;
        }
    }

    private static class Slice {
        final String processId;
        final int start;
        final int end;
        final int duration;

        Slice(String processId, int start, int end, int duration) {
            this.processId = processId;
            this.start = start;
            this.end = end;
            this.duration = duration;
        }
    }
}
