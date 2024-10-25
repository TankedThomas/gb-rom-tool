package com.gare.gbromtool;

import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas
 */
public class GbRomToolTest {
    
    public GbRomToolTest() {
    }

    @org.junit.BeforeClass
    public static void setUpClass() throws Exception {
    }

    @org.junit.AfterClass
    public static void tearDownClass() throws Exception {
    }

    @org.junit.Before
    public void setUp() throws Exception {
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }
    
//    @BeforeClass
//    public static void setUpClass() {
//    }
//    
//    @AfterClass
//    public static void tearDownClass() {
//    }
//    
//    @Before
//    public void setUp() {
//    }
//    
//    @After
//    public void tearDown() {
//    }

    /**
     * Test of validateFileType method, of class GbRomTool.
     */
    @org.junit.Test
    public void testValidateFileType() {
        System.out.println("validateFileType");
        File file = null;
        String[] extensions = null;
        boolean expResult = false;
        boolean result = GbRomTool.validateFileType(file, extensions);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of main method, of class GbRomTool.
     */
    @org.junit.Test
    public void testMain() {
        System.out.println("main");
        String[] args = null;
        GbRomTool.main(args);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
