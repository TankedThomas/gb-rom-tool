package com.gare.gbromtool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * This class draws the user interface and handles GUI interactions.
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

    // Constants for field names to avoid typos
    private static final String FILE_NAME = "File";
    private static final String TITLE = "Title";
    private static final String CODE = "Code";
    private static final String REVISION = "Revision";
    private static final String PUBLISHER = "Publisher";
    private static final String CART_TYPE = "Cart Type";
    private static final String DESTINATION = "Destination";
    private static final String ROM_SIZE = "ROM Size";
    private static final String RAM_SIZE = "RAM Size";
    private static final String GBC_COMPATIBLE = "Color Compatible";
    private static final String SGB_FUNCTION = "SGB Functionality";
    private static final String HDR_CHKSUM = "Header Checksum";
    private static final String GBL_CHKSUM = "Global Checksum";
    private static final String BOOT_LOGO = "Boot Logo";

    public UserInterface() throws SQLException {
        mainFrame = new JFrame();
        valueLabels = new HashMap<>();
        fileHandler = new FileHandler();
        dbQuery = new DatabaseQuery(DatabaseManager.getInstance());
        collectionManager = new CollectionManager(dbQuery, mainFrame);

        mainFrame.setTitle(Config.TITLE);
        mainFrame.setSize(Config.WINDOW_SIZE_X, Config.WINDOW_SIZE_Y);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLocationRelativeTo(null);

        // Create main container with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loadFileButton = new JButton("Load ROM from File...");
        selectFileButton = new JButton("Load ROM from Database");
        saveFileButton = new JButton("Save ROM to Database");

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
            // TODO: Load selection from list of ROMs currently in Collection table of database
        });
        saveFileButton.addActionListener((ActionEvent e) -> {
            String defaultName = fileHandler.getCurrentFileName().replaceFirst("[.][^.]+$", "");
            collectionManager.saveRomToCollection(fileHandler.getCurrentRomReader(), defaultName);
        });
    }

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

    private void addField(int x, int y, String fieldName,
            GridBagConstraints labelConstraints, GridBagConstraints valueConstraints) {
        labelConstraints.gridx = x;
        labelConstraints.gridy = y;
        labelConstraints.ipadx = 10;

        valueConstraints.gridx = x + 1;
        valueConstraints.gridy = y;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL; // Make values fill space

        // Label-value pairs
        JLabel label = new JLabel(fieldName + ":", JLabel.RIGHT); // Right-align labels
        JLabel value = new JLabel("", JLabel.LEFT); // Left-align values
        value.setFont(value.getFont().deriveFont(Font.PLAIN));
        valueLabels.put(fieldName, value);

        infoPanel.add(label, labelConstraints);
        infoPanel.add(value, valueConstraints);
    }

    private void clearValues() {
        valueLabels.values().forEach(label -> label.setText(""));
    }

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
            valueLabels.get(FILE_NAME).setText(fileHandler.getCurrentFileName());

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
            };

        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
            JOptionPane.showMessageDialog(mainFrame,
                    "Error reading ROM information from database: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatHexByte(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "Unknown";
        }
        return String.format("%02X", bytes[0]).toUpperCase(); // Changed to match your format
    }

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

    public void show() {
        mainFrame.setVisible(true);
    }

}
