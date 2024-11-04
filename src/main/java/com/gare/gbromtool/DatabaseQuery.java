package com.gare.gbromtool;

import java.sql.*;
import java.util.ArrayList;
import java.util.HexFormat;

/**
 * This class contains all the standard database queries for passing information
 * to RomReader.
 *
 * @author Thomas Robinson 23191795
 */
public class DatabaseQuery {

    private final Connection conn;

    public DatabaseQuery(DatabaseManager dbManager) throws SQLException {
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
     * @param rom the ROM file to save
     * @return true if save was successful, false otherwise
     * @throws SQLException if database operation fails
     */
    public boolean saveRomToCollection(Collection rom) throws SQLException {
        String sql = """
                    INSERT INTO Collection (
                        title, name, type_code, rom_rev, rom_size_code,
                        ram_size_code, sgb_flag, cgb_flag, dest_code,
                        licensee_code, head_chksm, global_chksm
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rom.getTitle());
            stmt.setString(2, rom.getName());
            stmt.setBytes(3, rom.getTypeCode());
            stmt.setBytes(4, rom.getRomRev());
            stmt.setInt(5, rom.getRomSizeCode());
            stmt.setInt(6, rom.getRamSizeCode());
            stmt.setBoolean(7, rom.getSgbFlag());
            stmt.setBytes(8, rom.getCgbFlag());
            stmt.setInt(9, rom.getDestCode());
            stmt.setBytes(10, rom.getLicenseeCode());
            stmt.setBytes(11, rom.getHeaderChecksum());
            stmt.setBytes(12, rom.getGlobalChecksum());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Checks if a ROM already exists in Collection based on title, revision,
     * and checksum.
     *
     * @param title The ROM title
     * @param romRev The ROM revision code
     * @param globalChecksum The global checksum
     * @return true if an identical ROM exists, false otherwise
     * @throws SQLException if database operation fails
     */
    public boolean romExistsInCollection(String title, byte[] romRev, byte[] globalChecksum)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM Collection "
                + "WHERE title = ? AND rom_rev = ? AND global_chksm = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setBytes(2, romRev);
            stmt.setBytes(3, globalChecksum);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * Updates an existing ROM entry in the Collection table.
     *
     * @param rom the ROM file to update
     * @throws java.sql.SQLException
     * @return true if save was successful, false otherwise
     */
    public boolean updateRomInCollection(Collection rom) throws SQLException {
        String sql = """
                    UPDATE Collection 
                    SET name = ?, type_code = ?, rom_size_code = ?, 
                        ram_size_code = ?, sgb_flag = ?, cgb_flag = ?, 
                        dest_code = ?, licensee_code = ?, head_chksm = ?
                    WHERE title = ? AND rom_rev = ? AND global_chksm = ?
                    """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rom.getName());
            stmt.setBytes(2, rom.getTypeCode());
            stmt.setInt(3, rom.getRomSizeCode());
            stmt.setInt(4, rom.getRamSizeCode());
            stmt.setBoolean(5, rom.getSgbFlag());
            stmt.setBytes(6, rom.getCgbFlag());
            stmt.setInt(7, rom.getDestCode());
            stmt.setBytes(8, rom.getLicenseeCode());
            stmt.setBytes(9, rom.getHeaderChecksum());
            stmt.setString(10, rom.getTitle());
            stmt.setBytes(11, rom.getRomRev());
            stmt.setBytes(12, rom.getGlobalChecksum());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Retrieves all ROMs from the Collection table in a format suitable for
     * display.
     *
     * @return List of ROMs in the collection
     * @throws SQLException if database operation fails
     */
    public ArrayList<Collection> getAllRoms() throws SQLException {
        ArrayList<Collection> roms = new ArrayList<>();
        String sql = """
                 SELECT * FROM Collection 
                 ORDER BY title, rom_rev, global_chksm
                 """;

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Collection rom = new Collection(
                        rs.getString("name"),
                        rs.getString("title"),
                        rs.getBytes("type_code"),
                        rs.getBytes("rom_rev"),
                        rs.getInt("rom_size_code"),
                        rs.getInt("ram_size_code"),
                        rs.getBoolean("sgb_flag"),
                        rs.getBytes("cgb_flag"),
                        rs.getInt("dest_code"),
                        rs.getBytes("licensee_code"),
                        rs.getBytes("head_chksm"),
                        rs.getBytes("global_chksm")
                );
                roms.add(rom);
            }
        }
        return roms;
    }

    /**
     * Debug method to print all ROMs in the Collection table with more details.
     *
     * @throws SQLException
     */
    public void printAllRoms() throws SQLException {
        String sql = """
                    SELECT name, title, rom_rev, global_chksm 
                    FROM Collection 
                    ORDER BY title, rom_rev, global_chksm
                    """;

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\nROMs in Collection:");
            System.out.println("------------------");
            while (rs.next()) {
                System.out.printf("Name: %-30s Title: %-20s Rev: %02X Global Checksum: %s%n",
                        rs.getString("name"),
                        rs.getString("title"),
                        rs.getBytes("rom_rev")[0],
                        bytesToHex(rs.getBytes("global_chksm")));
            }
            System.out.println("------------------\n");
        }
    }

    private String bytesToHex(byte[] bytes) {
        return bytes != null ? HexFormat.of().formatHex(bytes) : "null";
    }

}
