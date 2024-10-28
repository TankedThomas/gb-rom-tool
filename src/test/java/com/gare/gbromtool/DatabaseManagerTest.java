package com.gare.gbromtool;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class DatabaseManagerTest {

    private static DatabaseManager dbManager;
    private static DatabaseQuery dbQuery;

    @BeforeClass
    public static void setUpClass() throws SQLException {
        DatabaseManager.dropDatabase(); // Clean start
        dbManager = DatabaseManager.getInstance();
        dbQuery = new DatabaseQuery(dbManager);
    }

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

    @Test
    public void testDatabaseConnection() {
        Connection conn = dbManager.getConnection();
        assertNotNull("Database connection should not be null", conn);
    }

    @Test
    public void testTableExists() throws SQLException {
        Connection conn = dbManager.getConnection();
        // Test all required tables exist
        try (Statement stmt = conn.createStatement()) {
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
        }
    }

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

    @Test
    public void testUnknownCartridgeType() throws SQLException {
        // Test invalid cartridge type returns unknown
        assertEquals("Unknown cartridge type",
                dbQuery.getCartridgeTypeDescription("ZZ"));
    }

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

    @Test
    public void testOldLicenseeCodeQuery() throws SQLException {
        // Test known old licensee codes
        byte[] nintendoCode = HexFormat.of().parseHex("01");
        byte[] capcomCode = HexFormat.of().parseHex("08");

        assertEquals("Nintendo",
                dbQuery.getLicenseeCode(nintendoCode));
        assertEquals("Capcom",
                dbQuery.getLicenseeCode(capcomCode));
    }

    @Test
    public void testNewLicenseeCodeQuery() throws SQLException {
        // Test known new licensee codes
        byte[] squareCode = HexFormat.of().parseHex("4234"); // "B4" in ASCII
        byte[] konamiCode = HexFormat.of().parseHex("4134"); // "A4" in ASCII

        assertEquals("Enix",
                dbQuery.getLicenseeCode(squareCode));
        assertEquals("Konami",
                dbQuery.getLicenseeCode(konamiCode));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidLicenseeCodeLength() throws SQLException {
        byte[] invalidCode = new byte[3];
        dbQuery.getLicenseeCode(invalidCode);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullLicenseeCode() throws SQLException {
        dbQuery.getLicenseeCode(null);
    }

    @Test
    public void testCollectionTableStructure() throws SQLException {
        Connection conn = dbManager.getConnection();
        ResultSet columns = conn.getMetaData().getColumns(null, null, "COLLECTION", null);

        // Verify all required columns exist
        String[] expectedColumns = {
            "TITLE", "NAME", "TYPE_CODE", "ROM_REV", "ROM_SIZE_CODE",
            "RAM_SIZE_CODE", "SGB_FLAG", "CGB_FLAG", "DEST_CODE",
            "LICENSEE_CODE", "HEAD_CHKSM", "GLOBAL_CHKSM"
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

    private boolean containsIgnoreCase(String[] arr, String targetValue) {
        for (String s : arr) {
            if (s.equalsIgnoreCase(targetValue)) {
                return true;
            }
        }
        return false;
    }
}
