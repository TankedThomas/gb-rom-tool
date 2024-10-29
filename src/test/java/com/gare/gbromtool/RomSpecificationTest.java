package com.gare.gbromtool;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests for ROM specification constants and utility methods.
 * Verifies the correct implementation of GB ROM format specifications including
 * header offsets, sizes, and data extraction methods.
 *
 * @author Thomas Robinson 23191795
 */
public class RomSpecificationTest {

    /**
     * Tests header offset enumeration values.
     * Verifies that all header fields have correct offset and length values
     * according to GB specifications.
     */
    @Test
    public void testHeaderOffsetValues() {
        // Test Title offset
        assertEquals(0x134, RomSpecification.HeaderOffset.TITLE.getOffset());
        assertEquals(16, RomSpecification.HeaderOffset.TITLE.getLength());

        // Test GCB Flag offset
        assertEquals(0x143, RomSpecification.HeaderOffset.CGB_FLAG.getOffset());
        assertEquals(1, RomSpecification.HeaderOffset.CGB_FLAG.getLength());

        // Test SGB Flag offset
        assertEquals(0x146, RomSpecification.HeaderOffset.SGB_FLAG.getOffset());
        assertEquals(1, RomSpecification.HeaderOffset.SGB_FLAG.getLength());

        // Test Global Checksum offset
        assertEquals(0x14E, RomSpecification.HeaderOffset.GLOBAL_CHECKSUM.getOffset());
        assertEquals(2, RomSpecification.HeaderOffset.GLOBAL_CHECKSUM.getLength());
    }

    /**
     * Tests ROM size validation.
     * Verifies that the system correctly identifies valid and invalid ROM
     * sizes.
     * A valid ROM must be at least 0x150 bytes long to contain a complete
     * header.
     */
    @Test
    public void testValidSize() {
        assertTrue(RomSpecification.isValidSize(0x150));    // Minimum valid size
        assertTrue(RomSpecification.isValidSize(0x8000));   // Typical ROM size
        assertFalse(RomSpecification.isValidSize(0x100));   // Too small
        assertFalse(RomSpecification.isValidSize(0));       // Invalid size
    }

    /**
     * Tests field extraction from ROM data.
     * Verifies that header fields are correctly extracted at their specified
     * offsets and lengths.
     */
    @Test
    public void testFieldExtraction() {
        // Create test ROM data
        byte[] romData = new byte[0x150];
        romData[0x134] = 'T';
        romData[0x135] = 'E';
        romData[0x136] = 'S';
        romData[0x137] = 'T';

        // Extract and verify title field
        byte[] titleField = RomSpecification.extractField(
                romData,
                RomSpecification.HeaderOffset.TITLE
        );
        assertEquals(16, titleField.length);
        assertEquals('T', titleField[0]);
        assertEquals('E', titleField[1]);
        assertEquals('S', titleField[2]);
        assertEquals('T', titleField[3]);
    }

    /**
     * Tests boot logo constant.
     * Verifies that the Boot Logo is correctly defined according to
     * specification.
     */
    @Test
    public void testBootLogo() {
        // Verify boot Logo length
        assertEquals(48, RomSpecification.BOOT_LOGO.length);

        // Check first and last bytes of Logo
        assertEquals((byte) 0xCE, RomSpecification.BOOT_LOGO[0]);
        assertEquals((byte) 0x3E, RomSpecification.BOOT_LOGO[47]);
    }

}
