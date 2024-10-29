package com.gare.gbromtool;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;

/**
 * Test class for FileHandler.
 * Tests file validation and name handling functionality.
 *
 * @author Thomas Robinson 23191795
 */
public class FileHandlerTest {

    private FileHandler fileHandler;

    /**
     * Sets up each test with a fresh FileHandler instance.
     */
    @Before
    public void setUp() {
        fileHandler = new FileHandler();
    }

    /**
     * Tests file type validation for valid and invalid extensions.
     */
    @Test
    public void testValidateFileType() {
        File gbFile = new File("test.gb");
        File gbcFile = new File("test.gbc");
        File invalidFile = new File("test.txt");

        assertTrue(fileHandler.validateFileType(gbFile, new String[]{"gb", "gbc"}));
        assertTrue(fileHandler.validateFileType(gbcFile, new String[]{"gb", "gbc"}));
        assertFalse(fileHandler.validateFileType(invalidFile, new String[]{"gb", "gbc"}));
    }

    /**
     * Tests that getCurrentFileName returns empty string when no file is
     * loaded.
     */
    @Test
    public void testGetCurrentFileNameWithNoFile() {
        assertEquals("", fileHandler.getCurrentFileName());
    }
}
