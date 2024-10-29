package com.gare.gbromtool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            // Write base ROM data
            fos.write(romData);

            // Write Logo
            fos.getChannel().position(0x104);
            fos.write(RomSpecification.BOOT_LOGO);

            // Write title
            fos.getChannel().position(0x134);
            fos.write(title.getBytes());

            // Set minimal valid header values
            fos.getChannel().position(0x147); // Cartridge type
            fos.write(new byte[]{0x00});     // ROM ONLY

            fos.getChannel().position(0x148); // ROM size
            fos.write(new byte[]{0x00});     // 32KB

            fos.getChannel().position(0x149); // RAM size
            fos.write(new byte[]{0x00});     // No RAM
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
}
