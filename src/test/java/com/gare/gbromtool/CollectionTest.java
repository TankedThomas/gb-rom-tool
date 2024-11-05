package com.gare.gbromtool;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Arrays;

/**
 * Test class for Collection entity.
 * Tests data validation, immutability, and factory method functionality.
 *
 * @author Thomas Robinson 23191795
 */
public class CollectionTest {

    // Sample byte array for testing constructor parameters
    private static final byte[] SAMPLE_BYTES = new byte[]{0x00};

    /**
     * Tests successful creation of a Collection with valid parameters.
     */
    @Test
    public void testValidConstruction() {
        Collection rom = new Collection(
                "Test Name",
                "Test Title",
                "ABCD",
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                0,
                0,
                false,
                SAMPLE_BYTES,
                0,
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                true,
                SAMPLE_BYTES,
                true,
                true
        );

        assertEquals("Test Name", rom.getName());
        assertEquals("Test Title", rom.getTitle());
        assertTrue(Arrays.equals(SAMPLE_BYTES, rom.getTypeCode()));
        assertTrue(rom.isHeaderChecksumValid());
        assertTrue(rom.isGlobalChecksumValid());
        assertTrue(rom.isBootLogoValid());
    }

    /**
     * Tests that constructor throws IllegalArgumentException when name exceeds
     * maximum length.
     * The constructor call is ignored by design as it is expected to throw an
     * exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNameTooLong() {
        String longName = "A".repeat(Collection.MAX_NAME_LENGTH + 1);
        new Collection(
                longName,
                "Test Title",
                "ABCD",
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                0,
                0,
                false,
                SAMPLE_BYTES,
                0,
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                true,
                SAMPLE_BYTES,
                true,
                true
        );
    }

    /**
     * Tests that constructor throws IllegalArgumentException when title exceeds
     * maximum length.
     * The constructor call is ignored by design as it is expected to throw an
     * exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testTitleTooLong() {
        // Create a title that exceeds the maximum length
        String longTitle = "A".repeat(Collection.MAX_TITLE_LENGTH + 1);

        // This constructor call should throw IllegalArgumentException
        new Collection(
                "Test Name",
                longTitle, // Invalid title length
                "ABCD",
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                0,
                0,
                false,
                SAMPLE_BYTES,
                0,
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                true,
                SAMPLE_BYTES,
                true,
                true
        );
    }

    /**
     * Tests the immutability of byte arrays in the Collection class.
     * Verifies that modifying the original array doesn't affect the stored
     * data.
     */
    @Test
    public void testByteArrayImmutability() {
        // Create a mutable array that will be modified after construction
        byte[] mutableBytes = new byte[]{0x00};

        Collection rom = new Collection(
                "Test Name",
                "Test Title",
                "ABCD",
                mutableBytes, // Pass the mutable array
                SAMPLE_BYTES,
                0,
                0,
                false,
                SAMPLE_BYTES,
                0,
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                true,
                SAMPLE_BYTES,
                true,
                true
        );

        // Modify original array
        mutableBytes[0] = 0x01;

        // Verify the stored array wasn't modified
        // Tests defensive copying in constructor
        assertEquals(0x00, rom.getTypeCode()[0]);
    }

    /**
     * Tests the factory method that creates a Collection from a RomReader.
     * Tests the complete flow of reading ROM data and creating a Collection
     * instance.
     */
    @Test
    public void testFromRomReader() throws Exception {
        // Create a test ROM file and RomReader
        RomReader reader = TestUtils.createTestRomReader("TESTTITLE");

        Collection rom = Collection.fromRomReader(reader, "Test Name");

        assertEquals("Test Name", rom.getName());
        assertEquals("TESTTITLE", rom.getTitle().trim());
        assertNotNull(rom.getTypeCode());
    }

    /**
     * Tests that null Name values are rejected by the constructor.
     * The constructor call is ignored by design as it is expected to throw an
     * exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNullName() {
        new Collection(
                null, // Invalid null name
                "Title",
                "ABCD",
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                0,
                0,
                false,
                SAMPLE_BYTES,
                0,
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                true,
                SAMPLE_BYTES,
                true,
                true
        );
    }

    /**
     * Tests that null Title values are rejected by the constructor.
     * The constructor call is ignored by design as it is expected to throw an
     * exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNullTitle() {
        new Collection(
                "Name",
                null, // Invalid null title
                "ABCD",
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                0,
                0,
                false,
                SAMPLE_BYTES,
                0,
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                true,
                SAMPLE_BYTES,
                true,
                true
        );
    }

    /**
     * Tests that byte arrays are properly deep copied both in constructor and
     * getters.
     * This ensures complete immutability of the Collection's data.
     */
    @Test
    public void testByteArrayDeepCopy() {
        // Create an array that will be modified after construction
        byte[] original = new byte[]{1, 2, 3};

        Collection rom = new Collection(
                "Test",
                "Test",
                "ABCD",
                original, // Array that will be modified
                SAMPLE_BYTES,
                0,
                0,
                false,
                SAMPLE_BYTES,
                0,
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                true,
                SAMPLE_BYTES,
                true,
                true
        );

        // Test constructor's defensive copy
        original[0] = 99;
        byte[] stored = rom.getTypeCode();
        assertEquals("Constructor should create defensive copy", 1, stored[0]);

        // Test getter's defensive copy
        stored[0] = 88;
        byte[] secondGet = rom.getTypeCode();
        assertEquals("Getter should return new copy", 1, secondGet[0]);
    }
}
