package com.gare.gbromtool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Test class for RomReader functionality.
 * Tests ROM file loading and header field extraction.
 *
 * @author Thomas Robinson 23191795
 */
public class RomReaderTest {

    private File testRomFile;
    private RomReader romReader;

    @Before
    public void setUp() throws IOException {
        romReader = TestUtils.createTestRomReader("TESTGAME");
        testRomFile = null;  // Will be created by specific tests if needed
    }

    @After
    public void tearDown() {
        if (testRomFile != null && testRomFile.exists()) {
            testRomFile.delete();
        }
    }

    /**
     * Tests Title extraction from ROM header.
     * The Title should be properly read from the header area.
     */
    @Test
    public void testGetTitle() {
        assertEquals("TESTGAME", romReader.getTitle().trim());
    }

    /**
     * Tests SGB Flag extraction from ROM header.
     * The SGB Flag should be properly read from the header area.
     */
    @Test
    public void testGetSGBFlag() {
        assertTrue("SGB flag should be set to 0x03", romReader.getSGBFlag());
    }

    /**
     * Tests ROM Size extraction from ROM header.
     * The ROM Size should be properly read from the header area.
     */
    @Test
    public void testGetROMSize() {
        assertEquals(2, romReader.getROMSize());
    }

    /**
     * Tests RAM Size extraction from ROM header.
     * The RAM Size should be properly read from the header area.
     */
    @Test
    public void testGetRAMSize() {
        assertEquals(3, romReader.getRAMSize());
    }

    /**
     * Tests that attempting to read an invalid ROM file throws an exception.
     * ROM files must be at least 0x150 bytes to contain a valid header.
     *
     * @throws IOException if file operations fail
     */
    @Test(expected = IOException.class)
    public void testInvalidROMSize() throws IOException {
        // Create a file that's too small to be a valid ROM
        testRomFile = File.createTempFile("invalid", ".gb");
        testRomFile.deleteOnExit();
        Files.write(testRomFile.toPath(), new byte[0x100]);  // Too small for valid header
        new RomReader(testRomFile);  // Should throw IOException due to invalid size
    }

    /**
     * Tests parsing of ROM titles and manufacturer codes.
     * Creates a ROM with a CGB flag and tests the parsing logic.
     *
     * @throws IOException if ROM file creation or manipulation fails
     */
    @Test
    public void testTitleParsing() throws IOException {
        // Create complete test ROM data
        byte[] testRomData = new byte[0x150];  // Minimum size for header
        testRomData[0x143] = (byte) 0x80;  // Set CGB flag

        // Copy header data
        System.arraycopy(RomSpecification.BOOT_LOGO, 0, testRomData, 0x104, RomSpecification.BOOT_LOGO.length);
        System.arraycopy("TESTGAME".getBytes(), 0, testRomData, 0x134, "TESTGAME".length());

        // Create ROM file and reader
        File testFile = TestUtils.createTestRom(testRomData);
        RomReader reader = new RomReader(testFile);
        RomTitle title = reader.parseTitle();

        assertEquals("TESTGAME", title.getTitle().trim());
    }
}
