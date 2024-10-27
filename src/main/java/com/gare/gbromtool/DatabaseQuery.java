package com.gare.gbromtool;

import java.sql.*;
import java.util.HexFormat;

/**
 * This class contains all the standard database queries for passing information
 * to RomReader.
 *
 * @author Thomas Robinson 23191795
 */
public class DatabaseQuery {

    private final DatabaseManager dbManager;
    private final Connection conn;

    public DatabaseQuery(DatabaseManager dbManager) throws SQLException {
        this.dbManager = dbManager;
        this.conn = dbManager.getConnection();
    }

    public String getCartridgeTypeDescription(String typeCode) throws SQLException {
        try {
            byte[] typeCodeBytes = HexFormat.of().parseHex(typeCode);
            String query = "SELECT type_description FROM CartridgeType WHERE type_code = ?";
            System.out.println("Looking up cartridge type: " + typeCode); // Debug

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setBytes(1, typeCodeBytes);
                ResultSet typeDescription = stmt.executeQuery();

                if (typeDescription.next()) {
                    String result = typeDescription.getString("type_description");
                    System.out.println("Found cartridge type: " + result); // Debug
                    return result;
                }
                System.out.println("No cartridge type found for code: " + typeCode); // Debug
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid hex code: " + typeCode); // Debug
        }
        return "Unknown cartridge type";
    }

    public String getROMSizeInKiB(int sizeCode) throws SQLException {
        ResultSet ROMSizeInKiB;
        String suffix = "KiB";
        StringBuilder sizes = new StringBuilder();
        String query = "SELECT size_kib, num_banks FROM ROMSize WHERE size_code = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, sizeCode);
            ROMSizeInKiB = stmt.executeQuery();

            while (ROMSizeInKiB.next()) {
                int size_kib = ROMSizeInKiB.getInt(1);
                int num_banks = ROMSizeInKiB.getInt(2);
                // Sometimes the size in bits is preferred for ROMs
                int size_bits = ROMSizeInKiB.getInt(1) / 8;

                if (size_kib >= 1024) {
                    size_kib = size_kib / 1024;
                    suffix = "MiB";
                }

                sizes.append(size_kib).append(" ").append(suffix).append(", ")
                        .append(size_bits).append(" ").append("Kb").append(", ")
                        .append(num_banks).append(" banks \n");
            }
        }
        return sizes.toString();
    }

    public String getRAMSizeInKiB(int sizeCode) throws SQLException {
        ResultSet RAMSizeInKiB;
        String suffix = " x 8 KiB banks";
        StringBuilder sizes = new StringBuilder();
        String query = "SELECT size_kib, num_banks FROM RAMSize WHERE size_code = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, sizeCode);
            RAMSizeInKiB = stmt.executeQuery();

            while (RAMSizeInKiB.next()) {
                int size_kib = RAMSizeInKiB.getInt(1);
                int num_banks = RAMSizeInKiB.getInt(2);
                // Sometimes the size in bits is preferred for ROMs
                int size_bits = RAMSizeInKiB.getInt(1) / 8;

                if (num_banks == 1) {
                    suffix = "x 8 KiB bank";
                }

                if (num_banks == 0) {
                    suffix = "No RAM";
                    sizes.append(suffix).append("\n");
                } else {
                    sizes.append(size_kib).append(" KiB").append(", ")
                            .append(size_bits).append(" ").append("Kb").append(", ")
                            .append(num_banks).append(" ").append(suffix).append("\n");
                }
            }
        }
        return sizes.toString();
    }

    public String getLicenseeCode(byte[] licenseeCode) throws SQLException {
        if (licenseeCode == null) {
            throw new IllegalArgumentException("Licensee Code cannot be null");
        }

        System.out.println("License Code length: " + licenseeCode.length); // Debug
        System.out.println("License Code hex: " + HexFormat.of().formatHex(licenseeCode).toUpperCase()); // Debug

        String query;
        PreparedStatement stmt;

        try {
            switch (licenseeCode.length) {
                case 1 -> {
                    query = "SELECT publisher FROM OldLicenseeCode WHERE licensee_code = ?";
                    stmt = conn.prepareStatement(query);
                    stmt.setBytes(1, licenseeCode);
                    System.out.println("Using Old Licensee Code"); // Debug
                }
                case 2 -> {
                    query = "SELECT publisher FROM NewLicenseeCode WHERE licensee_code = ?";
                    stmt = conn.prepareStatement(query);
                    stmt.setBytes(1, licenseeCode);
                    System.out.println("Using New Licensee Code"); // Debug
                }
                default ->
                    throw new IllegalArgumentException("Invalid Licensee Code length: " + licenseeCode.length);
            }

            try (stmt) {
                ResultSet publisher = stmt.executeQuery();
                if (publisher.next()) {
                    return publisher.getString("publisher");
                }
                System.out.println("No publisher found for code"); // Debug
                return "Unknown publisher";
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getLicenseeCode: " + e.getMessage()); // Debug
            throw e;
        }
    }

    /**
     * Saves ROM information to Collection table.
     *
     * @param name User-provided name for the ROM
     * @param title ROM title from header
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
     * @return true if save was successful, false otherwise
     * @throws SQLException if database operation fails
     */
    public boolean saveRomToCollection(
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
            byte[] globalChecksum) throws SQLException {

        String sql = """
                    INSERT INTO Collection (
                        title, name, type_code, rom_rev, rom_size_code,
                        ram_size_code, sgb_flag, cgb_flag, dest_code,
                        licensee_code, head_chksm, global_chksm
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setString(2, name);
            stmt.setBytes(3, typeCode);
            stmt.setBytes(4, romRev);
            stmt.setInt(5, romSizeCode);
            stmt.setInt(6, ramSizeCode);
            stmt.setBoolean(7, sgbFlag);
            stmt.setBytes(8, cgbFlag);
            stmt.setInt(9, destCode);
            stmt.setBytes(10, licenseeCode);
            stmt.setBytes(11, headerChecksum);
            stmt.setBytes(12, globalChecksum);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Checks if a ROM with the given title already exists in Collection.
     *
     * @param title The ROM title to check
     * @return true if the ROM exists, false otherwise
     * @throws SQLException if database operation fails
     */
    public boolean romExistsInCollection(String title) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Collection WHERE title = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

}
