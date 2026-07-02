package com.buildme.partsstore.ui.charts;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal, dependency-free bar chart. Renders a Map of label -> value as
 * vertical bars. Bars below the given lowThreshold are drawn in a warning
 * colour so low-stock parts stand out at a glance.
 */
public class BarChartPanel extends JPanel {

    private Map<String, Double> data = new LinkedHashMap<>();
    private String title = "";
    private double lowThreshold = -1;
    private Color barColor = new Color(64, 132, 214);
    private Color warningColor = new Color(214, 90, 64);

    public void setData(Map<String, Double> data, String title) {
        this.data = data;
        this.title = title;
        repaint();
    }

    public void setLowThreshold(double lowThreshold) {
        this.lowThreshold = lowThreshold;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int marginLeft = 46;
        int marginBottom = 60;
        int marginTop = 30;
        int marginRight = 16;

        g2.setColor(getForeground());
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
        g2.drawString(title, 10, 18);

        if (data.isEmpty()) {
            g2.dispose();
            return;
        }

        double max = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        if (max <= 0) max = 1;

        int chartWidth = width - marginLeft - marginRight;
        int chartHeight = height - marginTop - marginBottom;
        int n = data.size();
        int gap = 10;
        int barWidth = Math.max(6, (chartWidth - gap * (n + 1)) / Math.max(n, 1));

        // axis
        g2.setColor(new Color(200, 200, 200));
        g2.drawLine(marginLeft, marginTop, marginLeft, marginTop + chartHeight);
        g2.drawLine(marginLeft, marginTop + chartHeight, marginLeft + chartWidth, marginTop + chartHeight);

        int x = marginLeft + gap;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            double value = entry.getValue();
            int barHeight = (int) Math.round((value / max) * (chartHeight - 10));
            int y = marginTop + chartHeight - barHeight;

            boolean low = lowThreshold >= 0 && value < lowThreshold;
            g2.setColor(low ? warningColor : barColor);
            g2.fillRoundRect(x, y, barWidth, barHeight, 6, 6);

            g2.setColor(getForeground());
            String valueStr = value == Math.floor(value) ? String.valueOf((int) value)
                    : String.format("%.1f", value);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(valueStr, x + barWidth / 2 - fm.stringWidth(valueStr) / 2, y - 4);

            // rotated label
            String label = entry.getKey();
            if (label.length() > 12) label = label.substring(0, 11) + "…";
            Graphics2D labelG = (Graphics2D) g2.create();
            labelG.translate(x + barWidth / 2 + 4, marginTop + chartHeight + 10);
            labelG.rotate(Math.toRadians(45));
            labelG.drawString(label, 0, 0);
            labelG.dispose();

            x += barWidth + gap;
        }

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 260);
    }
}