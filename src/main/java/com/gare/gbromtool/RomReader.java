package com.gare.gbromtool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * This class contains methods for reading ROM data.
 *
 * @author Thomas Robinson 23191795
 */
public class RomReader {

    private final byte[] romData;
    byte CGBFlag[];
    boolean isNewCode;

    public RomReader(File romFile) throws IOException {
        // Read the ROM file into the romData byte array
        this.romData = Files.readAllBytes(romFile.toPath());
        if (this.romData.length < 0x150) {
            throw new IOException("File is too small to be a valid Game Boy ROM");
        }
    }

    /**
     * This method extracts the Logo from the ROM header.
     *
     * @return true if the logo matches, false otherwise
     */
    public boolean getLogo() {
        // The Logo is stored at offset 0x104 - 0x133
        byte[] logo = new byte[24];
        // Store offset as int
        int offsetStart = 0x104;
        int offsetEnd = 0x133;

        // The Logo expected in the ROM header to check against
        byte[] checkLogo = {
            (byte) 0xCE, (byte) 0xED, (byte) 0x66, (byte) 0x66, (byte) 0xCC, (byte) 0x0D, (byte) 0x00, (byte) 0x0B,
            (byte) 0x03, (byte) 0x73, (byte) 0x00, (byte) 0x83, (byte) 0x00, (byte) 0x0C, (byte) 0x00, (byte) 0x0D,
            (byte) 0x00, (byte) 0x08, (byte) 0x11, (byte) 0x1F, (byte) 0x88, (byte) 0x89, (byte) 0x00, (byte) 0x0E,
            (byte) 0xDC, (byte) 0xCC, (byte) 0x6E, (byte) 0xE6, (byte) 0xDD, (byte) 0xDD, (byte) 0xD9, (byte) 0x99,
            (byte) 0xBB, (byte) 0xBB, (byte) 0x67, (byte) 0x63, (byte) 0x6E, (byte) 0x0E, (byte) 0xEC, (byte) 0xCC,
            (byte) 0xDD, (byte) 0xDC, (byte) 0x99, (byte) 0x9F, (byte) 0xBB, (byte) 0xB9, (byte) 0x33, (byte) 0x3E
        };

        // Check if the Logo can fit starting from the offset
        if (offsetEnd > logo.length) {
            return false; // Out of bounds
        }

        // Compare bytes at 0x104 - 0x133 to expected Logo
        return Arrays.equals(Arrays.copyOfRange(logo, offsetStart, offsetEnd), checkLogo);
    }

    /**
     * This method extracts the Title from the ROM header.
     *
     * @return the Title as a string
     */
    public String getTitle() {
        // The Title is stored at offset 0x134 - 0x142 in the ROM
        byte[] titleBytes;  // 16 bytes for title
        int titleLength = (romData[0x143] == (byte) 0x80 || romData[0x143] == (byte) 0xC0) ? 15 : 16;

        titleBytes = new byte[titleLength];
        System.arraycopy(romData, 0x134, titleBytes, 0, titleLength);

        // Convert to string and clean up
        return new String(titleBytes, StandardCharsets.US_ASCII)
                .replaceAll("[^\\x20-\\x7E]", "") // Remove non-printable ASCII chars
                .trim();
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

    private boolean isCGBCompatible() {
        return romData[0x143] == (byte) 0x80 || romData[0x143] == (byte) 0xC0;
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
        byte licenseeCode[] = new byte[2];

        if (romData[0x14B] == 0x33) {
            System.arraycopy(romData, 0x144, licenseeCode, 0, 2);
            isNewCode = true;
        } else {
            System.arraycopy(romData, 0x14B, licenseeCode, 0, 1);
            isNewCode = false;
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
        // It might be possible to check the checksum's validity in realtime
        // using the same method that the boot ROM uses:
        // uint8_t checksum = 0;
        //for (uint16_t address = 0x0134; address <= 0x014C; address++) {
        //    checksum = checksum - rom[address] - 1;
        //}
        byte headerChecksum[] = new byte[1];

        System.arraycopy(romData, 0x14D, headerChecksum, 0, 1);

        return headerChecksum;
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

    private String cleanTitle(String rawTitle) {
        // Remove trailing nulls and spaces
        String cleaned = rawTitle.replaceAll("\\x00+$", "")
                .replaceAll("((_)_+|(\\x00)\\x00+|(\\s)\\s+)", "$2$3$4")
                .replaceAll("\\x00", "_")
                .trim();

        // Filter non-printable characters
        return cleaned.chars()
                .filter(ch -> Character.isLetterOrDigit(ch) || ch == ' ' || ch == '_' || ch == '-')
                .mapToObj(ch -> String.valueOf((char) ch))
                .collect(Collectors.joining())
                .trim();
    }

    /**
     * This method splits the manufacturer code from the ROM title.
     *
     * Attempts to solve the problem where some CGB ROMs have a manufacturer
     * code added to the end of the allotted title string without any obvious
     * separation marks in the ROM header.
     *
     * Based on code Python code from:
     * https://github.com/lesserkuma/FlashGBX/blob/master/FlashGBX/RomFileDMG.py
     *
     * @return the ROM title and manufacturer code as two strings in a RomTitle
     */
    public RomTitle parseTitle() {
        boolean isCGB = (romData[0x143] == (byte) 0x80 || romData[0x143] == (byte) 0xC0);
        String rawTitle = getTitle();

        if (isCGB && rawTitle.length() >= 15) {
            String potentialCode = rawTitle.substring(rawTitle.length() - 4);
            if (isValidManufacturerCode(potentialCode)) {
                return new RomTitle(
                        rawTitle.substring(0, rawTitle.length() - 4).trim(),
                        potentialCode
                );
            }
        }

        return new RomTitle(rawTitle, "");
    }

    private boolean isValidManufacturerCode(String code) {
        if (code.length() != 4) {
            return false;
        }
        char first = code.charAt(0);
        char last = code.charAt(3);
        return "ABHKV".indexOf(first) >= 0 && "ABDEFIJKPSUXY".indexOf(last) >= 0;
    }

}
