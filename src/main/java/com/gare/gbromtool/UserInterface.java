package com.gare.gbromtool;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

/**
 * Draws the user interface and handles GUI interactions.
 * Manages communication between UI components, database, and ROM data.
 *
 * @author Thomas Robinson 23191795
 */
public class UserInterface {

    private final JFrame mainFrame;
    private final JButton loadFileButton;
    private final JButton selectFileButton;
    private final JButton saveFileButton;
    private final JPanel infoPanel;
    private final Map<String, JLabel> valueLabels;
    private final FileHandler fileHandler;
    private final DatabaseQuery dbQuery;
    private final CollectionManager collectionManager;

    // GUI constants
    public static final String WINDOW_TITLE = "GB ROM Tool";
    public static final int WINDOW_SIZE_X = 800;
    public static final int WINDOW_SIZE_Y = 400;

    // Field name constants
    public static final String FILE_NAME = "File";
    public static final String TITLE = "Title";
    public static final String CODE = "Code";
    public static final String REVISION = "Revision";
    public static final String PUBLISHER = "Publisher";
    public static final String CART_TYPE = "Cart Type";
    public static final String DESTINATION = "Destination";
    public static final String ROM_SIZE = "ROM Size";
    public static final String RAM_SIZE = "RAM Size";
    public static final String GBC_COMPATIBLE = "Color Compatible";
    public static final String SGB_FUNCTION = "SGB Functionality";
    public static final String HDR_CHKSUM = "Header Checksum";
    public static final String GBL_CHKSUM = "Global Checksum";
    public static final String BOOT_LOGO = "Boot Logo";

    /**
     * Initialises UI components and database connections.
     *
     * @throws SQLException if database initialisation fails
     */
    public UserInterface() throws SQLException {
        mainFrame = new JFrame();
        valueLabels = new HashMap<>();
        fileHandler = new FileHandler();
        dbQuery = new DatabaseQuery(DatabaseManager.getInstance());
        collectionManager = new CollectionManager(dbQuery, mainFrame);

        mainFrame.setTitle(WINDOW_TITLE);
        mainFrame.setSize(WINDOW_SIZE_X, WINDOW_SIZE_Y);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLocationRelativeTo(null);

        // Create main container with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loadFileButton = new JButton("Load from File...");
        selectFileButton = new JButton("Load from Database...");
        saveFileButton = new JButton("Save to Database...");

        topPanel.add(loadFileButton);
        topPanel.add(selectFileButton);
        topPanel.add(saveFileButton);

        // Info panel with GridBagLayout for precise control
        infoPanel = new JPanel(new GridBagLayout());
        initialiseInfoPanel();

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(infoPanel, BorderLayout.CENTER);

        mainFrame.add(mainPanel);

        // Add listeners
        loadFileButton.addActionListener((ActionEvent e) -> {
            handleFileSelection();
        });

        selectFileButton.addActionListener((ActionEvent e) -> {
            handleDatabaseSelection();
        });

        saveFileButton.addActionListener((ActionEvent e) -> {
            handleFileSave();
        });

    }

    /**
     * Initialises the information panel with all required fields.
     * Sets up layout and label-value pairs for ROM information display.
     */
    private void initialiseInfoPanel() {
        // Top panel spacing
//        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0)); // Reduce from 10

        // Label-value pair constraints
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(2, 5, 2, 5);

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.anchor = GridBagConstraints.WEST;
        valueConstraints.weightx = 1.0;
        valueConstraints.insets = new Insets(2, 5, 2, 15);

        // Row 0
        // Filename
        addField(0, 0, FILE_NAME, labelConstraints, valueConstraints);

        // Row 1
        // Title and Manufacturer Code
        addField(0, 1, TITLE, labelConstraints, valueConstraints);
        addField(2, 1, CODE, labelConstraints, valueConstraints);

        // Row 2
        // Revision and Publisher
        addField(0, 2, REVISION, labelConstraints, valueConstraints);
        addField(2, 2, PUBLISHER, labelConstraints, valueConstraints);

        // Row 3
        // Cartridge Type and Destination
        addField(0, 3, CART_TYPE, labelConstraints, valueConstraints);
        addField(2, 3, DESTINATION, labelConstraints, valueConstraints);

        // Row 4
        // RAM/ROM Sizes
        addField(0, 4, ROM_SIZE, labelConstraints, valueConstraints);
        addField(2, 4, RAM_SIZE, labelConstraints, valueConstraints);

        // Row 5
        // Compatibility flags
        addField(0, 5, GBC_COMPATIBLE, labelConstraints, valueConstraints);
        addField(2, 5, SGB_FUNCTION, labelConstraints, valueConstraints);

        // Row 6
        // Checksums
        addField(0, 6, HDR_CHKSUM, labelConstraints, valueConstraints);
        addField(2, 6, GBL_CHKSUM, labelConstraints, valueConstraints);

        // Row 7
        // Boot Logo
        addField(0, 7, BOOT_LOGO, labelConstraints, valueConstraints);

        // Clear all values initially
        clearValues();
    }

    /**
     * Adds a field to the information panel.
     *
     * @param x grid x position
     * @param y grid y position
     * @param fieldName name of the field
     * @param labelConstraints constraints for label layout
     * @param valueConstraints constraints for value layout
     */
    private void addField(int x, int y, String fieldName,
            GridBagConstraints labelConstraints, GridBagConstraints valueConstraints) {
        labelConstraints.gridx = x;
        labelConstraints.gridy = y;
        labelConstraints.ipadx = 10;

        valueConstraints.gridx = x + 1;
        valueConstraints.gridy = y;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL; // Make values fill space

        // Make filename value span both columns
        if (fieldName.equals(FILE_NAME)) {
            valueConstraints.gridwidth = 3;  // Span across both columns
        } else {
            valueConstraints.gridwidth = 1;  // Default width
        }

        // Label-value pairs
        JLabel label = new JLabel(fieldName + ":", JLabel.RIGHT); // Right-align labels
        JLabel value = new JLabel("", JLabel.LEFT); // Left-align values
        value.setFont(value.getFont().deriveFont(Font.PLAIN));
        valueLabels.put(fieldName, value);

        infoPanel.add(label, labelConstraints);
        infoPanel.add(value, valueConstraints);
    }

    /**
     * Clears all displayed values from the interface.
     */
    private void clearValues() {
        valueLabels.values().forEach(label -> label.setText(""));
    }

    /**
     * Updates all displayed values with data from a ROM.
     *
     * @param reader RomReader containing ROM data
     */
    private void updateValues(RomReader reader) {
        if (reader == null) {
            clearValues();
            return;
        }

        try {
            // Basic info
            RomTitle titleInfo = reader.parseTitle();
            valueLabels.get(TITLE).setText(titleInfo.getTitle());
            valueLabels.get(CODE).setText(titleInfo.hasManufacturerCode()
                    ? "CGB-" + titleInfo.getManufacturerCode() : "");

            // Filename
            if (reader.getLoadFromDatabase()) {
                String dbName = reader.getDatabaseName();
                System.out.println("File name retrieved from database: '" + dbName + "'");
                valueLabels.get(FILE_NAME).setText(dbName);
            } else {
                String fileName = fileHandler.getCurrentFileName();
                System.out.println("File name retrieved from file: '" + fileName + "'");
                valueLabels.get(FILE_NAME).setText(fileName);
            }

            // Revision and Publisher
            valueLabels.get(REVISION).setText(formatHexByte(reader.getMaskVersion()));
            valueLabels.get(PUBLISHER).setText(dbQuery.getLicenseeCode(reader.getLicenseeCode()));

            // Cartridge and Destination
            valueLabels.get(CART_TYPE).setText(dbQuery.getCartridgeTypeDescription(
                    formatHexByte(reader.getCartridgeType().getBytes())));
            valueLabels.get(DESTINATION).setText(formatDestination(reader.getDestinationCode()));

            // Sizes
            valueLabels.get(ROM_SIZE).setText(dbQuery.getROMSizeInKiB(reader.getROMSize()));
            valueLabels.get(RAM_SIZE).setText(dbQuery.getRAMSizeInKiB(reader.getRAMSize()));

            // Compatibility flags
            valueLabels.get(GBC_COMPATIBLE).setText(formatGBCFlag(reader.getCGBFlag()));
            valueLabels.get(SGB_FUNCTION).setText(reader.getSGBFlag() ? "Yes" : "No");

            // Header Checksum
            String headerStatus = reader.verifyHeaderChecksum() ? "Valid" : "Invalid";
            valueLabels.get(HDR_CHKSUM).setText(reader.getStoredHeaderChecksum() + " (" + headerStatus + ")");

            // Global Checksum
            String globalStatus = reader.verifyGlobalChecksum() ? "Valid" : "Invalid";
            valueLabels.get(GBL_CHKSUM).setText(reader.getStoredGlobalChecksum() + " (" + globalStatus + ")");

            // Boot Logo
            boolean isValid = reader.getLogo();
            valueLabels.get(BOOT_LOGO).setText(isValid ? "Valid" : "Invalid");
            if (isValid) {
                valueLabels.get(BOOT_LOGO).setForeground(new Color(0, 150, 0));
            } else {
                valueLabels.get(BOOT_LOGO).setForeground(new Color(150, 0, 0));
            }

        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
            JOptionPane.showMessageDialog(mainFrame,
                    "Error reading ROM information from database: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Formats a byte array into a hexadecimal string.
     * Handles null or empty arrays by returning "Unknown".
     *
     * @param bytes the byte array to format
     * @return formatted hexadecimal string or "Unknown" if invalid input
     */
    private String formatHexByte(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "Unknown";
        }
        return String.format("%02X", bytes[0]).toUpperCase(); // Changed to match your format
    }

    /**
     * Formats a Destination code into a human-readable string.
     * Includes both description and hex code.
     *
     * @param code the destination code to format
     * @return formatted string describing region and hex code
     */
    private String formatDestination(int code) {
        return switch (code) {
            case 0x00 ->
                "Japan (0x00)";
            case 0x01 ->
                "International (0x01)";
            default ->
                String.format("Unknown (0x%02X)", code);
        };
    }

    /**
     * Formats a CGB Compatibility flag into a human-readable string.
     * Includes both compatibility level and hex code when applicable.
     *
     * @param flag the CGB Compatibility flag to format
     * @return formatted string describing compatibility status
     */
    private String formatGBCFlag(byte[] flag) {
        if (flag == null || flag.length == 0) {
            return "No";
        }
        return switch (flag[0]) {
            case (byte) 0x80 ->
                "Yes (0x80)";
            case (byte) 0xC0 ->
                "Only (0xC0)";
            default ->
                "No";
        };
    }

    /**
     * Handles selection and loading of ROM files.
     * Displays appropriate error messages if loading fails.
     */
    private void handleFileSelection() {
        FileHandler.FileOperationResult result = fileHandler.selectFile(mainFrame);

        switch (result) {
            case SUCCESS -> {
                updateValues(fileHandler.getCurrentRomReader());
            }

            case INVALID_FILE_TYPE -> {
                JOptionPane.showMessageDialog(mainFrame,
                        "Invalid file type. Please select a .gb or .gbc file.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            case READ_ERROR -> {
                JOptionPane.showMessageDialog(mainFrame,
                        "Error reading ROM file.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Handles loading a ROM from the database through the selection dialog.
     */
    private void handleDatabaseSelection() {
        RomSelectionDialog dialog = new RomSelectionDialog(mainFrame, dbQuery);
        Collection selectedRom = dialog.showDialog();

        if (selectedRom != null) {
            System.out.println("Selected ROM name: '" + selectedRom.getName() + "'"); // Debug
            // Create RomReader from Collection table in database
            RomReader dbReader = new RomReader(selectedRom);
            System.out.println("Created reader with database name: '" + dbReader.getDatabaseName() + "'"); // Debug

            // Update display
            clearValues();
            updateValues(dbReader);
        }
    }

    /**
     * Handles saving current ROM data to the database.
     */
    private void handleFileSave() {
        String defaultName = fileHandler.getCurrentFileName().replaceFirst("[.][^.]+$", "");
        collectionManager.saveRomToCollection(fileHandler.getCurrentRomReader(), defaultName);
    }

    /**
     * Makes the main window visible.
     */
    public void show() {
        mainFrame.setVisible(true);
    }

}
