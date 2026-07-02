package com.buildme.partsstore.ui.charts;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal, dependency-free pie chart with a legend. Renders a Map of
 * label -> value as proportional wedges.
 */
public class PieChartPanel extends JPanel {

    private static final Color[] PALETTE = {
            new Color(64, 132, 214), new Color(214, 90, 64), new Color(80, 170, 120),
            new Color(230, 175, 46), new Color(150, 100, 200), new Color(90, 190, 190),
            new Color(220, 120, 170), new Color(130, 130, 130)
    };

    private Map<String, Double> data = new LinkedHashMap<>();
    private String title = "";

    public void setData(Map<String, Double> data, String title) {
        this.data = data;
        this.title = title;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getForeground());
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
        g2.drawString(title, 10, 18);

        double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
        if (data.isEmpty() || total <= 0) {
            g2.dispose();
            return;
        }

        int diameter = Math.min(getWidth() - 170, getHeight() - 50);
        diameter = Math.max(diameter, 60);
        int cx = 30;
        int cy = 34;

        double startAngle = 90;
        int colorIndex = 0;
        int legendY = cy;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            double sweep = (entry.getValue() / total) * 360.0;
            Color color = PALETTE[colorIndex % PALETTE.length];
            g2.setColor(color);
            g2.fillArc(cx, cy, diameter, diameter, (int) Math.round(startAngle), -(int) Math.round(sweep));
            startAngle -= sweep;

            // legend
            int legendX = cx + diameter + 24;
            g2.setColor(color);
            g2.fillRect(legendX, legendY, 10, 10);
            g2.setColor(getForeground());
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
            double pct = (entry.getValue() / total) * 100;
            g2.drawString(String.format("%s (%.0f%%)", entry.getKey(), pct), legendX + 16, legendY + 10);
            legendY += 20;

            colorIndex++;
        }

        g2.setColor(getBackground() == null ? Color.WHITE : getBackground());
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 260);
    }
}