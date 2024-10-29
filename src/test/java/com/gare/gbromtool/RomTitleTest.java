package com.gare.gbromtool;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test class for RomTitle parsing functionality.
 * Tests parsing of ROM titles with and without manufacturer codes.
 *
 * @author Thomas Robinson 23191795
 */
public class RomTitleTest {

    /**
     * Tests parsing a basic ROM title without manufacturer code.
     *
     * @throws IOException if ROM file creation fails
     */
    @Test
    public void testBasicTitle() throws IOException {
        // Create a ROM with simple title
        RomReader reader = TestUtils.createTestRomReader("TESTGAME");
        // Parse the title from ROM data
        RomTitle title = RomTitle.parseTitle(reader.getRomData());

        // Verify correct parsing
        assertEquals("TESTGAME", title.getTitle().trim());
        assertFalse("Should not have manufacturer code", title.hasManufacturerCode());
        assertEquals("", title.getManufacturerCode());
    }

    /**
     * Tests parsing a ROM title that includes a valid manufacturer code.
     * The manufacturer code appears in the last four bytes of a CGB ROM's title
     * if it is present.
     *
     * @throws IOException if ROM file operations fail
     */
    @Test
    public void testTitleWithManufacturerCode() throws IOException {
        // Create test ROM with title and manufacturer code
        byte[] romData = new byte[0x150];
        Arrays.fill(romData, (byte) 0);

        // Write the title with manufacturer code
        byte[] titleData = "MAORIO   KMEX".getBytes();
        System.arraycopy(titleData, 0, romData, 0x134, titleData.length);

        // Set CGB flag to indicate CGB ROM
        romData[0x143] = (byte) 0x80;

        // Create ROM file
        File testFile = TestUtils.createTestRom(romData);
        RomReader reader = new RomReader(testFile);
        RomTitle title = RomTitle.parseTitle(reader.getRomData());

        // Verify title is correctly parsed
        assertEquals("MAORIO", title.getTitle().trim());
        assertTrue("Should have valid manufacturer code", title.hasManufacturerCode());
        assertEquals("KMEX", title.getManufacturerCode());
    }

    /**
     * Tests that invalid manufacturer codes are properly handled.
     * If a code doesn't match the expected format, it should be treated as part
     * of the title.
     *
     * @throws IOException if ROM file creation fails
     */
    @Test
    public void testInvalidManufacturerCode() throws IOException {
        // Create ROM with invalid manufacturer code
        byte[] romData = new byte[0x150];
        Arrays.fill(romData, (byte) 0);

        // Write title with invalid code
        byte[] titleData = "GAME     1234".getBytes();
        System.arraycopy(titleData, 0, romData, 0x134, titleData.length);

        // Set CGB flag
        romData[0x143] = (byte) 0x80;

        File testFile = TestUtils.createTestRom(romData);
        RomReader reader = new RomReader(testFile);
        RomTitle title = RomTitle.parseTitle(reader.getRomData());

        // Invalid code should be treated as part of title
        assertEquals("GAME     1234", title.getTitle().trim());
        assertFalse("Should not detect invalid manufacturer code", title.hasManufacturerCode());
        assertEquals("", title.getManufacturerCode());
    }
}
