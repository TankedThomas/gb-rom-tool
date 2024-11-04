package com.gare.gbromtool;

import java.sql.SQLException;
import javax.swing.*;

/**
 * Main class containing program entry point.
 * Initialises UI and database connection.
 *
 * @author Thomas Robinson 23191795
 */
public class GbRomTool {

    /**
     * Main program entry point.
     * Sets up look and feel, initialises UI and database.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            System.out.println("Caught exception:" + e);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UserInterface gui = new UserInterface();
                gui.show();
            } catch (SQLException e) {
                System.err.println("Failed to initialize database: " + e.getMessage());
                JOptionPane.showMessageDialog(null,
                        "Failed to initialize database: " + e.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
