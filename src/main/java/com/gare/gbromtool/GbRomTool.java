package com.gare.gbromtool;

import javax.swing.*;

/**
 * This class contains the main method for executing the program.
 *
 * @author Thomas Robinson 23191795
 */
public class GbRomTool {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            System.out.println("Caught exception:" + e);
        }

        SwingUtilities.invokeLater(() -> {
            UserInterface gui = new UserInterface();
            gui.show();
        });
    }
}
