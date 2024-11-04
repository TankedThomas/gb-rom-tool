package com.gare.gbromtool;

/**
 * Handles checksum calculation and verification for ROM data.
 * Implements Header Checksum and Global Checksum verification according to
 * ROM specifications.
 *
 * @author Thomas Robinson 23191795
 */
public class RomChecksum {

    private final byte[] romData;
    private final RomReader reader;

    /**
     * Creates a new checksum validator for the given ROM data.
     *
     * @param romData the ROM data to validate
     * @param reader the RomReader instance for validation flags
     */
    public RomChecksum(byte[] romData, RomReader reader) {
        this.romData = romData;
        this.reader = reader;
    }

    /**
     * Calculates and compares the Header Checksum using the same method that
     * the Boot ROM uses.
     * For database entries, returns true without verification.
     *
     * Boot ROM code:
     * uint8_t checksum = 0;
     * for (uint16_t address = 0x0134; address <= 0x014C; address++) {
     * checksum = checksum - rom[address] - 1;
     * }
     *
     * @return true if the checksums match, false otherwise
     */
    public boolean verifyHeaderChecksum() {
        if (reader.getLoadFromDatabase()) {
            return true;
        }

        int checksum = 0;
        for (int address = 0x0134; address <= 0x014C; address++) {
            checksum = checksum - (romData[address] & 0xFF) - 1;
        }
        // Get lower 8 bits only
        checksum = checksum & 0xFF;

        // Compare with stored checksum
        return (checksum == (romData[0x014D] & 0xFF));
    }

    /**
     * Calculates and compares the Global Checksum.
     * For database entries, returns true without verification.
     *
     * @return true if the checksums match, false otherwise
     */
    public boolean verifyGlobalChecksum() {
        if (reader.getLoadFromDatabase()) {
            return true;
        }

        int checksum = 0;
        // Sum all bytes except the checksum bytes themselves
        for (int i = 0; i < romData.length; i++) {
            if (i != 0x014E && i != 0x014F) {
                checksum += (romData[i] & 0xFF);
            }
        }
        // Get 16 bits only
        checksum = checksum & 0xFFFF;

        // Get stored checksum (big-endian)
        int storedChecksum = ((romData[0x014E] & 0xFF) << 8) | (romData[0x014F] & 0xFF);

        return checksum == storedChecksum;
    }

    /**
     * Gets the stored Header Checksum as a formatted string.
     *
     * @return the Header Checksum in hexadecimal format
     */
    public String getStoredHeaderChecksum() {
        return String.format("%02X", romData[0x014D] & 0xFF);
    }

    /**
     * Gets the stored Global Checksum as a formatted string.
     *
     * @return the Global Checksum in hexadecimal format
     */
    public String getStoredGlobalChecksum() {
        return String.format("%04X", ((romData[0x014E] & 0xFF) << 8) | (romData[0x014F] & 0xFF));
    }

}
