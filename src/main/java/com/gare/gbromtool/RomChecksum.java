package com.gare.gbromtool;

/**
 * This class handles checksum calculation and verification for RomReader.
 *
 * @author Thomas Robinson 23191795
 */
public class RomChecksum {

    private final byte[] romData;

    public RomChecksum(byte[] romData) {
        this.romData = romData;
    }

    /**
     * This method calculates and compares the Header Checksum using the same
     * method that the Boot ROM uses.
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
     * This method calculates and compares the Global Checksum.
     *
     * @return true if the checksums match, false otherwise
     */
    public boolean verifyGlobalChecksum() {
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

    public String getStoredHeaderChecksum() {
        return String.format("%02X", romData[0x014D] & 0xFF);
    }

    public String getStoredGlobalChecksum() {
        return String.format("%04X", ((romData[0x014E] & 0xFF) << 8) | (romData[0x014F] & 0xFF));
    }

}
