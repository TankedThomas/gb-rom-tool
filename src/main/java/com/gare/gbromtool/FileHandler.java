package com.gare.gbromtool;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * This class handles the loading of files and calls RomReader to extract their
 * information.
 *
 * @author Thomas Robinson 23191795
 */
public class FileHandler {

    private final JFileChooser chooseRom;
    private RomReader romReader;
    private File currentRom;

    public FileHandler() {

        // File selection
        chooseRom = new JFileChooser();
        chooseRom.setDialogTitle("Select a .gb or .gbc file");
        chooseRom.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        FileNameExtensionFilter romFiletype = new FileNameExtensionFilter("Game Boy (Color) ROMs (*.gb, *.gbc)", "gb", "gbc");
        chooseRom.setFileFilter(romFiletype);
    }

    public enum FileOperationResult {
        SUCCESS,
        DIRECTORY_SELECTED,
        INVALID_FILE_TYPE,
        READ_ERROR,
        CANCELLED
    }

    // Getter for current filename
    public String getCurrentFileName() {
        return currentRom != null ? currentRom.getName() : "";
    }

    // Getter for current ROM title
    public String getCurrentRomTitle() {
        return romReader != null ? romReader.parseTitle().getTitle() : "";
    }

    // Getter for current Manufacturer Code
    public String getCurrentManufacturerCode() {
        return romReader != null ? romReader.parseTitle().getManufacturerCode() : "";
    }

    // Getter for current RomReader
    public RomReader getCurrentRomReader() {
        return romReader;
    }

    // Method to show file dialog and handle selection
    public FileOperationResult selectFile(Component parent) {

        int userSelection = chooseRom.showOpenDialog(parent);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooseRom.getSelectedFile();

            if (selectedFile.isDirectory()) {
                chooseRom.setCurrentDirectory(selectedFile);
                return FileOperationResult.DIRECTORY_SELECTED;
            } else if (validateFileType(selectedFile, new String[]{"gb", "gbc"})) {

                // Create a reader of the chosen ROM for use in other classes
                try {
                    currentRom = selectedFile;
                    romReader = new RomReader(selectedFile);
                    return FileOperationResult.SUCCESS;
                } catch (IOException e) {
                    return FileOperationResult.READ_ERROR;
                }
            }
            return FileOperationResult.INVALID_FILE_TYPE;
        }
        return FileOperationResult.CANCELLED;
    }

    public boolean validateFileType(File file, String[] extensions) {
        String fileName = file.getName().toLowerCase();

        for (String ext : extensions) {
            if (fileName.endsWith("." + ext)) {
                return true;
            }
        }

        return false;
    }

}
