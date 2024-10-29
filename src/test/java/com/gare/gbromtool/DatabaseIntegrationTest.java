package com.gare.gbromtool;

import org.junit.Test;
import static org.junit.Assert.*;

import java.sql.SQLException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

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
                testRom.getTypeCode(),
                testRom.getRomRev(),
                testRom.getRomSizeCode(),
                testRom.getRamSizeCode(),
                testRom.getSgbFlag(),
                testRom.getCgbFlag(),
                testRom.getDestCode(),
                testRom.getLicenseeCode(),
                testRom.getHeaderChecksum(),
                testRom.getGlobalChecksum()
        );

        assertTrue("Failed to update ROM", dbQuery.updateRomInCollection(updatedRom));
    }
}
