package com.gare.gbromtool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * This class contains methods for reading ROM data.
 *
 * @author Thomas Robinson 23191795
 */
public class RomReader {

    private final byte[] romData;
    private final RomChecksum checksum;
    byte CGBFlag[];

    public RomReader(File romFile) throws IOException {
        // Read the ROM file into the romData byte array
        this.romData = Files.readAllBytes(romFile.toPath());
        if (this.romData.length < 0x150) {
            throw new IOException("File is too small to be a valid Game Boy ROM");
        }
        this.checksum = new RomChecksum(this.romData);
    }

    /**
     * This method extracts the Logo from the ROM header.
     *
     * @return true if the logo matches, false otherwise
     */
    public boolean getLogo() {
        // The Logo is stored at offset 0x104 - 0x133 (48 bytes)
        byte[] logo = Arrays.copyOfRange(romData, 0x104, 0x134);

        // The Logo expected in the ROM header to check against
        byte[] checkLogo = RomSpecification.BOOT_LOGO;

        System.out.println("Extracted logo bytes: " + HexFormat.of().formatHex(logo));
        System.out.println("Expected logo bytes: " + HexFormat.of().formatHex(checkLogo));

        // Compare bytes at 0x104 - 0x133 to expected Logo
        return Arrays.equals(logo, checkLogo);
    }

    /**
     * This method extracts the Title from the ROM header.
     *
     * @return the Title as a string
     */
    public String getTitle() {
        // The Title is stored at offset 0x134 - 0x142 in the ROM
        byte[] titleBytes = new byte[15];  // Always get 15 bytes for GBC titles
        System.arraycopy(romData, 0x134, titleBytes, 0, 15);

        // Debug output of raw bytes
        System.out.println("Raw title bytes: " + HexFormat.of().formatHex(titleBytes));

        // Convert to string and clean up
        return new String(titleBytes, StandardCharsets.US_ASCII);
    }

    public RomTitle parseTitle() {
        return RomTitle.parseTitle(romData);
    }

    /**
     * This method extracts the CGB Flag from the ROM header.
     *
     * @return the CGB Flag as a byte
     */
    public byte[] getCGBFlag() {
        // If it exists, the CGB Flag is stored at offset 0x143 in the ROM
        CGBFlag = new byte[1];

        System.arraycopy(romData, 0x143, CGBFlag, 0, 1);

        return CGBFlag;
    }

    /**
     * This method extracts the SGB flag from the ROM header.
     *
     * @return true if the flag is present, false otherwise
     */
    public boolean getSGBFlag() {
        // The SGB flag is stored at offset 0x146 in the ROM
        // 0x03 is the only valid flag - everything else is ignored      
        return Byte.toUnsignedInt(romData[0x146]) == 0x03;
    }

    /**
     * This method extracts the Cartridge Type from the ROM header.
     *
     * @return the Cartridge Type as a string
     */
    public String getCartridgeType() {
        // The Cartridge Type is stored at offset 0x147 in the ROM
        byte typeByte[] = new byte[1];
        String cartridgeType;

        System.arraycopy(romData, 0x147, typeByte, 0, 1);

        cartridgeType = new String(typeByte);

        return cartridgeType;
    }

    /**
     * This method extracts the Licensee Code from the ROM header.
     *
     * If the Old Licensee Code is 0x33, the New Licensee Code is used instead
     *
     * The Licensee Code is stored at either 0x14B (old code)
     * or 0x144-0x145 (new code)
     *
     * The New Licensee Code should be converted to ASCII when returned
     *
     * @return the Licensee Code as 1-2 bytes, depending on type
     */
    public byte[] getLicenseeCode() {
        byte licenseeCode[];

        if (romData[0x14B] == 0x33) {
            licenseeCode = new byte[2];
            System.arraycopy(romData, 0x144, licenseeCode, 0, 2);
        } else {
            licenseeCode = new byte[1];
            System.arraycopy(romData, 0x14B, licenseeCode, 0, 1);
        }

        return licenseeCode;
    }

    /**
     * This method extracts the ROM size from the ROM header as an integer.
     * ROM size is stored as hexadecimal but only 00-08 are used,
     * so using an integer simplifies this method.
     *
     * @return the ROM size as an unsigned integer
     */
    public int getROMSize() {
        // The ROM size is stored at offset 0x148 in the ROM
        return Byte.toUnsignedInt(romData[0x148]);
    }

    /**
     * This method extracts the RAM size from the ROM header as an integer.
     * RAM size is stored as a hexadecimal but only 00-05 are used,
     * so using an integer simplifies this method.
     *
     * @return the ROM size as an unsigned integer
     */
    public int getRAMSize() {
        // The RAM size is stored at offset 0x149 in the ROM
        return Byte.toUnsignedInt(romData[0x149]);
    }

    /**
     * This method extracts the Destination Code from the ROM header as an
     * integer.
     * Destination Code is stored as hexadecimal but only 00-01 are used,
     * so using an integer simplifies this method.
     *
     * @return the Destination Code as an unsigned integer
     */
    public int getDestinationCode() {
        // The Destination Code is stored at offset 0x14A in the ROM
        return Byte.toUnsignedInt(romData[0x14A]);
    }

    /**
     * This method extracts the Mask ROM version number from the ROM header.
     *
     * @return the Mask ROM Version as a byte
     */
    public byte[] getMaskVersion() {
        // The Mask ROM version number is stored at offset 0x14C
        byte maskVersion[] = new byte[1];

        System.arraycopy(romData, 0x14C, maskVersion, 0, 1);

        return maskVersion;
    }

    /**
     * This method extracts the Header Checksum from the ROM Header.
     * The Header Checksum is stored at offset 0x14D
     * This byte contains an 8-bit checksum computed from the cartridge header
     * bytes $0134–014C
     *
     * @return the Header Checksum as a byte
     */
    public byte[] getHeaderChecksum() {
        byte headerChecksum[] = new byte[1];

        System.arraycopy(romData, 0x14D, headerChecksum, 0, 1);

        return headerChecksum;
    }

    public boolean verifyHeaderChecksum() {
        return checksum.verifyHeaderChecksum();
    }

    public String getStoredHeaderChecksum() {
        return checksum.getStoredHeaderChecksum();
    }

    /**
     * This method extracts the Global Checksum from the ROM Header.
     * The Global Checksum is stored at offset 0x14E - 0x14F
     * These bytes contains a 16-bit checksum computed from all bytes in the ROM
     * (except these checksum bytes)
     *
     * @return the Global Checksum as two bytes
     */
    public byte[] getGlobalChecksum() {
        byte globalChecksum[] = new byte[2];

        System.arraycopy(romData, 0x14E, globalChecksum, 0, 2);

        return globalChecksum;
    }

    public boolean verifyGlobalChecksum() {
        return checksum.verifyGlobalChecksum();
    }

    public String getStoredGlobalChecksum() {
        return checksum.getStoredGlobalChecksum();
    }
}
