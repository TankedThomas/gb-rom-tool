package com.gare.gbromtool;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class RomReaderTest {

    private File testRomFile;
    private RomReader reader;

    @Before
    public void setUp() throws IOException {
        // Create a test ROM file with known header data
        testRomFile = File.createTempFile("test", ".gb");
        testRomFile.deleteOnExit();

        // Create mock ROM data (minimal valid ROM header)
        byte[] mockRomData = new byte[0x150];  // Minimum size for header

        // Set up test title "TESTTITLE" at 0x134
        byte[] titleBytes = "TESTTITLE".getBytes();
        try (FileOutputStream fos = new FileOutputStream(testRomFile)) {
            // Fill with zeros first
            fos.write(new byte[0x150]);
            // Write title at correct offset
            fos.getChannel().position(0x134);
            fos.write(titleBytes);
            // Set some test values
            fos.getChannel().position(0x143);  // CGB flag
            fos.write(new byte[]{(byte) 0x80});
            fos.getChannel().position(0x146);  // SGB flag
            fos.write(new byte[]{0x03});
            fos.getChannel().position(0x147);  // Cartridge type
            fos.write(new byte[]{0x01});
            fos.getChannel().position(0x148);  // ROM size
            fos.write(new byte[]{0x02});
            fos.getChannel().position(0x149);  // RAM size
            fos.write(new byte[]{0x03});
        }

        reader = new RomReader(testRomFile);
    }

    @Test
    public void testGetTitle() {
        assertEquals("TESTTITLE", reader.getTitle().trim());
    }

    @Test
    public void testGetSGBFlag() {
        assertTrue(reader.getSGBFlag());
    }

    @Test
    public void testGetROMSize() {
        assertEquals(2, reader.getROMSize());
    }

    @Test
    public void testGetRAMSize() {
        assertEquals(3, reader.getRAMSize());
    }

    @Test(expected = IOException.class)
    public void testInvalidROMSize() throws IOException {
        // Create a file that's too small to be a valid ROM
        File invalidFile = File.createTempFile("invalid", ".gb");
        invalidFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(invalidFile)) {
            fos.write(new byte[0x100]);  // Too small for valid header
        }
        new RomReader(invalidFile);  // Should throw IOException
    }

    @Test
    public void testTitleParsing() throws IOException {
        byte[] cgbRomData = new byte[0x150];
        cgbRomData[0x143] = (byte) 0x80;  // Set CGB flag
        byte[] titleBytes = "POKEMON    ".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(titleBytes, 0, cgbRomData, 0x134, titleBytes.length);

        RomReader reader = new RomReader(createTestRom(cgbRomData));
        RomTitle title = reader.parseTitle();
        assertEquals("POKEMON", title.getTitle().trim());
        assertEquals("", title.getManufacturerCode());
    }

    private File createTestRom(byte[] data) throws IOException {
        File tempFile = File.createTempFile("test", ".gb");
        tempFile.deleteOnExit();
        Files.write(tempFile.toPath(), data);
        return tempFile;
    }
}
