package com.gare.gbromtool;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Dialog for selecting a ROM from the Collection table.
 * Displays a scrollable list of ROMs and allows the user to select one.
 *
 * @author Thomas Robinson 23191795
 */
public class RomSelectionDialog extends JDialog {

    private final JList<Collection> romList;
    private final DefaultListModel<Collection> listModel;
    private Collection selectedRom = null;
    private final DatabaseQuery dbQuery;

    public RomSelectionDialog(JFrame parent, DatabaseQuery dbQuery) {
        super(parent, "Select ROM from Database", true);
        this.dbQuery = dbQuery;

        // Create list model and JList
        listModel = new DefaultListModel<>();
        romList = new JList<>(listModel);
        romList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom cell renderer to display ROM information
        romList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                Collection rom = (Collection) value;
                String display = String.format("%s (%s)",
                        rom.getName(),
                        rom.getTitle().trim()
                );
                return super.getListCellRendererComponent(
                        list, display, index, isSelected, cellHasFocus);
            }
        });

        // Add components to dialog
        setLayout(new BorderLayout(10, 10));
        add(new JScrollPane(romList), BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton selectButton = new JButton("Select");
        JButton cancelButton = new JButton("Cancel");

        selectButton.addActionListener(e -> {
            selectedRom = romList.getSelectedValue();
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Set dialog properties
        setSize(400, 300);
        setLocationRelativeTo(parent);

        // Load ROMs
        refreshRomList();
    }

    /**
     * Refreshes the list of ROMs from the database.
     * Updates the display with current database contents.
     */
    public final void refreshRomList() {
        listModel.clear();
        try {
            ArrayList<Collection> roms = dbQuery.getAllRoms();
            roms.forEach(listModel::addElement);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading ROMs from database: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Shows the dialog and returns the selected ROM entry.
     *
     * @return the selected RomEntry, or null if cancelled
     */
    public Collection showDialog() {
        setVisible(true);
        return selectedRom;
    }
}
