package com.gare.gbromtool;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * This class draws the user interface and handles GUI interactions.
 *
 * @author Thomas Robinson 23191795
 */
public class UserInterface {

    private final JFrame mainFrame;
    private final JButton selectFileButton;
    private final JLabel filePathLabel;
    private final FileHandler fileHandler;

    public UserInterface() {
        mainFrame = new JFrame();
        fileHandler = new FileHandler();

        mainFrame.setTitle(Config.TITLE);
        mainFrame.setSize(Config.WINDOW_SIZE_X, Config.WINDOW_SIZE_Y);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setLayout(new FlowLayout());

        selectFileButton = new JButton("Select ROM...");
        filePathLabel = new JLabel("No file selected.");

        // Draw GUI elements
        mainFrame.add(selectFileButton);
        mainFrame.add(filePathLabel);

        // Add listeners
        selectFileButton.addActionListener((ActionEvent e) -> {
            handleFileSelection();
        });
    }

    public void show() {
        mainFrame.setVisible(true);
    }

    private void handleFileSelection() {
        FileHandler.FileOperationResult result = fileHandler.selectFile(mainFrame);

        switch (result) {
            case SUCCESS:
                filePathLabel.setText(fileHandler.getCurrentFileName());
                RomReader reader = fileHandler.getCurrentRomReader();
                if (reader != null) {
                    System.out.println("ROM Title: " + reader.getTitle());
                }
                break;

            case INVALID_FILE_TYPE:
                JOptionPane.showMessageDialog(mainFrame,
                        "Invalid file type. Please select a .gb or .gbc file.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                break;

            case READ_ERROR:
                JOptionPane.showMessageDialog(mainFrame,
                        "Error reading ROM file.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                break;

            case DIRECTORY_SELECTED:
                // Directory navigation handled by JFileChooser
                break;

            case CANCELLED:
                System.out.println("File selection cancelled");
                break;
        }
    }

}
