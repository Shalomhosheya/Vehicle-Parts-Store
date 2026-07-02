package com.buildme.partsstore.ui;

import com.buildme.partsstore.inventory.Inventory;
import com.buildme.partsstore.model.Category;
import com.buildme.partsstore.model.Part;
import com.buildme.partsstore.pricing.PricingStrategy;
import com.buildme.partsstore.reflection.StrategyRegistry;
import com.buildme.partsstore.store.VehiclePartsStore;
import com.buildme.partsstore.ui.charts.BarChartPanel;
import com.buildme.partsstore.ui.charts.PieChartPanel;
import com.buildme.partsstore.ui.components.PartFormDialog;
import com.buildme.partsstore.ui.components.StatCardPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Swing GUI front-end for the Vehicle Parts Store.
 *
 * This class is purely a presentation layer: it does not contain any
 * pricing, inventory or reflection logic itself. It wires user input
 * to the existing domain classes - Inventory, StrategyRegistry and
 * VehiclePartsStore - which were built and tested independently of
 * this GUI. The console demo in Main still works unchanged, and all
 * JUnit tests still exercise the domain logic directly.
 *
 * Features:
 *   - FlatLaf light/dark theme toggle
 *   - Inventory tab: live search + category filter, Add / Edit / Delete
 *   - Quote tab: price the same order under different pricing strategies
 *   - Dashboard tab: stat cards, stock-level bar chart, value-by-category
 *     pie chart, and a low-stock watchlist
 */
public class GuiApp extends JFrame {

    private final Inventory inventory;
    private final StrategyRegistry strategyRegistry;
    private final VehiclePartsStore store;

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private JTable inventoryTable;
    private DefaultTableModel inventoryTableModel;
    private JTextField searchField;
    private JComboBox<String> categoryFilterBox;
    private JLabel totalValueLabel;

    private JComboBox<String> partComboBox;
    private JComboBox<String> strategyComboBox;
    private JSpinner quantitySpinner;
    private JLabel resultLabel;
    private JTextArea logArea;

    private StatCardPanel totalPartsCard;
    private StatCardPanel totalValueCard;
    private StatCardPanel lowStockCard;
    private BarChartPanel stockChart;
    private PieChartPanel valueByCategoryChart;
    private JList<String> lowStockList;
    private DefaultListModel<String> lowStockListModel;
    private JSpinner thresholdSpinner;

    private boolean darkTheme = false;

    public GuiApp() {
        super("Vehicle Parts Store");

        this.inventory = buildSampleInventory();
        this.strategyRegistry = new StrategyRegistry();
        this.store = new VehiclePartsStore(inventory, strategyRegistry.createStrategy("REGULAR"));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildToolBar(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Inventory", buildInventoryPanel());
        tabs.addTab("Get Quote", buildQuotePanel());
        tabs.addTab("Dashboard", buildDashboardPanel());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 2) {
                refreshDashboard();
            }
        });
        add(tabs, BorderLayout.CENTER);

        refreshAll();
    }

    // Toolbar (theme toggle)

    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JLabel appLabel = new JLabel("Vehicle Parts Store");
        appLabel.setFont(appLabel.getFont().deriveFont(Font.BOLD, 15f));
        bar.add(appLabel);
        bar.add(Box.createHorizontalGlue());

        JToggleButton themeToggle = new JToggleButton("Dark mode");
        themeToggle.addActionListener(e -> {
            darkTheme = themeToggle.isSelected();
            applyTheme();
        });
        bar.add(themeToggle);

        return bar;
    }

    private void applyTheme() {
        try {
            if (darkTheme) {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
            } else {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            // FlatLaf not on the classpath (or failed to load) - keep current look and feel.
            log("[THEME] Could not switch theme: " + ex.getMessage());
        }
    }

    // Inventory tab

    private JPanel buildInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- search / filter bar ---
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.add(new JLabel("Search:"));
        searchField = new JTextField(18);
        searchField.getDocument().addDocumentListener(new SimpleDocListener(this::refreshInventoryTable));
        filterBar.add(searchField);

        filterBar.add(new JLabel("Category:"));
        categoryFilterBox = new JComboBox<>();
        categoryFilterBox.addItem("All");
        for (Category c : Category.values()) {
            categoryFilterBox.addItem(c.toString());
        }
        categoryFilterBox.addActionListener(e -> refreshInventoryTable());
        filterBar.add(categoryFilterBox);

        panel.add(filterBar, BorderLayout.NORTH);

        // --- table ---
        String[] columns = {"ID", "Name", "Category", "Unit Price", "Stock", "Value"};
        inventoryTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        inventoryTable = new JTable(inventoryTableModel);
        inventoryTable.setRowHeight(24);
        inventoryTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);

        // --- bottom bar: total value + CRUD buttons ---
        JPanel bottom = new JPanel(new BorderLayout());
        totalValueLabel = new JLabel();
        totalValueLabel.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        totalValueLabel.setFont(totalValueLabel.getFont().deriveFont(Font.BOLD));
        bottom.add(totalValueLabel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        JButton addButton = new JButton("Add Part");
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");
        addButton.addActionListener(e -> onAddPart());
        editButton.addActionListener(e -> onEditPart());
        deleteButton.addActionListener(e -> onDeletePart());
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        bottom.add(buttons, BorderLayout.EAST);

        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private List<Part> filteredParts() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String categoryFilter = categoryFilterBox == null ? "All" : (String) categoryFilterBox.getSelectedItem();

        return inventory.allParts().stream()
                .filter(p -> query.isEmpty()
                        || p.getId().toLowerCase(Locale.ROOT).contains(query)
                        || p.getName().toLowerCase(Locale.ROOT).contains(query))
                .filter(p -> categoryFilter == null || categoryFilter.equals("All")
                        || p.getCategory().toString().equals(categoryFilter))
                .collect(Collectors.toList());
    }

    private void refreshInventoryTable() {
        inventoryTableModel.setRowCount(0);
        for (Part part : filteredParts()) {
            double value = part.getUnitPrice() * part.getQuantityInStock();
            inventoryTableModel.addRow(new Object[]{
                    part.getId(),
                    part.getName(),
                    part.getCategory(),
                    String.format("%.2f", part.getUnitPrice()),
                    part.getQuantityInStock(),
                    String.format("%.2f", value)
            });
        }
        if (totalValueLabel != null) {
            totalValueLabel.setText(String.format("Total inventory value: %.2f", inventory.totalInventoryValue()));
        }
    }

    private Part getSelectedPart() {
        int row = inventoryTable.getSelectedRow();
        if (row < 0) return null;
        String id = (String) inventoryTableModel.getValueAt(row, 0);
        return inventory.findById(id).orElse(null);
    }

    private void onAddPart() {
        PartFormDialog dialog = new PartFormDialog(this, inventory, null);
        dialog.setVisible(true);
        Part newPart = dialog.getResult();
        if (newPart != null) {
            inventory.addPart(newPart);
            log("[ADD] " + newPart.getId() + " - " + newPart.getName());
            refreshAll();
        }
    }

    private void onEditPart() {
        Part selected = getSelectedPart();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a part to edit first.",
                    "No part selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PartFormDialog dialog = new PartFormDialog(this, inventory, selected);
        dialog.setVisible(true);
        Part updated = dialog.getResult();
        if (updated != null) {
            inventory.addPart(updated); // same id -> overwrites existing entry
            log("[EDIT] " + updated.getId() + " - " + updated.getName());
            refreshAll();
        }
    }

    private void onDeletePart() {
        Part selected = getSelectedPart();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a part to delete first.",
                    "No part selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete \"" + selected.getName() + "\" (" + selected.getId() + ")?",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            inventory.removePart(selected.getId());
            log("[DELETE] " + selected.getId() + " - " + selected.getName());
            refreshAll();
        }
    }

    // ---------------------------------------------------------------
    // Quote tab
    // ---------------------------------------------------------------

    private JPanel buildQuotePanel() {
        JPanel outer = new JPanel(new BorderLayout(10, 10));
        outer.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Get a Quote"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        partComboBox = new JComboBox<>();

        strategyComboBox = new JComboBox<>();
        for (String key : strategyRegistry.availableStrategies()) {
            strategyComboBox.addItem(key);
        }

        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));

        JButton quoteButton = new JButton("Get Quote");
        quoteButton.addActionListener(e -> onGetQuote());

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Part:"), gbc);
        gbc.gridx = 1; gbc.gridy = row; form.add(partComboBox, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1; gbc.gridy = row; form.add(quantitySpinner, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Pricing strategy:"), gbc);
        gbc.gridx = 1; gbc.gridy = row; form.add(strategyComboBox, gbc);

        row++;
        gbc.gridx = 1; gbc.gridy = row; form.add(quoteButton, gbc);

        resultLabel = new JLabel(" ");
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 16f));
        resultLabel.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));

        logArea = new JTextArea(10, 24);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel rightPanel = new JPanel(new BorderLayout(4, 4));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Activity Log"));
        rightPanel.add(resultLabel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        outer.add(form, BorderLayout.WEST);
        outer.add(rightPanel, BorderLayout.CENTER);
        return outer;
    }

    private void refreshPartComboBox() {
        String previousSelection = (String) partComboBox.getSelectedItem();
        partComboBox.removeAllItems();
        for (Part part : inventory.allParts()) {
            partComboBox.addItem(part.getId() + " - " + part.getName());
        }
        if (previousSelection != null) {
            partComboBox.setSelectedItem(previousSelection);
        }
    }

    private void onGetQuote() {
        int selectedIndex = partComboBox.getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }
        Part selectedPart = inventory.allParts().get(selectedIndex);
        int quantity = (Integer) quantitySpinner.getValue();
        String strategyKey = (String) strategyComboBox.getSelectedItem();

        try {
            // Reflection: strategy is created dynamically by key via StrategyRegistry.
            PricingStrategy strategy = strategyRegistry.createStrategy(strategyKey);
            store.setPricingStrategy(strategy);

            double price = store.quote(selectedPart.getId(), quantity);

            resultLabel.setForeground(new Color(0, 130, 0));
            resultLabel.setText(String.format("Total: %.2f  (%d x %s @ %s)",
                    price, quantity, selectedPart.getName(), strategyKey));

            log(String.format("[%s] %d x %s -> %.2f", strategyKey, quantity,
                    selectedPart.getName(), price));

        } catch (IllegalStateException | NoSuchElementException | IllegalArgumentException ex) {
            resultLabel.setForeground(new Color(190, 0, 0));
            resultLabel.setText("Cannot price this order: " + ex.getMessage());
            log("[ERROR] " + ex.getMessage());
        }
    }

    private void log(String message) {
        if (logArea != null) {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    // ---------------------------------------------------------------
    // Dashboard tab
    // ---------------------------------------------------------------

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        // stat cards
        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0));
        totalPartsCard = new StatCardPanel("Total Parts", "0", new Color(64, 132, 214));
        totalValueCard = new StatCardPanel("Total Value", "0.00", new Color(80, 170, 120));
        lowStockCard = new StatCardPanel("Low Stock Items", "0", new Color(214, 90, 64));
        cards.add(wrapCard(totalPartsCard));
        cards.add(wrapCard(totalValueCard));
        cards.add(wrapCard(lowStockCard));
        panel.add(cards, BorderLayout.NORTH);

        // charts
        JPanel charts = new JPanel(new GridLayout(1, 2, 10, 0));
        stockChart = new BarChartPanel();
        valueByCategoryChart = new PieChartPanel();
        charts.add(wrapCard(stockChart));
        charts.add(wrapCard(valueByCategoryChart));

        // low stock watchlist
        JPanel watchlistPanel = new JPanel(new BorderLayout(4, 4));
        watchlistPanel.setBorder(BorderFactory.createTitledBorder("Low Stock Watchlist"));

        JPanel thresholdBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        thresholdBar.add(new JLabel("Alert threshold:"));
        thresholdSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_LOW_STOCK_THRESHOLD, 0, 1000, 1));
        thresholdSpinner.addChangeListener(e -> refreshDashboard());
        thresholdBar.add(thresholdSpinner);
        watchlistPanel.add(thresholdBar, BorderLayout.NORTH);

        lowStockListModel = new DefaultListModel<>();
        lowStockList = new JList<>(lowStockListModel);
        watchlistPanel.add(new JScrollPane(lowStockList), BorderLayout.CENTER);

        JPanel centerSplit = new JPanel(new BorderLayout(10, 10));
        centerSplit.add(charts, BorderLayout.CENTER);
        centerSplit.add(watchlistPanel, BorderLayout.SOUTH);

        panel.add(centerSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel wrapCard(JComponent content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private void refreshDashboard() {
        if (totalPartsCard == null) return; // dashboard not built yet

        int threshold = (Integer) thresholdSpinner.getValue();
        List<Part> lowStock = inventory.lowStockParts(threshold);

        totalPartsCard.setValue(String.valueOf(inventory.size()));
        totalValueCard.setValue(String.format("%.2f", inventory.totalInventoryValue()));
        lowStockCard.setValue(String.valueOf(lowStock.size()));

        Map<String, Double> stockData = new LinkedHashMap<>();
        for (Part p : inventory.allParts()) {
            stockData.put(p.getName(), (double) p.getQuantityInStock());
        }
        stockChart.setLowThreshold(threshold);
        stockChart.setData(stockData, "Stock levels by part");

        Map<String, Double> valueByCategory = new LinkedHashMap<>();
        for (Map.Entry<Category, Double> entry : inventory.totalValueByCategory().entrySet()) {
            valueByCategory.put(entry.getKey().toString(), entry.getValue());
        }
        valueByCategoryChart.setData(valueByCategory, "Inventory value by category");

        lowStockListModel.clear();
        if (lowStock.isEmpty()) {
            lowStockListModel.addElement("No parts below threshold - all good!");
        } else {
            for (Part p : lowStock) {
                lowStockListModel.addElement(String.format("%s - %s (%d in stock)",
                        p.getId(), p.getName(), p.getQuantityInStock()));
            }
        }
    }

    // ---------------------------------------------------------------
    // Shared refresh
    // ---------------------------------------------------------------

    private void refreshAll() {
        refreshInventoryTable();
        refreshPartComboBox();
        refreshDashboard();
    }

    // ---------------------------------------------------------------
    // Sample data + entry point
    // ---------------------------------------------------------------

    private static Inventory buildSampleInventory() {
        Inventory inventory = new Inventory();
        // Engine parts
        inventory.addPart(new Part("P-ENG-001", "Alloy Piston Set", Category.ENGINE, 89.99, 40));
        inventory.addPart(new Part("P-ENG-002", "Timing Belt Kit", Category.ENGINE, 54.50, 15));
        inventory.addPart(new Part("P-ENG-003", "Cylinder Head", Category.ENGINE, 220.00, 5));
        inventory.addPart(new Part("P-ENG-004", "Engine Oil Pump", Category.ENGINE, 75.25, 10));
        inventory.addPart(new Part("P-ENG-005", "Spark Plug Set", Category.ENGINE, 18.99, 50));

        // Brake parts
        inventory.addPart(new Part("P-BRK-001", "Ceramic Brake Pads", Category.BRAKES, 32.00, 60));
        inventory.addPart(new Part("P-BRK-002", "Brake Disc Rotor", Category.BRAKES, 45.75, 3));
        inventory.addPart(new Part("P-BRK-003", "Brake Caliper", Category.BRAKES, 85.00, 6));
        inventory.addPart(new Part("P-BRK-004", "Brake Fluid Bottle", Category.BRAKES, 12.50, 25));

        // Electrical parts
        inventory.addPart(new Part("P-ELE-001", "12V Car Battery", Category.ELECTRICAL, 110.00, 8));
        inventory.addPart(new Part("P-ELE-002", "Alternator", Category.ELECTRICAL, 145.20, 2));
        inventory.addPart(new Part("P-ELE-003", "Starter Motor", Category.ELECTRICAL, 130.00, 4));
        inventory.addPart(new Part("P-ELE-004", "Headlight Bulb", Category.ELECTRICAL, 9.99, 100));
        inventory.addPart(new Part("P-ELE-005", "Fuse Kit", Category.ELECTRICAL, 6.50, 75));

        // Body parts
        inventory.addPart(new Part("P-BOD-001", "Wing Mirror (Left)", Category.BODY, 28.99, 4));
        inventory.addPart(new Part("P-BOD-002", "Wing Mirror (Right)", Category.BODY, 28.99, 6));
        inventory.addPart(new Part("P-BOD-003", "Front Bumper", Category.BODY, 150.00, 2));
        inventory.addPart(new Part("P-BOD-004", "Rear Bumper", Category.BODY, 145.00, 3));
        inventory.addPart(new Part("P-BOD-005", "Car Door Handle", Category.BODY, 12.75, 20));

        // Paint / accessories
        inventory.addPart(new Part("P-IOD-001", "Matt Black Paint Bottle", Category.PAINT, 2.99, 4));
        inventory.addPart(new Part("P-IOD-002", "Gloss White Paint", Category.PAINT, 3.50, 10));
        inventory.addPart(new Part("P-IOD-003", "Car Polish Kit", Category.PAINT, 15.00, 12));
        inventory.addPart(new Part("P-IOD-004", "Scratch Remover", Category.PAINT, 8.99, 18));

        // Tyres
        inventory.addPart(new Part("P-TYR-001", "All-Season Tyre 195/65R15", Category.TYRES, 55.00, 10));
        inventory.addPart(new Part("P-TYR-002", "Performance Tyre 225/40R18", Category.TYRES, 95.00, 6));
        inventory.addPart(new Part("P-TYR-003", "All-Season Tyre 205/55R16", Category.TYRES, 65.00, 4));
        inventory.addPart(new Part("P-TYR-004", "Winter Tyre 205/60R16", Category.TYRES, 70.00, 5));


        return inventory;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            } catch (Exception ex) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // fall back to default look and feel
                }
            }
            new GuiApp().setVisible(true);
        });
    }

    /**
     * Small helper so we don't need three anonymous DocumentListener methods
     * every time we just want "run this on any text change".
     */
    private static class SimpleDocListener implements javax.swing.event.DocumentListener {
        private final Runnable action;

        SimpleDocListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) { action.run(); }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) { action.run(); }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
    }
}