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
    private boolean testMode = false;
    private boolean testConfirmationResult = false;  // Default response for testing

    public CollectionManager(DatabaseQuery dbQuery, JFrame parentFrame) {
        this.dbQuery = dbQuery;
        this.parentFrame = parentFrame;
    }

    // Test helper methods
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public void setTestConfirmationResult(boolean result) {
        this.testConfirmationResult = result;
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

        String name = promptForName(defaultName);
        if (name == null) {
            return false;  // User cancelled
        }

        try {
            Collection rom = Collection.fromRomReader(reader, name);

            // Check if ROM already exists
            if (dbQuery.romExistsInCollection(
                    rom.getTitle(),
                    rom.getRomRev(),
                    rom.getGlobalChecksum())) {

                if (!confirmOverwrite()) {
                    return false;
                }

                // Update existing ROM
                boolean success = dbQuery.updateRomInCollection(rom);
                if (success) {
                    showSuccess("ROM information updated successfully.");
                    return true;
                } else {
                    showError("Failed to update ROM information.", "Update Failed", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }

            // Save new ROM
            boolean success = dbQuery.saveRomToCollection(rom);
            if (success) {
                showSuccess("ROM information saved successfully.");
                return true;
            } else {
                showError("Failed to save ROM information.", "Save Failed", JOptionPane.ERROR_MESSAGE);
                return false;
            }

        } catch (IllegalArgumentException e) {
            showError(e.getMessage(), "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (SQLException ex) {
            handleDatabaseError("Database error during save: ", ex);
            return false;
        }
    }

    private boolean confirmOverwrite() {
        if (testMode) {
            return testConfirmationResult;
        }

        int choice = JOptionPane.showConfirmDialog(parentFrame,
                "This ROM version already exists in the database. Do you want to update it?",
                "ROM Version Already Exists", JOptionPane.YES_NO_OPTION);

        return choice == JOptionPane.YES_OPTION;
    }

    private String promptForName(String defaultName) {
        if (testMode) {
            return defaultName;  // In test mode, just return the default name
        }

        String name = JOptionPane.showInputDialog(parentFrame, "Enter a name for this ROM:", defaultName);

        // Check length here before creating Collection object
        if (name != null && name.length() > Collection.MAX_NAME_LENGTH) {
            showError("Name is too long. Maximum length is " + Collection.MAX_NAME_LENGTH
                    + " characters.", "Invalid Name", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        return name;
    }

    private void showError(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(parentFrame, message, title, messageType);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(parentFrame, message, "Save Successful", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleDatabaseError(String message, SQLException ex) {
        System.err.println(message + ex.getMessage());
        showError("Error saving to database: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}
