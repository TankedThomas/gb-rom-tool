package com.gare.gbromtool;

import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Test class for DatabaseManager.
 * Tests database connection, table creation, and data initialisation.
 *
 * @author Thomas Robinson 23191795
 */
public class DatabaseManagerTest {

    private static DatabaseManager dbManager;
    private static DatabaseQuery dbQuery;

    /**
     * Sets up the test environment before any tests run.
     * Creates a fresh database instance.
     *
     * @throws SQLException if database initialisation fails
     */
    @BeforeClass
    public static void setUpClass() throws SQLException {
        DatabaseManager.dropDatabase(); // Clean start
        dbManager = DatabaseManager.getInstance();
        dbQuery = new DatabaseQuery(dbManager);
    }

    /**
     * Sets up each individual test.
     * Cleans the database but maintains the connection.
     *
     * @throws SQLException if database cleanup fails
     */
    @Before
    public void setUp() throws SQLException {
        dbManager.cleanDatabase();
        dbManager.initialiseDatabase();
    }

    @After
    public void tearDown() throws SQLException {
        // No need to do anything here
    }

    @AfterClass
    public static void tearDownClass() {
        DatabaseManager.dropDatabase();
    }

    /**
     * Tests database connection establishment.
     */
    @Test
    public void testDatabaseConnection() {
        Connection conn = dbManager.getConnection();
        assertNotNull("Database connection should not be null", conn);
    }

    /**
     * Tests table creation by verifying all required tables exist.
     *
     * @throws SQLException if database operations fail
     */
    @Test
    public void testTableExists() throws SQLException {
        Connection conn = dbManager.getConnection();
        // Test all required tables exist
        try {
            // Test all required tables exist
            String[] tables = {
                "CartridgeType", "ROMSize", "RAMSize",
                "NewLicenseeCode", "OldLicenseeCode", "Collection"
            };

            for (String table : tables) {
                try (ResultSet rs = conn.getMetaData().getTables(null, null,
                        table.toUpperCase(), new String[]{"TABLE"})) {
                    assertTrue("Table " + table + " should exist", rs.next());
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error while checking tables: " + e.getMessage());
            throw e;  // Re-throw to fail the test
        }
    }

    /**
     * Tests Manufacturer Code storage in Collection table.
     * Verifies that Manufacturer Codes are properly saved and retrieved.
     *
     * @throws SQLException if database operations fail
     * @throws IOException if ROM file operations fail
     */
    @Test
    public void testManufacturerCode() throws SQLException, IOException {
        // Create CGB ROM with Manufacturer Code
        RomReader reader = TestUtils.createTestCGBRomReader("MAORIO", "KMEX");  // Title with Manufacturer Code
        Collection rom = Collection.fromRomReader(reader, "Test ROM");

        // Save and verify
        assertTrue(dbQuery.saveRomToCollection(rom));
        ArrayList<Collection> roms = dbQuery.getAllRoms();
        assertFalse("Collection should not be empty", roms.isEmpty());
        assertEquals("KMEX", roms.get(0).getManufacturerCode());
    }

    /**
     * Tests that null Manufacturer Code is handled properly.
     * Verifies that ROMs without Manufacturer Code can be saved and retrieved.
     *
     * @throws SQLException if database operations fail
     * @throws IOException if ROM file operations fail
     */
    @Test
    public void testNullManufacturerCode() throws SQLException, IOException {
        // Create ROM without manufacturer code
        RomReader reader = TestUtils.createTestRomReader("TESTGAME");
        Collection rom = Collection.fromRomReader(reader, "Test ROM");

        // Save and verify
        assertTrue(dbQuery.saveRomToCollection(rom));
        ArrayList<Collection> roms = dbQuery.getAllRoms();
        assertFalse("Collection should not be empty", roms.isEmpty());
        Collection retrieved = roms.get(0);
        assertTrue("Manufacturer code should be null or empty",
                retrieved.getManufacturerCode() == null || retrieved.getManufacturerCode().isEmpty());
    }

    /**
     * Tests Cartridge Type lookup functionality.
     * Verifies that known Cartridge Types return correct descriptions.
     *
     * @throws SQLException if database query fails
     */
    @Test
    public void testCartridgeTypeQuery() throws SQLException {
        // Test known cartridge types
        assertEquals("ROM ONLY",
                dbQuery.getCartridgeTypeDescription("00"));
        assertEquals("MBC1",
                dbQuery.getCartridgeTypeDescription("01"));
        assertEquals("MBC3+RAM+BATTERY",
                dbQuery.getCartridgeTypeDescription("13"));
    }

    /**
     * Tests handling of unknown Cartridge Types.
     * Verifies that invalid type codes return appropriate default message.
     *
     * @throws SQLException if database query fails
     */
    @Test
    public void testUnknownCartridgeType() throws SQLException {
        // Test invalid cartridge type returns unknown
        assertEquals("Unknown cartridge type",
                dbQuery.getCartridgeTypeDescription("ZZ"));
    }

    /**
     * Tests ROM Size lookup functionality.
     * Verifies that ROM Size codes map to correct sizes and bank numbers.
     *
     * @throws SQLException if database query fails
     */
    @Test
    public void testROMSizeQuery() throws SQLException {
        Connection conn = dbManager.getConnection();
        // Test specific ROM sizes
        try (Statement stmt = conn.createStatement()) {
            // Test specific ROM sizes
            ResultSet rs = stmt.executeQuery(
                    "SELECT size_kib, num_banks FROM ROMSize WHERE size_code = 0");
            assertTrue(rs.next());
            assertEquals(32, rs.getInt("size_kib"));
            assertEquals(2, rs.getInt("num_banks"));

            rs = stmt.executeQuery(
                    "SELECT size_kib, num_banks FROM ROMSize WHERE size_code = 5");
            assertTrue(rs.next());
            assertEquals(1024, rs.getInt("size_kib")); // 1 MiB
            assertEquals(64, rs.getInt("num_banks"));

            rs.close();
        }
    }

    /**
     * Tests RAM Size lookup functionality.
     * Verifies that RAM Size codes map to correct sizes and bank numbers.
     *
     * @throws SQLException if database query fails
     */
    @Test
    public void testRAMSizeQuery() throws SQLException {
        Connection conn = dbManager.getConnection();
        // Test specific RAM sizes
        try (Statement stmt = conn.createStatement()) {
            // Test specific RAM sizes
            ResultSet rs = stmt.executeQuery(
                    "SELECT size_kib, num_banks FROM RAMSize WHERE size_code = 0");
            assertTrue(rs.next());
            assertEquals(0, rs.getInt("size_kib"));
            assertEquals(0, rs.getInt("num_banks"));

            rs = stmt.executeQuery(
                    "SELECT size_kib, num_banks FROM RAMSize WHERE size_code = 3");
            assertTrue(rs.next());
            assertEquals(32, rs.getInt("size_kib"));
            assertEquals(4, rs.getInt("num_banks"));

            rs.close();
        }
    }

    /**
     * Tests Old Licensee Code lookup.
     * Verifies that Old Licensee Codes (single byte) are correctly
     * mapped.
     *
     * @throws SQLException if database query fails
     */
    @Test
    public void testOldLicenseeCodeQuery() throws SQLException {
        // Test known Old licensee Codes
        byte[] nintendoCode = HexFormat.of().parseHex("01");
        byte[] capcomCode = HexFormat.of().parseHex("08");

        assertEquals("Nintendo",
                dbQuery.getLicenseeCode(nintendoCode));
        assertEquals("Capcom",
                dbQuery.getLicenseeCode(capcomCode));
    }

    /**
     * Tests New Licensee Code lookup.
     * Verifies that New Licensee Codes (two bytes) are correctly mapped.
     *
     * @throws SQLException if database query fails
     */
    @Test
    public void testNewLicenseeCodeQuery() throws SQLException {
        // Test known New licensee Codes
        byte[] squareCode = HexFormat.of().parseHex("4234"); // "B4" in ASCII
        byte[] konamiCode = HexFormat.of().parseHex("4134"); // "A4" in ASCII

        assertEquals("Enix",
                dbQuery.getLicenseeCode(squareCode));
        assertEquals("Konami",
                dbQuery.getLicenseeCode(konamiCode));
    }

    /**
     * Tests rejection of invalid Licensee Code lengths.
     * Verifies that codes of incorrect length throw appropriate exception.
     *
     * @throws SQLException if database query fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidLicenseeCodeLength() throws SQLException {
        byte[] invalidCode = new byte[3];
        dbQuery.getLicenseeCode(invalidCode);
    }

    /**
     * Tests handling of null Licensee Codes.
     * Verifies that null input throws appropriate exception.
     *
     * @throws SQLException if database query fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNullLicenseeCode() throws SQLException {
        dbQuery.getLicenseeCode(null);
    }

    /**
     * Tests RomReader file loading with validity checks.
     *
     * @throws IOException
     */
    @Test
    public void testFileValidity() throws IOException {
        // Create test ROM file with invalid checksum
        byte[] romData = new byte[0x150];
        System.arraycopy(RomSpecification.BOOT_LOGO, 0, romData, 0x104, RomSpecification.BOOT_LOGO.length);
        // Set an invalid header checksum
        romData[0x14D] = (byte) 0xFF;

        File testFile = TestUtils.createTestRom(romData);
        RomReader reader = new RomReader(testFile);

        assertFalse("Should detect invalid header checksum", reader.verifyHeaderChecksum());
        assertTrue("Should have valid boot logo", reader.getLogo());
    }

    /**
     * Tests that validity flags are properly transferred when creating
     * Collection from RomReader.
     *
     * @throws IOException
     */
    @Test
    public void testFromRomReaderValidityFlags() throws IOException {
        RomReader reader = TestUtils.createTestRomReader("TESTGAME");
        Collection rom = Collection.fromRomReader(reader, "Test Name");

        // The test ROM should have valid checksums and boot logo
        assertTrue("Header checksum validity not transferred", rom.isHeaderChecksumValid());
        assertTrue("Global checksum validity not transferred", rom.isGlobalChecksumValid());
        assertTrue("Boot logo validity not transferred", rom.isBootLogoValid());
    }

    /**
     * Tests Collection table structure.
     * Verifies that all required columns exist with correct names.
     *
     * @throws SQLException if database metadata query fails
     */
    @Test
    public void testCollectionTableStructure() throws SQLException {
        Connection conn = dbManager.getConnection();
        ResultSet columns = conn.getMetaData().getColumns(null, null, "COLLECTION", null);

        // Verify all required columns exist
        String[] expectedColumns = {
            "TITLE", "NAME", "MFT_CODE", "TYPE_CODE", "ROM_REV",
            "ROM_SIZE_CODE", "RAM_SIZE_CODE", "SGB_FLAG", "CGB_FLAG",
            "DEST_CODE", "LICENSEE_CODE", "HEAD_CHKSM", "HEAD_CHKSM_VALID",
            "GLOBAL_CHKSM", "GLOBAL_CHKSM_VALID", "BOOT_LOGO_VALID"
        };

        int columnCount = 0;
        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            assertTrue("Unexpected column: " + columnName,
                    containsIgnoreCase(expectedColumns, columnName));
            columnCount++;
        }

        assertEquals("Collection table should have exact number of columns",
                expectedColumns.length, columnCount);
    }

    /**
     * Helper method for case-insensitive string comparison.
     * Used for database column name validation.
     *
     * @param arr Array of strings to search
     * @param targetValue Value to find (case-insensitive)
     * @return true if value is found, false otherwise
     */
    private boolean containsIgnoreCase(String[] arr, String targetValue) {
        for (String s : arr) {
            if (s.equalsIgnoreCase(targetValue)) {
                return true;
            }
        }
        return false;
    }
}
