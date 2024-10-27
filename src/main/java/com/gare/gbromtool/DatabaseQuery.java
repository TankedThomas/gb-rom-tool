package com.gare.gbromtool;

import java.nio.charset.StandardCharsets;
import java.sql.*;

/**
 * This class contains all the standard database queries for passing information
 * to RomReader.
 *
 * @author Thomas Robinson 23191795
 */
public class DatabaseQuery {

    private final DatabaseManager dbManager;
    private final Connection conn;
    private RomReader romReader;

    public DatabaseQuery() throws SQLException {
        dbManager = new DatabaseManager();
        conn = dbManager.getConnection();
        try (Statement stmt = conn.createStatement()) {
        } catch (SQLException ex) {
            System.out.println("Error creating statement: " + ex.getMessage());
        }
    }

    public String getCartridgeTypeDescription(String typeCode) throws SQLException {
        typeCode = romReader.getCartridgeType();
        ResultSet typeDescription;
        String query = "SELECT type_description FROM CartridgeType WHERE HEX(type_code) = ?";

        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, typeCode);
        typeDescription = stmt.executeQuery();

        return typeDescription.toString();
    }

    public String getROMSizeInKiB(int sizeCode) throws SQLException {
        sizeCode = romReader.getROMSize();
        ResultSet ROMSizeInKiB;
        String suffix = "KiB";
        StringBuilder sizes = new StringBuilder();
        String query = "SELECT size_kib, num_banks FROM ROMSize WHERE size_code = ?";

        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, sizeCode);
        ROMSizeInKiB = stmt.executeQuery();

        while (ROMSizeInKiB.next()) {
            int size_kib = ROMSizeInKiB.getInt(1);
            int num_banks = ROMSizeInKiB.getInt(2);

            if (size_kib >= 1024) {
                size_kib = size_kib / 2;
                suffix = "MiB";
            }

            sizes.append(size_kib).append(" ").append(suffix).append(", ").append(num_banks).append(" banks \n");
        }
        String ROMSize = sizes.toString();

        return ROMSize;
    }

    public String getRAMSizeInKiB(int sizeCode) throws SQLException {
        sizeCode = romReader.getRAMSize();
        ResultSet RAMSizeInKiB;
        String suffix = " x 8 KiB banks";
        StringBuilder sizes = new StringBuilder();
        String query = "SELECT size_kib, num_banks FROM RAMSize WHERE size_code = ?";

        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, sizeCode);
        RAMSizeInKiB = stmt.executeQuery();

        while (RAMSizeInKiB.next()) {
            int size_kib = RAMSizeInKiB.getInt(1);
            int num_banks = RAMSizeInKiB.getInt(2);

            if (num_banks == 1) {
                suffix = " x 8 KiB bank";
            }

            if (num_banks == 0) {
                suffix = "No RAM";
                sizes.append(suffix).append("\n");
            } else {
                sizes.append(size_kib).append(" KiB").append(", ").append(num_banks).append(" ").append(suffix).append("\n");
            }
        }
        String RAMSize = sizes.toString();

        return RAMSize;
    }

    public String getLicenseeCode(byte[] licenseeCode) throws SQLException {
        licenseeCode = romReader.getLicenseeCode();
        ResultSet publisher;

        switch (licenseeCode.length) {
            case 1: {
                String hexValue = String.format("%02X", licenseeCode[0]);
                String query = "SELECT publisher FROM OldLicenseeCode WHERE HEX(licensee_code) = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, hexValue);
                publisher = stmt.executeQuery();
                break;
            }
            case 2: {
                String asciiValue = new String(licenseeCode, StandardCharsets.US_ASCII);
                String query = "SELECT publisher FROM NewLicenseeCode WHERE HEX(licensee_code) = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, asciiValue);
                publisher = stmt.executeQuery();
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid byte array length");
        }

        return publisher.toString();
    }

}
