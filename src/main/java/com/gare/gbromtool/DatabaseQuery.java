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
}
