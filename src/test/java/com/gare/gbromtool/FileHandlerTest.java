package com.gare.gbromtool;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;

public class FileHandlerTest {

    private FileHandler fileHandler;

    @Before
    public void setUp() {
        fileHandler = new FileHandler();
    }

    @Test
    public void testValidateFileType() {
        File gbFile = new File("test.gb");
        File gbcFile = new File("test.gbc");
        File invalidFile = new File("test.txt");

        assertTrue(fileHandler.validateFileType(gbFile, new String[]{"gb", "gbc"}));
        assertTrue(fileHandler.validateFileType(gbcFile, new String[]{"gb", "gbc"}));
        assertFalse(fileHandler.validateFileType(invalidFile, new String[]{"gb", "gbc"}));
    }

    @Test
    public void testGetCurrentFileNameWithNoFile() {
        assertEquals("", fileHandler.getCurrentFileName());
    }
}
