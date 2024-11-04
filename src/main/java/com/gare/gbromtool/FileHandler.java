package com.gare.gbromtool;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Handles the loading of files and calls RomReader to extract their
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

    /**
     * @return name of currently loaded file, or empty string if none
     */
    public String getCurrentFileName() {
        return currentRom != null ? currentRom.getName() : "";
    }

    /**
     * @return title of currently loaded ROM, or empty string if none
     */
    public String getCurrentRomTitle() {
        return romReader != null ? romReader.parseTitle().getTitle() : "";
    }

    /**
     * @return manufacturer code of current ROM, or empty string if none
     */
    public String getCurrentManufacturerCode() {
        return romReader != null ? romReader.parseTitle().getManufacturerCode() : "";
    }

    /**
     * @return current RomReader instance
     */
    public RomReader getCurrentRomReader() {
        return romReader;
    }

    /**
     * Shows file selection dialog and processes selected file.
     *
     * @param parent parent component for dialog
     * @return result of the file operation
     */
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

    /**
     * Validates file extension against allowed types.
     *
     * @param file file to validate
     * @param extensions array of allowed extensions
     * @return true if file has valid extension
     */
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
