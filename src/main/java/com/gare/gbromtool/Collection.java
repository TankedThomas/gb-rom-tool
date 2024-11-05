package com.gare.gbromtool;

/**
 * Entity class representing a row in the Collection table of the database.
 * This class provides an object-oriented representation of ROM data that maps
 * directly to database columns.
 * All fields are immutable to ensure data integrity.
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
    private final String manufacturerCode;
    private final byte[] typeCode;
    private final byte[] romRev;
    private final int romSizeCode;
    private final int ramSizeCode;
    private final boolean sgbFlag;
    private final byte[] cgbFlag;
    private final int destCode;
    private final byte[] licenseeCode;
    private final byte[] headerChecksum;
    private final boolean headerChecksumValid;
    private final byte[] globalChecksum;
    private final boolean globalChecksumValid;
    private final boolean bootLogoValid;

    /**
     * Creates a new Collection entity with the specified ROM data.
     *
     * @param name user-provided name for the ROM
     * @param title ROM Title
     * @param manufacturerCode ROM Manufacturer Code
     * @param typeCode Cartridge type code
     * @param romRev ROM Revision number
     * @param romSizeCode ROM Size code
     * @param ramSizeCode RAM Size code
     * @param sgbFlag SGB flag
     * @param cgbFlag CGB flag
     * @param destCode Destination Code
     * @param licenseeCode Licensee Code
     * @param headerChecksum Header Checksum
     * @param headerChecksumValid Header Checksum validity
     * @param globalChecksum Global Checksum
     * @param globalChecksumValid Global Checksum validity
     * @param bootLogoValid Boot Logo validity
     * @throws IllegalArgumentException if name or title exceed maximum length
     */
    public Collection(
            String name,
            String title,
            String manufacturerCode,
            byte[] typeCode,
            byte[] romRev,
            int romSizeCode,
            int ramSizeCode,
            boolean sgbFlag,
            byte[] cgbFlag,
            int destCode,
            byte[] licenseeCode,
            byte[] headerChecksum,
            boolean headerChecksumValid,
            byte[] globalChecksum,
            boolean globalChecksumValid,
            boolean bootLogoValid) {

        if (name == null || title == null) {
            throw new IllegalArgumentException("Name and title cannot be null");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Name exceeds maximum length of " + MAX_NAME_LENGTH);
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "Title exceeds maximum length of " + MAX_TITLE_LENGTH);
        }

        this.name = name;
        this.title = title;
        this.manufacturerCode = manufacturerCode;
        this.typeCode = typeCode.clone();
        this.romRev = romRev.clone();
        this.romSizeCode = romSizeCode;
        this.ramSizeCode = ramSizeCode;
        this.sgbFlag = sgbFlag;
        this.cgbFlag = cgbFlag.clone();
        this.destCode = destCode;
        this.licenseeCode = licenseeCode.clone();
        this.headerChecksum = headerChecksum.clone();
        this.headerChecksumValid = headerChecksumValid;
        this.globalChecksum = globalChecksum.clone();
        this.globalChecksumValid = globalChecksumValid;
        this.bootLogoValid = bootLogoValid;
    }

    // Getters
    /**
     * @return user-provided name for this ROM
     */
    public String getName() {
        return name;
    }

    /**
     * @return Title extracted from the ROM header
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return Manufacturer Code extracted from the ROM header, if it exists
     */
    public String getManufacturerCode() {
        return manufacturerCode;
    }

    /**
     * @return a copy of the Cartridge Type code
     */
    public byte[] getTypeCode() {
        return typeCode.clone();
    }

    /**
     * @return a copy of the ROM Revision number
     */
    public byte[] getRomRev() {
        return romRev.clone();
    }

    /**
     * @return ROM Size code
     */
    public int getRomSizeCode() {
        return romSizeCode;
    }

    /**
     * @return RAM Size code
     */
    public int getRamSizeCode() {
        return ramSizeCode;
    }

    /**
     * @return SGB Compatibility flag
     */
    public boolean getSgbFlag() {
        return sgbFlag;
    }

    /**
     * @return a copy of the CGB Compatibility flag
     */
    public byte[] getCgbFlag() {
        return cgbFlag.clone();
    }

    /**
     * @return Destination code
     */
    public int getDestCode() {
        return destCode;
    }

    /**
     * @return a copy of the Licensee Code
     */
    public byte[] getLicenseeCode() {
        return licenseeCode.clone();
    }

    /**
     * @return a copy of the Header Checksum
     */
    public byte[] getHeaderChecksum() {
        return headerChecksum.clone();
    }

    /**
     * @return true if the Header Checksum is valid, false otherwise
     */
    public boolean isHeaderChecksumValid() {
        return headerChecksumValid;
    }

    /**
     * @return a copy of the Global Checksum
     */
    public byte[] getGlobalChecksum() {
        return globalChecksum.clone();
    }

    /**
     * @return true if the Global Checksum is valid, false otherwise
     */
    public boolean isGlobalChecksumValid() {
        return globalChecksumValid;
    }

    /**
     * @return true if the Boot Logo is valid, false otherwise
     */
    public boolean isBootLogoValid() {
        return bootLogoValid;
    }

    /**
     * Creates a Collection entity from a RomReader instance.
     * This factory method simplifies the creation of Collection objects
     * by extracting all necessary data from a ROM file.
     *
     * @param reader the RomReader containing ROM data
     * @param name user-provided name for the ROM
     * @return a new Collection entity containing the ROM data
     * @throws IllegalArgumentException if the name is too long
     */
    public static Collection fromRomReader(RomReader reader, String name) {
        RomTitle titleInfo = reader.parseTitle();
        return new Collection(
                name,
                titleInfo.getTitle(),
                titleInfo.getManufacturerCode(),
                reader.getCartridgeType().getBytes(),
                reader.getMaskVersion(),
                reader.getROMSize(),
                reader.getRAMSize(),
                reader.getSGBFlag(),
                reader.getCGBFlag(),
                reader.getDestinationCode(),
                reader.getLicenseeCode(),
                reader.getHeaderChecksum(),
                reader.verifyHeaderChecksum(),
                reader.getGlobalChecksum(),
                reader.verifyGlobalChecksum(),
                reader.getLogo()
        );
    }
}
