package com.gare.gbromtool;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Integration tests for database operations.
 * Tests the complete flow of database operations including saving, retrieving,
 * and updating ROM data across multiple tables.
 *
 * This test class verifies that:
 * - ROMs can be saved to and retrieved from the database
 * - Multiple ROM versions are handled correctly
 * - Duplicate detection works as expected
 * - Database constraints are properly enforced
 *
 * @author Thomas Robinson 23191795
 */
public class DatabaseIntegrationTest {

    private static DatabaseManager dbManager;
    private DatabaseQuery dbQuery;
    private Collection testRom;

    @BeforeClass
    public static void setUpClass() throws SQLException {
        // Start with clean database
        DatabaseManager.dropDatabase();
        dbManager = DatabaseManager.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        // Clean database between tests
        dbManager.cleanDatabase();
        dbQuery = new DatabaseQuery(dbManager);

        // Create test ROM data for each test
        RomReader reader = TestUtils.createTestRomReader("TESTGAME");
        testRom = Collection.fromRomReader(reader, "Test ROM");
    }

    @After
    public void tearDown() {
        try {
            dbManager.cleanDatabase();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    /**
     * Tests basic save and retrieve operations.
     * Verifies that a ROM can be saved to the database and confirm its
     * existence.
     *
     * @throws SQLException if database operations fail
     */
    @Test
    public void testSaveAndRetrieve() throws SQLException {
        assertTrue(dbQuery.saveRomToCollection(testRom));
        assertTrue(dbQuery.romExistsInCollection(
                testRom.getTitle(),
                testRom.getRomRev(),
                testRom.getGlobalChecksum()
        ));
    }

    /**
     * Tests updating existing ROM entries.
     * Verifies that ROM data can be saved and then updated with new
     * information.
     *
     * @throws SQLException if database operations fail
     */
    @Test
    public void testUpdate() throws SQLException {
        // Save initial version
        assertTrue(dbQuery.saveRomToCollection(testRom));

        // Create updated version with new name
        Collection updatedRom = new Collection(
                "Updated Name",
                testRom.getTitle(),
                testRom.getManufacturerCode(),
                testRom.getTypeCode(),
                testRom.getRomRev(),
                testRom.getRomSizeCode(),
                testRom.getRamSizeCode(),
                testRom.getSgbFlag(),
                testRom.getCgbFlag(),
                testRom.getDestCode(),
                testRom.getLicenseeCode(),
                testRom.getHeaderChecksum(),
                testRom.isHeaderChecksumValid(),
                testRom.getGlobalChecksum(),
                testRom.isGlobalChecksumValid(),
                testRom.isBootLogoValid()
        );

        assertTrue("Failed to update ROM", dbQuery.updateRomInCollection(updatedRom));
    }

    /**
     * Test saving multiple versions of the same ROM with different revisions.
     * Verifies that different versions of the same ROM can coexist in the
     * database.
     *
     * @throws SQLException if database operations fail
     * @throws IOException if ROM file operations fail
     */
    @Test
    public void testMultipleVersions() throws SQLException, IOException {
        // Save initial version
        assertTrue(dbQuery.saveRomToCollection(testRom));
        // Create new ROM with different revision
        byte[] newRev = new byte[]{0x01};  // Different revision number
        Collection testRomV2 = new Collection(
                "Test ROM v2",
                testRom.getTitle(),
                testRom.getManufacturerCode(),
                testRom.getTypeCode(),
                newRev, // Different revision
                testRom.getRomSizeCode(),
                testRom.getRamSizeCode(),
                testRom.getSgbFlag(),
                testRom.getCgbFlag(),
                testRom.getDestCode(),
                testRom.getLicenseeCode(),
                testRom.getHeaderChecksum(),
                testRom.isHeaderChecksumValid(),
                testRom.getGlobalChecksum(),
                testRom.isGlobalChecksumValid(),
                testRom.isBootLogoValid()
        );

        // Save second version
        assertTrue(dbQuery.saveRomToCollection(testRomV2));

        // Verify both versions exist
        assertTrue(dbQuery.romExistsInCollection(
                testRom.getTitle(),
                testRom.getRomRev(),
                testRom.getGlobalChecksum()
        ));
        assertTrue(dbQuery.romExistsInCollection(
                testRomV2.getTitle(),
                testRomV2.getRomRev(),
                testRomV2.getGlobalChecksum()
        ));
    }

    /**
     * Test that identical ROMs are properly detected.
     * Verifies that attempting to save the same ROM twice is handled correctly.
     *
     * @throws SQLException if database operations fail
     * @throws IOException if ROM file operations fail
     */
    @Test
    public void testDuplicateDetection() throws SQLException, IOException {
        // Save initial ROM
        assertTrue(dbQuery.saveRomToCollection(testRom));

        // Attempt to save same ROM again
        assertTrue("Should detect duplicate ROM",
                dbQuery.romExistsInCollection(
                        testRom.getTitle(),
                        testRom.getRomRev(),
                        testRom.getGlobalChecksum()
                ));
    }

    /**
     * Test database constraints with invalid data.
     * Verifies that the database properly rejects invalid data by attempting to
     * save a ROM with a binary field that exceeds the database column size.
     *
     * @throws SQLException if database operation fails
     */
    @Test(expected = SQLException.class)
    public void testInvalidData() throws SQLException {
        // Create a collection with an oversized byte array that should exceed
        // the database column size for binary data
        byte[] oversizedArray = new byte[1024];  // Much larger than the database can handle
        Arrays.fill(oversizedArray, (byte) 0xFF);  // Fill with non-zero data

        Collection invalidRom = new Collection(
                "Test Name",
                "Test Title",
                "ABCD",
                oversizedArray, // This should be too large for the database column
                testRom.getRomRev(),
                testRom.getRomSizeCode(),
                testRom.getRamSizeCode(),
                testRom.getSgbFlag(),
                testRom.getCgbFlag(),
                testRom.getDestCode(),
                testRom.getLicenseeCode(),
                testRom.getHeaderChecksum(),
                testRom.isHeaderChecksumValid(),
                testRom.getGlobalChecksum(),
                testRom.isGlobalChecksumValid(),
                testRom.isBootLogoValid()
        );

        // This should throw SQLException due to data being too large for the column
        dbQuery.saveRomToCollection(invalidRom);
    }

    /**
     * Tests that validation flags are correctly saved and retrieved from the
     * database.
     *
     * @throws SQLException if database operations fail
     * @throws IOException if ROM file operations fail
     */
    @Test
    public void testValidationFlags() throws SQLException, IOException {
        // Create ROMs with different validation states
        Collection validRom = new Collection(
                "Valid ROM",
                "TEST1",
                "ABCD",
                testRom.getTypeCode(),
                testRom.getRomRev(),
                testRom.getRomSizeCode(),
                testRom.getRamSizeCode(),
                testRom.getSgbFlag(),
                testRom.getCgbFlag(),
                testRom.getDestCode(),
                testRom.getLicenseeCode(),
                testRom.getHeaderChecksum(),
                true, // Valid header checksum
                testRom.getGlobalChecksum(),
                true, // Valid global checksum
                true // Valid boot logo
        );

        Collection invalidRom = new Collection(
                "Invalid ROM",
                "TEST2",
                "ABCD",
                testRom.getTypeCode(),
                testRom.getRomRev(),
                testRom.getRomSizeCode(),
                testRom.getRamSizeCode(),
                testRom.getSgbFlag(),
                testRom.getCgbFlag(),
                testRom.getDestCode(),
                testRom.getLicenseeCode(),
                testRom.getHeaderChecksum(),
                false, // Invalid header checksum
                testRom.getGlobalChecksum(),
                false, // Invalid global checksum
                false // Invalid boot logo
        );

        // Save both ROMs
        assertTrue(dbQuery.saveRomToCollection(validRom));
        assertTrue(dbQuery.saveRomToCollection(invalidRom));

        // Retrieve and verify
        ArrayList<Collection> roms = dbQuery.getAllRoms();
        assertEquals("Should have saved two ROMs", 2, roms.size());

        // Find the ROMs in the results (they might be in any order)
        Collection retrievedValid = null;
        Collection retrievedInvalid = null;
        for (Collection rom : roms) {
            if (rom.getName().equals("Valid ROM")) {
                retrievedValid = rom;
            } else if (rom.getName().equals("Invalid ROM")) {
                retrievedInvalid = rom;
            }
        }

        assertNotNull("Valid ROM should be retrieved", retrievedValid);
        assertNotNull("Invalid ROM should be retrieved", retrievedInvalid);

        // Verify valid ROM flags
        assertTrue("Valid ROM header checksum flag not preserved",
                retrievedValid.isHeaderChecksumValid());
        assertTrue("Valid ROM global checksum flag not preserved",
                retrievedValid.isGlobalChecksumValid());
        assertTrue("Valid ROM boot logo flag not preserved",
                retrievedValid.isBootLogoValid());

        // Verify invalid ROM flags
        assertFalse("Invalid ROM header checksum flag not preserved",
                retrievedInvalid.isHeaderChecksumValid());
        assertFalse("Invalid ROM global checksum flag not preserved",
                retrievedInvalid.isGlobalChecksumValid());
        assertFalse("Invalid ROM boot logo flag not preserved",
                retrievedInvalid.isBootLogoValid());
    }
}
