package com.gare.gbromtool;

import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests for ROM checksum calculation and verification functionality.
 * Verifies that both header and global checksums are correctly calculated
 * and validated according to GB specifications.
 *
 * GB ROMs use two different checksums:
 * 1. Header Checksum (1 byte at 0x14D) - validates header data integrity
 * 2. Global Checksum (2 bytes at 0x14E-0x14F) - validates entire ROM content
 *
 *
 * @author Thomas Robinson 23191795
 */
public class RomChecksumTest {

    /**
     * Calculates a valid header checksum using the GB Boot ROM algorithm.
     *
     * The algorithm is:
     * uint8_t checksum = 0;
     * for (uint16_t address = 0x0134; address <= 0x014C; address++) {
     * checksum = checksum - rom[address] - 1;
     * }
     *
     * This method mimics how the actual Game Boy hardware verifies cartridge
     * header data.
     * A mismatch between the calculated checksum and the stored checksum at
     * 0x14D
     * would cause the real hardware to fail to boot.
     *
     * @param romData The complete ROM data
     * @return A single byte containing the calculated header checksum
     */
    private byte[] createValidHeaderChecksum(byte[] romData) {
        int checksum = 0;
        for (int address = 0x0134; address <= 0x014C; address++) {
            checksum = checksum - (romData[address] & 0xFF) - 1;
        }
        return new byte[]{(byte) (checksum & 0xFF)};
    }

    /**
     * Calculates a valid global checksum using the GB cartridge specification.
     *
     * The algorithm sums all bytes in the ROM except the two checksum bytes
     * themselves (0x014E-0x014F).
     * The result is a 16-bit value stored in big-endian format.
     *
     * @param romData The complete ROM data
     * @return Two bytes containing the calculated global checksum in big-endian
     * format
     */
    private byte[] createValidGlobalChecksum(byte[] romData) {
        int checksum = 0;
        for (int i = 0; i < romData.length; i++) {
            if (i != 0x014E && i != 0x014F) {
                checksum += (romData[i] & 0xFF);
            }
        }
        return new byte[]{(byte) ((checksum >> 8) & 0xFF), (byte) (checksum & 0xFF)};
    }

    /**
     * Tests header checksum verification with valid data.
     * Header checksum is calculated over bytes 0x134-0x14C.
     *
     * @throws IOException if ROM file creation fails
     */
    @Test
    public void testVerifyHeaderChecksum() throws IOException {
        // Create ROM with valid header checksum
        byte[] romData = new byte[0x150];  // Minimum ROM size
        System.arraycopy(RomSpecification.BOOT_LOGO, 0, romData, 0x104, RomSpecification.BOOT_LOGO.length);
        System.arraycopy("TESTGAME".getBytes(), 0, romData, 0x134, "TESTGAME".length());

        // Calculate and set valid header checksum
        byte[] headerChecksum = createValidHeaderChecksum(romData);
        System.arraycopy(headerChecksum, 0, romData, 0x14D, 1);

        File testFile = TestUtils.createTestRom(romData);
        RomReader reader = new RomReader(testFile);

        assertTrue("Valid header checksum should verify", reader.verifyHeaderChecksum());
    }

    /**
     * Tests global checksum verification with valid data.
     * Global checksum is calculated over all ROM bytes except the checksum
     * bytes themselves.
     *
     * @throws IOException if ROM file creation fails
     */
    @Test
    public void testVerifyGlobalChecksum() throws IOException {
        // Create ROM with valid global checksum
        byte[] romData = new byte[0x150];  // Minimum ROM size
        System.arraycopy(RomSpecification.BOOT_LOGO, 0, romData, 0x104, RomSpecification.BOOT_LOGO.length);
        System.arraycopy("TESTGAME".getBytes(), 0, romData, 0x134, "TESTGAME".length());

        // Calculate and set valid checksums
        byte[] headerChecksum = createValidHeaderChecksum(romData);
        System.arraycopy(headerChecksum, 0, romData, 0x14D, 1);

        byte[] globalChecksum = createValidGlobalChecksum(romData);
        System.arraycopy(globalChecksum, 0, romData, 0x14E, 2);

        File testFile = TestUtils.createTestRom(romData);
        RomReader reader = new RomReader(testFile);

        assertTrue("Valid global checksum should verify", reader.verifyGlobalChecksum());
    }

    /**
     * Tests detection of invalid header checksum.
     * Verifies that the system correctly identifies corrupted header data.
     *
     * @throws IOException if ROM file creation fails
     */
    @Test
    public void testInvalidHeaderChecksum() throws IOException {
        byte[] romData = new byte[0x150];  // Minimum ROM size
        System.arraycopy(RomSpecification.BOOT_LOGO, 0, romData, 0x104, RomSpecification.BOOT_LOGO.length);
        System.arraycopy("TESTGAME".getBytes(), 0, romData, 0x134, "TESTGAME".length());

        // Set invalid header checksum
        romData[0x14D] = (byte) 0xFF;

        File testFile = TestUtils.createTestRom(romData);
        RomReader reader = new RomReader(testFile);

        assertFalse("Should detect invalid header checksum", reader.verifyHeaderChecksum());
    }

    /**
     * Tests detection of invalid global checksum.
     * Verifies that the system correctly identifies corrupted ROM data.
     *
     * @throws IOException if ROM file creation fails
     */
    @Test
    public void testInvalidGlobalChecksum() throws IOException {
        byte[] romData = new byte[0x150];  // Minimum ROM size
        System.arraycopy(RomSpecification.BOOT_LOGO, 0, romData, 0x104, RomSpecification.BOOT_LOGO.length);
        System.arraycopy("TESTGAME".getBytes(), 0, romData, 0x134, "TESTGAME".length());

        // Set invalid global checksum
        romData[0x14E] = (byte) 0xFF;
        romData[0x14F] = (byte) 0xFF;

        File testFile = TestUtils.createTestRom(romData);
        RomReader reader = new RomReader(testFile);

        assertFalse("Should detect invalid global checksum", reader.verifyGlobalChecksum());
    }
}
