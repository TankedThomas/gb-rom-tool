package com.gare.gbromtool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Utility class providing common functionality for test classes.
 * Contains methods for creating test ROMs and other test data.
 *
 * @author Thomas Robinson 23191795
 */
public final class TestUtils {

    private TestUtils() {
        // Prevent instantiation
    }

    /**
     * Creates a test ROM file with minimal valid data.
     *
     * @param title The title to embed in the ROM header
     * @return A RomReader instance for the test ROM
     * @throws IOException if file operations fail
     */
    public static RomReader createTestRomReader(String title) throws IOException {
        File testFile = File.createTempFile("test", ".gb");
        testFile.deleteOnExit();

        byte[] romData = new byte[0x150];  // Minimum size for header
        Arrays.fill(romData, (byte) 0);    // Ensure clean initial data

        // Set header data directly in the array
        System.arraycopy(RomSpecification.BOOT_LOGO, 0, romData, 0x104, RomSpecification.BOOT_LOGO.length);
        System.arraycopy(title.getBytes(), 0, romData, 0x134, Math.min(title.length(), 16));
        // Cartridge Types
        romData[0x146] = 0x03;  // SGB flag      
        romData[0x147] = 0x00;  // ROM ONLY
        romData[0x148] = 0x02;  // 128KiB ROM
        romData[0x149] = 0x03;  // 32KiB RAM

        // Write complete ROM data at once
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            // Write base ROM data
            fos.write(romData);
            fos.getFD().sync();  // Force write to disk
        }

        // Verify file contents
        byte[] readBack = Files.readAllBytes(testFile.toPath());
        if (readBack[0x146] != 0x03) {
            throw new IOException("Failed to write SGB flag: expected 0x03, got 0x"
                    + String.format("%02X", readBack[0x146]));
        }

        return new RomReader(testFile);
    }

    /**
     * Creates a byte array of specified size filled with test data.
     *
     * @param size The size of the array to create
     * @param value The value to fill the array with
     * @return A new byte array filled with the specified value
     */
    public static byte[] createTestBytes(int size, byte value) {
        byte[] result = new byte[size];
        java.util.Arrays.fill(result, value);
        return result;
    }

    /**
     * Creates a test ROM file with custom data.
     * Used for testing specific ROM configurations.
     *
     * @param data The complete ROM data to write
     * @return A temporary file containing the ROM data
     * @throws IOException if file operations fail
     */
    public static File createTestRom(byte[] data) throws IOException {
        File tempFile = File.createTempFile("test", ".gb");
        tempFile.deleteOnExit();

        // Write the complete data
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(data);
            fos.getFD().sync();
        }

        return tempFile;
    }
}
