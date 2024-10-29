package com.gare.gbromtool;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Arrays;

public class CollectionTest {

    private static final byte[] SAMPLE_BYTES = new byte[]{0x00};

    @Test
    public void testValidConstruction() {
        Collection rom = new Collection(
                "Test Name",
                "Test Title",
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                0,
                0,
                false,
                SAMPLE_BYTES,
                0,
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                SAMPLE_BYTES
        );

        assertEquals("Test Name", rom.getName());
        assertEquals("Test Title", rom.getTitle());
        assertTrue(Arrays.equals(SAMPLE_BYTES, rom.getTypeCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNameTooLong() {
        String longName = "A".repeat(Collection.MAX_NAME_LENGTH + 1);
        new Collection(
                longName,
                "Test Title",
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                0,
                0,
                false,
                SAMPLE_BYTES,
                0,
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                SAMPLE_BYTES
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTitleTooLong() {
        String longTitle = "A".repeat(Collection.MAX_TITLE_LENGTH + 1);
        new Collection(
                "Test Name",
                longTitle,
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                0,
                0,
                false,
                SAMPLE_BYTES,
                0,
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                SAMPLE_BYTES
        );
    }

    @Test
    public void testByteArrayImmutability() {
        byte[] mutableBytes = new byte[]{0x00};
        Collection rom = new Collection(
                "Test Name",
                "Test Title",
                mutableBytes,
                SAMPLE_BYTES,
                0,
                0,
                false,
                SAMPLE_BYTES,
                0,
                SAMPLE_BYTES,
                SAMPLE_BYTES,
                SAMPLE_BYTES
        );

        // Modify original array
        mutableBytes[0] = 0x01;

        // Verify the stored array wasn't modified
        assertEquals(0x00, rom.getTypeCode()[0]);
    }

    @Test
    public void testFromRomReader() throws Exception {
        // Create a test ROM file and RomReader
        RomReader reader = TestUtils.createTestRomReader("TESTTITLE");

        Collection rom = Collection.fromRomReader(reader, "Test Name");

        assertEquals("Test Name", rom.getName());
        assertEquals("TESTTITLE", rom.getTitle().trim());
        assertNotNull(rom.getTypeCode());
    }
}
