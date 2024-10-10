package com.gare.gbromtool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * This class contains methods for reading ROMs.
 *
 * @author Thomas Robinson 23191795
 */
public class RomReader {

    private byte[] romData;

    public RomReader(File romFile) throws IOException {
        // TODO: Read the ROM file into the romData byte array
        // Hint: Use Files.readAllBytes(Path)
        
    }

    public String getTitle() {
        // TODO: Extract the title from the ROM header
        // Hint: The title is stored at a specific offset in the ROM
        return "";
    }

    public int getCartridgeType() {
        // TODO: Extract the cartridge type from the ROM header
        // Hint: The cartridge type is stored at a specific offset in the ROM
        return 0;
    }

    public int getROMSize() {
        // TODO: Extract the ROM size from the ROM header
        // Hint: The ROM size is stored at a specific offset in the ROM
        return 0;
    }

    // TODO: Add more methods to extract other relevant information from the ROM
}
