package com.buildme.partsstore.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * A small rounded "stat card" showing a title, a big value, and an accent colour.
 * Used on the Dashboard tab (Total Parts / Total Value / Low Stock Count).
 */
public class StatCardPanel extends JPanel {

    private final JLabel valueLabel;
    private final Color accent;

    public StatCardPanel(String title, String initialValue, Color accent) {
        this.accent = accent;
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setOpaque(false);

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 11f));
        titleLabel.setForeground(new Color(120, 120, 120));

        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 26f));
        valueLabel.setForeground(accent);

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
        g2.fillRoundRect(0, 0, 5, getHeight(), 14, 14);
        g2.dispose();
        super.paintComponent(g);
    }
}