package com.gare.gbromtool;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
 * This class contains the main method for executing the program.
 *
 * @author Thomas Robinson 23191795
 */
public class GbRomTool extends JFrame {

    private final JButton selectFileButton;
    private final JLabel filePathLabel;
    private final JFileChooser chooseRom;

    public GbRomTool() {
        setTitle(Config.TITLE);
        setSize(Config.WINDOW_SIZE_X, Config.WINDOW_SIZE_Y);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new FlowLayout());

        selectFileButton = new JButton("Select ROM...");
        filePathLabel = new JLabel("No file selected.");

        // Draw GUI elements
        add(selectFileButton);
        add(filePathLabel);

        // File selection
        chooseRom = new JFileChooser();
        chooseRom.setDialogTitle("Select a .gb or .gbc file");
        chooseRom.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        FileNameExtensionFilter romFiletype = new FileNameExtensionFilter("Game Boy (Color) ROMs (*.gb, *.gbc)", "gb", "gbc");
        chooseRom.setFileFilter(romFiletype);

        selectFileButton.addActionListener((ActionEvent e) -> {
            selectFile();
        });
    }

    private void selectFile() {
        while (true) {

            int userSelection = chooseRom.showOpenDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooseRom.getSelectedFile();

                if (selectedFile.isDirectory()) {
                    chooseRom.setCurrentDirectory(selectedFile);
                } else if (validateFileType(selectedFile, new String[]{"gb", "gbc"})) {
                    System.out.println("File selected: " + selectedFile.getAbsolutePath());
                    filePathLabel.setText(selectedFile.getName());
                    break;
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid file type. Please select a .gb or .gbc file.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                System.out.println("No file selected.");
                break;
            }
        }
    }

    public static boolean validateFileType(File file, String[] extensions) {
        String fileName = file.getName().toLowerCase();

        for (String ext : extensions) {
            if (fileName.endsWith("." + ext)) {
                return true;
            }
        }

        return false;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new GbRomTool().setVisible(true);
        });
    }
}
