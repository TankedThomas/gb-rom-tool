package com.gare.gbromtool;

import java.sql.SQLException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Manages interactions between the UI, ROM data, and the Collection
 * table in the database.
 * Handles saving and updating ROM information while providing user feedback.
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

    /**
     * Enables test mode for automated testing.
     * When enabled, skips user interaction dialogs.
     *
     * @param testMode true to enable test mode
     */
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    /**
     * Sets the result to return in test mode for confirmation dialogs.
     *
     * @param result true to simulate user confirmation
     */
    public void setTestConfirmationResult(boolean result) {
        this.testConfirmationResult = result;
    }

    /**
     * Saves the current ROM's information to the Collection table.
     * Prompts user for name and handles duplicate detection.
     *
     * @param reader the RomReader containing the ROM data
     * @param defaultName default name to suggest to the user
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
                    dbQuery.printAllRoms(); // For debugging
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
                dbQuery.printAllRoms(); // For debugging
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

    /**
     * Displays a confirmation dialog for overwriting existing ROM data.
     * In test mode, returns the preset test result.
     *
     * @return true if user confirms overwrite
     */
    private boolean confirmOverwrite() {
        if (testMode) {
            return testConfirmationResult;
        }

        int choice = JOptionPane.showConfirmDialog(parentFrame,
                "This ROM version already exists in the database. Do you want to update it?",
                "ROM Version Already Exists", JOptionPane.YES_NO_OPTION);

        return choice == JOptionPane.YES_OPTION;
    }

    /**
     * Prompts the user for a ROM name.
     * Validates input length and handles cancellation.
     *
     * @param defaultName default name to display in prompt
     * @return user-inputted name, or null if cancelled
     */
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

    /**
     * Displays an error message to the user.
     *
     * @param message error message to display
     * @param title dialog title
     * @param messageType type of message (e.g., ERROR_MESSAGE)
     */
    private void showError(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(parentFrame, message, title, messageType);
    }

    /**
     * Displays a success message to the user.
     *
     * @param message success message to display
     */
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(parentFrame, message, "Save Successful", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Handles database errors by logging and displaying error message.
     *
     * @param message error context message
     * @param ex the SQLException that occurred
     */
    private void handleDatabaseError(String message, SQLException ex) {
        System.err.println(message + ex.getMessage());
        showError("Error saving to database: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}
