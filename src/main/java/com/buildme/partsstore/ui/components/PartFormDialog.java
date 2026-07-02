package com.buildme.partsstore.ui.components;

import com.buildme.partsstore.inventory.Inventory;
import com.buildme.partsstore.model.Category;
import com.buildme.partsstore.model.Part;

import javax.swing.*;
import java.awt.*;

/**
 * Modal form used for both "Add part" and "Edit part".
 * When editing, the id field is locked (ids are the map key in Inventory,
 * so we never rename an existing part - that would effectively create a
 * second entry).
 */
public class PartFormDialog extends JDialog {

    private final JTextField idField = new JTextField(14);
    private final JTextField nameField = new JTextField(18);
    private final JComboBox<Category> categoryBox = new JComboBox<>(Category.values());
    private final JSpinner priceSpinner =
            new JSpinner(new SpinnerNumberModel(0.0d, 0.0d, 1_000_000.0d, 0.5d));
    private final JSpinner stockSpinner =
            new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));
    private final JLabel errorLabel = new JLabel(" ");

    private Part result;

    public PartFormDialog(Frame owner, Inventory inventory, Part existing) {
        super(owner, existing == null ? "Add Part" : "Edit Part", true);
        boolean editing = existing != null;

        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow(form, gbc, row++, "Part ID:", idField);
        addRow(form, gbc, row++, "Name:", nameField);
        addRow(form, gbc, row++, "Category:", categoryBox);
        addRow(form, gbc, row++, "Unit price:", priceSpinner);
        addRow(form, gbc, row++, "Stock:", stockSpinner);

        errorLabel.setForeground(new Color(180, 0, 0));
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        form.add(errorLabel, gbc);

        if (editing) {
            idField.setText(existing.getId());
            idField.setEditable(false);
            idField.setFocusable(false);
            nameField.setText(existing.getName());
            categoryBox.setSelectedItem(existing.getCategory());
            priceSpinner.setValue(existing.getUnitPrice());
            stockSpinner.setValue(existing.getQuantityInStock());
        }

        JButton saveButton = new JButton(editing ? "Save Changes" : "Add Part");
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            result = null;
            dispose();
        });
        saveButton.addActionListener(e -> onSave(inventory, editing));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(saveButton);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(saveButton);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void addRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        form.add(field, gbc);
    }

    private void onSave(Inventory inventory, boolean editing) {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        Category category = (Category) categoryBox.getSelectedItem();
        double price = (Double) priceSpinner.getValue();
        int stock = (Integer) stockSpinner.getValue();

        if (id.isEmpty()) {
            showError("Part ID cannot be empty.");
            return;
        }
        if (name.isEmpty()) {
            showError("Name cannot be empty.");
            return;
        }
        if (!editing && inventory.containsId(id)) {
            showError("A part with ID \"" + id + "\" already exists.");
            return;
        }
        if (price < 0) {
            showError("Unit price cannot be negative.");
            return;
        }

        result = new Part(id, name, category, price, stock);
        dispose();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        pack();
    }

    /**
     * @return the Part the user entered/edited, or null if the dialog was cancelled.
     */
    public Part getResult() {
        return result;
    }
}