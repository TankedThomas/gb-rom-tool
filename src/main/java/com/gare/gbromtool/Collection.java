package com.gare.gbromtool;

/**
 * Entity class representing a row in the Collection table of the database.
 * This class provides an object-oriented representation of ROM data that maps
 * directly to database columns. All fields are immutable to ensure data
 * integrity.
 *
 * @author Thomas Robinson 23191795
 */
public class Collection {

    // Constants for field constraints
    // Maximum length for the user-provided name field
    public static final int MAX_NAME_LENGTH = 200;
    // Maximum length for the ROM title field
    public static final int MAX_TITLE_LENGTH = 100;

    // Fields matching database columns
    private final String title;
    private final String name;
    private final byte[] typeCode;
    private final byte[] romRev;
    private final int romSizeCode;
    private final int ramSizeCode;
    private final boolean sgbFlag;
    private final byte[] cgbFlag;
    private final int destCode;
    private final byte[] licenseeCode;
    private final byte[] headerChecksum;
    private final byte[] globalChecksum;

    /**
     * Creates a new Collection entity with the specified ROM data.
     *
     * @param name User-provided name for the ROM
     * @param title ROM title
     * @param typeCode Cartridge type code
     * @param romRev ROM revision number
     * @param romSizeCode ROM size code
     * @param ramSizeCode RAM size code
     * @param sgbFlag Super Game Boy flag
     * @param cgbFlag Game Boy Color flag
     * @param destCode Destination code
     * @param licenseeCode Licensee code
     * @param headerChecksum Header checksum
     * @param globalChecksum Global checksum
     */
    public Collection(
            String name,
            String title,
            byte[] typeCode,
            byte[] romRev,
            int romSizeCode,
            int ramSizeCode,
            boolean sgbFlag,
            byte[] cgbFlag,
            int destCode,
            byte[] licenseeCode,
            byte[] headerChecksum,
            byte[] globalChecksum) {
        this.name = name;
        this.title = title;
        this.typeCode = typeCode;
        this.romRev = romRev;
        this.romSizeCode = romSizeCode;
        this.ramSizeCode = ramSizeCode;
        this.sgbFlag = sgbFlag;
        this.cgbFlag = cgbFlag;
        this.destCode = destCode;
        this.licenseeCode = licenseeCode;
        this.headerChecksum = headerChecksum;
        this.globalChecksum = globalChecksum;
    }

    // Getters
    /**
     * @return The user-provided name for this ROM
     */
    public String getName() {
        return name;
    }

    /**
     * @return The title extracted from the ROM header
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return A copy of the cartridge type code
     */
    public byte[] getTypeCode() {
        return typeCode.clone();
    }

    /**
     * @return A copy of the ROM revision number
     */
    public byte[] getRomRev() {
        return romRev.clone();
    }

    /**
     * @return The ROM size code
     */
    public int getRomSizeCode() {
        return romSizeCode;
    }

    /**
     * @return The RAM size code
     */
    public int getRamSizeCode() {
        return ramSizeCode;
    }

    /**
     * @return The Super Game Boy compatibility flag
     */
    public boolean getSgbFlag() {
        return sgbFlag;
    }

    /**
     * @return A copy of the Game Boy Color compatibility flag
     */
    public byte[] getCgbFlag() {
        return cgbFlag.clone();
    }

    /**
     * @return The destination code
     */
    public int getDestCode() {
        return destCode;
    }

    /**
     * @return A copy of the licensee code
     */
    public byte[] getLicenseeCode() {
        return licenseeCode.clone();
    }

    /**
     * @return A copy of the header checksum
     */
    public byte[] getHeaderChecksum() {
        return headerChecksum.clone();
    }

    /**
     * @return A copy of the global checksum
     */
    public byte[] getGlobalChecksum() {
        return globalChecksum.clone();
    }

    /**
     * Creates a Collection entity from a RomReader instance.
     * This factory method simplifies the creation of Collection objects
     * by extracting all necessary data from a ROM file.
     *
     * @param reader The RomReader containing ROM data
     * @param name User-provided name for the ROM
     * @return A new Collection entity containing the ROM data
     * @throws IllegalArgumentException if the name is too long
     */
    public static Collection fromRomReader(RomReader reader, String name) {
        RomTitle titleInfo = reader.parseTitle();
        return new Collection(
                name,
                titleInfo.getTitle(),
                reader.getCartridgeType().getBytes(),
                reader.getMaskVersion(),
                reader.getROMSize(),
                reader.getRAMSize(),
                reader.getSGBFlag(),
                reader.getCGBFlag(),
                reader.getDestinationCode(),
                reader.getLicenseeCode(),
                reader.getHeaderChecksum(),
                reader.getGlobalChecksum()
        );
    }
}
