package com.gare.gbromtool;

/**
 * Defines ROM specifications including header offsets and logo data.
 * Contains constants and utilities for working with ROM data.
 *
 * @author Thomas Robinson 23191795
 */
public class RomSpecification {

    // BOOT_LOGO is shared and immutable
    public static final byte[] BOOT_LOGO = {
        (byte) 0xCE, (byte) 0xED, (byte) 0x66, (byte) 0x66, (byte) 0xCC, (byte) 0x0D, (byte) 0x00, (byte) 0x0B,
        (byte) 0x03, (byte) 0x73, (byte) 0x00, (byte) 0x83, (byte) 0x00, (byte) 0x0C, (byte) 0x00, (byte) 0x0D,
        (byte) 0x00, (byte) 0x08, (byte) 0x11, (byte) 0x1F, (byte) 0x88, (byte) 0x89, (byte) 0x00, (byte) 0x0E,
        (byte) 0xDC, (byte) 0xCC, (byte) 0x6E, (byte) 0xE6, (byte) 0xDD, (byte) 0xDD, (byte) 0xD9, (byte) 0x99,
        (byte) 0xBB, (byte) 0xBB, (byte) 0x67, (byte) 0x63, (byte) 0x6E, (byte) 0x0E, (byte) 0xEC, (byte) 0xCC,
        (byte) 0xDD, (byte) 0xDC, (byte) 0x99, (byte) 0x9F, (byte) 0xBB, (byte) 0xB9, (byte) 0x33, (byte) 0x3E
    };

    // Header offsets
    public enum HeaderOffset {
        TITLE(0x134, 16),
        CGB_FLAG(0x143, 1),
        SGB_FLAG(0x146, 1),
        CARTRIDGE_TYPE(0x147, 1),
        ROM_SIZE(0x148, 1),
        RAM_SIZE(0x149, 1),
        DESTINATION_CODE(0x14A, 1),
        OLD_LICENSEE_CODE(0x14B, 1),
        MASK_ROM_VERSION(0x14C, 1),
        HEADER_CHECKSUM(0x14D, 1),
        GLOBAL_CHECKSUM(0x14E, 2);

        private final int offset;
        private final int length;

        HeaderOffset(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }
    }

    /**
     * Validates ROM Size against minimum requirements.
     *
     * @param size size of ROM data in bytes
     * @return true if size is valid for a ROM file
     */
    public static boolean isValidSize(int size) {
        return size >= HeaderOffset.GLOBAL_CHECKSUM.getOffset()
                + HeaderOffset.GLOBAL_CHECKSUM.getLength();
    }

    /**
     * Extracts a field from ROM data at specified offset.
     *
     * @param romData complete ROM data
     * @param field header offset enumeration specifying position and length
     * @return byte array containing the extracted field
     */
    public static byte[] extractField(byte[] romData, HeaderOffset field) {
        byte[] result = new byte[field.getLength()];
        System.arraycopy(romData, field.getOffset(), result, 0, field.getLength());
        return result;
    }
}
