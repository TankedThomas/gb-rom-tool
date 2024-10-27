package com.gare.gbromtool;

import java.sql.SQLException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * This class manages interactions between the UI, ROM data, and the Collection
 * table in the database.
 *
 * @author Thomas Robinson 23191795
 */
public class CollectionManager {
    private final DatabaseQuery dbQuery;
    private final JFrame parentFrame;
    
    public CollectionManager(DatabaseQuery dbQuery, JFrame parentFrame) {
        this.dbQuery = dbQuery;
        this.parentFrame = parentFrame;
    }
    
    /**
     * Saves the current ROM's information to the Collection table.
     * 
     * @param reader The RomReader containing the ROM data
     * @param defaultName Default name to suggest to the user
     * @return true if save was successful, false if cancelled or failed
     */
    public boolean saveRomToCollection(RomReader reader, String defaultName) {
        if (reader == null) {
            showError("Please load a ROM file first.", "No ROM Loaded", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        RomTitle titleInfo = reader.parseTitle();
        String name = promptForName(defaultName);

        // Check if user cancelled
        if (name == null) {
            return false;
        }

        // Validate name length
        if (name.length() > 200) {
            showError("Name is too long. Maximum length is 200 characters.", 
                    "Invalid Name", 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            // Check if ROM already exists
            if (dbQuery.romExistsInCollection(titleInfo.getTitle())) {
                if (!confirmOverwrite()) {
                    return false;
                }
                // TODO: Implement update functionality if needed
                return false;
            }

            boolean success = saveRomData(reader, name, titleInfo);
            if (success) {
                showSuccess("ROM information saved successfully.");
                return true;
            } else {
                showError("Failed to save ROM information.", 
                        "Save Failed", 
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
        } catch (SQLException ex) {
            handleDatabaseError("Database error during save: ", ex);
            return false;
        }
    }
    
    private String promptForName(String defaultName) {
        return JOptionPane.showInputDialog(parentFrame,
                "Enter a name for this ROM:",
                defaultName);
    }
    
    private boolean confirmOverwrite() {
        int choice = JOptionPane.showConfirmDialog(parentFrame,
                "A ROM with this title already exists in the database. Do you want to overwrite it?",
                "ROM Already Exists",
                JOptionPane.YES_NO_OPTION);
        
        return choice == JOptionPane.YES_OPTION;
    }
    
    private boolean saveRomData(RomReader reader, String name, RomTitle titleInfo) throws SQLException {
        return dbQuery.saveRomToCollection(
                name,
                titleInfo.getTitle(),
                reader.getCartridgeType().getBytes(),
                reader.getMaskVersion(),
                reader.getROMSize(),
                reader.getRAMSize(),
                reader.getSGBFlag(),
                reader.getCGBFlag(),
                reader.getDestinationCode(),
                reader.getLicenseeCode(),
                reader.getHeaderChecksum(),
                reader.getGlobalChecksum()
        );
    }
    
    private void showError(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(parentFrame,
                message,
                title,
                messageType);
    }
    
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(parentFrame,
                message,
                "Save Successful",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void handleDatabaseError(String message, SQLException ex) {
        System.err.println(message + ex.getMessage());
        showError("Error saving to database: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
    }
}