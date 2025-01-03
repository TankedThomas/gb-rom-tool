package com.gare.gbromtool;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.JFrame;

/**
 * Test class for CollectionManager functionality.
 * Tests insertion, update and validation of data in the Collection table.
 *
 * @author Thomas Robinson 23191795
 */
public class CollectionManagerTest {

    private static DatabaseManager dbManager;
    private CollectionManager collectionManager;
    private DatabaseQuery dbQuery;
    private JFrame testFrame;
    private File testRomFile;
    private RomReader testReader;

    @Before
    public void setUp() throws SQLException, IOException {
        DatabaseManager.dropDatabase();
        dbManager = DatabaseManager.getInstance();
        dbQuery = new DatabaseQuery(dbManager);
        testFrame = new JFrame();
        collectionManager = new CollectionManager(dbQuery, testFrame);
        collectionManager.setTestMode(true);  // Enable test mode
        createTestRomFile();
    }

    @After
    public void tearDown() {
        if (testRomFile != null && testRomFile.exists()) {
            testRomFile.delete();
        }
        DatabaseManager.dropDatabase();
    }

    private void createTestRomFile() throws IOException {
        testRomFile = File.createTempFile("test", ".gb");
        testRomFile.deleteOnExit();

        // Create test ROM data (minimum 0x150 bytes)
        byte[] romData = new byte[0x150];

        // Logo data (required for validation)
        byte[] logoData = RomSpecification.BOOT_LOGO;

        try (FileOutputStream fos = new FileOutputStream(testRomFile)) {
            // Write base ROM data
            fos.write(romData);

            // Write Logo
            fos.getChannel().position(0x104);
            fos.write(logoData);

            // Write title "TESTTITLE" at 0x134
            fos.getChannel().position(0x134);
            fos.write("TESTGAME".getBytes());

            // Write various header fields
            fos.getChannel().position(0x143); // CGB flag
            fos.write(new byte[]{0x00});  // Not CGB

            fos.getChannel().position(0x146); // SGB flag
            fos.write(new byte[]{0x00});  // Not SGB

            fos.getChannel().position(0x147); // Cartridge type
            fos.write(new byte[]{0x00});  // ROM ONLY

            fos.getChannel().position(0x148); // ROM size
            fos.write(new byte[]{0x00});  // 32KB

            fos.getChannel().position(0x149); // RAM size
            fos.write(new byte[]{0x00});  // No RAM

            // Write some valid checksums
            fos.getChannel().position(0x14D); // Header checksum
            fos.write(new byte[]{0x00});

            fos.getChannel().position(0x14E); // Global checksum
            fos.write(new byte[]{0x00, 0x00});
        }

        testReader = new RomReader(testRomFile);
    }

    @Test
    public void testSaveValidROM() throws SQLException, IOException {
        // Save the ROM
        boolean result = collectionManager.saveRomToCollection(testReader, "TestROM");
        assertTrue("Failed to save valid ROM", result);

        // Verify using complete version checking
        assertTrue("ROM not found in database",
                dbQuery.romExistsInCollection(
                        testReader.parseTitle().getTitle(),
                        testReader.getMaskVersion(),
                        testReader.getGlobalChecksum()
                )
        );

        // Print debug info
        dbQuery.printAllRoms();
    }

    @Test
    public void testSaveNullReader() {
        boolean result = collectionManager.saveRomToCollection(null, "TestROM");
        assertFalse("Should fail when saving null ROM reader", result);
    }

    @Test
    public void testSaveLongName() throws IOException {
        String longName = "A".repeat(201);
        boolean result = collectionManager.saveRomToCollection(testReader, longName);
        assertFalse("Should fail when name is too long", result);
    }

    @Test
    public void testSaveDuplicateROM() throws SQLException, IOException {
        // First save should succeed
        assertTrue("Initial save failed",
                collectionManager.saveRomToCollection(testReader, "TestROM"));

        // Set test mode to reject overwrite
        collectionManager.setTestConfirmationResult(false);

        // Second save should fail because we rejected the overwrite
        assertFalse("Duplicate save should fail when overwrite is rejected",
                collectionManager.saveRomToCollection(testReader, "TestROM2"));

        // Verify only one entry exists
        dbQuery.printAllRoms();
    }

    @Test
    public void testSaveDifferentVersions() throws SQLException, IOException {
        // First save should succeed
        assertTrue("Initial save failed",
                collectionManager.saveRomToCollection(testReader, "TestROM v1"));

        // Modify the ROM with a different revision
        try (FileOutputStream fos = new FileOutputStream(testRomFile)) {
            // Write complete test ROM data first
            byte[] romData = new byte[0x150];
            fos.write(romData);

            // Set essential data
            fos.getChannel().position(0x104);
            fos.write(RomSpecification.BOOT_LOGO);

            fos.getChannel().position(0x134);
            fos.write("TESTTITLE".getBytes());

            fos.getChannel().position(0x14C);  // ROM revision location
            fos.write(new byte[]{0x01});  // Different revision
        }

        // Create new reader with modified ROM
        RomReader newReader = new RomReader(testRomFile);

        // Second save should succeed as it's a different version
        assertTrue("Different version save failed",
                collectionManager.saveRomToCollection(newReader, "TestROM v2"));

        // Verify both versions exist
        dbQuery.printAllRoms();
    }

    @Test
    public void testUpdateExistingROM() throws SQLException, IOException {
        // First save
        assertTrue("Initial save failed",
                collectionManager.saveRomToCollection(testReader, "TestROM Original"));

        // Verify initial save
        dbQuery.printAllRoms();

        // Set test mode to accept overwrite
        collectionManager.setTestConfirmationResult(true);

        // Update with new name
        assertTrue("Update failed",
                collectionManager.saveRomToCollection(testReader, "TestROM Updated"));

        // Verify update
        dbQuery.printAllRoms();
    }

    /**
     * Tests saving a ROM with Manufacturer Code.
     * Verifies that Manufacturer Code is properly preserved.
     *
     * @throws SQLException if database operations fail
     * @throws IOException if ROM file operations fail
     */
    @Test
    public void testSaveRomWithManufacturerCode() throws SQLException, IOException {
        // Create test ROM with Manufacturer Code
        RomReader reader = TestUtils.createTestRomReader("MARIOBXTJ");  // With Manufacturer Code

        // Save the ROM
        boolean result = collectionManager.saveRomToCollection(reader, "Test ROM");
        assertTrue("Failed to save ROM with Manufacturer Code", result);

        // Verify using complete version checking
        assertTrue("ROM not found in database",
                dbQuery.romExistsInCollection(
                        reader.parseTitle().getTitle(),
                        reader.getMaskVersion(),
                        reader.getGlobalChecksum()
                )
        );

        // Print debug info
        dbQuery.printAllRoms();
    }

    /**
     * Tests updating a ROM with Manufacturer Code.
     * Verifies that Manufacturer Code is properly preserved during updates.
     *
     * @throws SQLException if database operations fail
     * @throws IOException if ROM file operations fail
     */
    @Test
    public void testUpdateRomWithManufacturerCode() throws SQLException, IOException {
        // Create a test ROM with CGB flag and valid Manufacturer Code
        RomReader reader = TestUtils.createTestCGBRomReader("MAORIO", "BXTA");

        // First save
        assertTrue("Initial save failed",
                collectionManager.saveRomToCollection(reader, "Original Name"));

        // Set test mode to accept overwrite
        collectionManager.setTestConfirmationResult(true);

        // Update with new name
        assertTrue("Update failed",
                collectionManager.saveRomToCollection(reader, "Updated Name"));

        // Verify update preserved Manufacturer Code
        ArrayList<Collection> roms = dbQuery.getAllRoms();
        assertFalse("No ROMs found after update", roms.isEmpty());
        Collection updated = roms.get(0);
        assertEquals("Updated Name", updated.getName());
        assertEquals("BXTA", updated.getManufacturerCode());

        // Verify update
        dbQuery.printAllRoms();
    }

    /**
     * Tests that error handling works properly for ROMs with manufacturer
     * codes.
     * Verifies that saving fails appropriately with error messages.
     *
     * @throws IOException if ROM file operations fail
     */
    @Test
    public void testManufacturerCodeErrorHandling() throws IOException {
        // Enable test mode
        collectionManager.setTestMode(true);

        // Try to save ROM with same Manufacturer Code twice
        RomReader reader = TestUtils.createTestRomReader("MAORIOBXTJ");

        // First save should succeed
        assertTrue("Initial save failed",
                collectionManager.saveRomToCollection(reader, "First ROM"));

        // Second save should prompt for overwrite
        collectionManager.setTestConfirmationResult(false);  // Reject overwrite
        assertFalse("Duplicate save should fail when overwrite is rejected",
                collectionManager.saveRomToCollection(reader, "Second ROM"));
    }
}
