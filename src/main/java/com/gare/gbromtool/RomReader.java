package com.gare.gbromtool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import javax.swing.JFileChooser;

/**
 * This class contains methods for reading ROMs.
 *
 * @author Thomas Robinson 23191795
 */
public class RomReader {

    private final byte[] romData;
    private final JFileChooser chooseRom;
    private RomReader romReader;
    byte CGBFlag[];
    boolean isNewCode;

    public RomReader(File romFile) throws IOException {
        // Read the ROM file into the romData byte array
        this.romData = Files.readAllBytes(romFile.toPath());
        if (this.romData.length < 0x150) {
            throw new IOException("File is too small to be a valid Game Boy ROM");
        }
    }

    public boolean getLogo() {
        // Extract the Logo from the ROM header
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

    public String getTitle() {
        // Extract the Title from the ROM header
        // The Title is stored at offset 0x134 - 0x142 in the ROM
        byte[] titleBytes = new byte[16];  // 16 bytes for title
        String title;

        // Check for CGB flag
        if (romData[0x143] == 0x80 | romData[0x143] == 0xC0) {
            // CGB flag exists - copy 15 title bytes into array
            System.arraycopy(romData, 0x134, titleBytes, 0, 15);
            getCGBFlag();
        } else {
            // CGB flag does not exist - copy 16 title bytes into array
            System.arraycopy(romData, 0x134, titleBytes, 0, 16);
        }

        // Convert title to string and remove empty bytes
        title = new String(titleBytes, StandardCharsets.US_ASCII).replace("\0", "").trim();

        return title;
    }

    public byte[] getCGBFlag() {
        // Extract the CGB Flag from the ROM header
        // If it exists, the CGB Flag is stored at offset 0x143 in the ROM
        CGBFlag = new byte[1];

        System.arraycopy(romData, 0x143, CGBFlag, 0, 1);

        return CGBFlag;
    }

    public boolean getSGBFlag() {
        // Extract the SGB flag from the ROM header
        // The SGB flag is stored at offset 0x146 in the ROM
        // 0x03 is the only valid flag - everything else is ignored        
        return Byte.toUnsignedInt(romData[0x146]) == 0x03;
    }

    public String getCartridgeType() {
        // Extract the cartridge type from the ROM header
        // The cartridge type is stored at offset 0x147 in the ROM
        byte typeByte[] = new byte[1];

        String cartridgeType;

        System.arraycopy(romData, 0x147, typeByte, 0, 1);

        cartridgeType = new String(typeByte);

        return cartridgeType;
    }

    public byte[] getLicenseeCode() {

        // Extract the Licensee Code from the ROM header
        // The Licensee Code is stored at either 0x14B (old code) 
        // or 0x144-0x145 (new code)
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

    public int getROMSize() {
        // Extract the ROM size from the ROM header
        // The ROM size is stored at offset 0x148 in the ROM

        return Byte.toUnsignedInt(romData[0x148]);
    }

    public int getRAMSize() {
        // Extract the RAM size from the ROM header
        // The RAM size is stored at offset 0x149 in the ROM
        return Byte.toUnsignedInt(romData[0x149]);
    }

    public int getDestinationCode() {
        // Extract the Destination Code from the ROM header
        // The Destination Code is stored at offset 0x14A in the ROM
        return Byte.toUnsignedInt(romData[0x14A]);
    }

    public byte[] getMaskVersion() {
        // Extract the Mask ROM version number from the ROM header
        // The Mask ROM version number is stored at offset 0x14C
        byte maskVersion[] = new byte[1];

        System.arraycopy(romData, 0x14C, maskVersion, 0, 1);

        return maskVersion;
    }

    public byte[] getHeaderChecksum() {
        // Extract the Header Checksum from the ROM Header
        // The Header Checksum is stored at offset 0x14D
        // This byte contains an 8-bit checksum computed from the cartridge header bytes $0134–014C

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

    public byte[] getGlobalChecksum() {
        // Extract the Header Checksum from the ROM Header
        // The Header Checksum is stored at offset 0x14E - 0x14F
        // This byte contains an 8-bit checksum computed from the cartridge header bytes $0134–014C

        byte globalChecksum[] = new byte[2];

        System.arraycopy(romData, 0x14E, globalChecksum, 0, 2);

        return globalChecksum;
    }

}
