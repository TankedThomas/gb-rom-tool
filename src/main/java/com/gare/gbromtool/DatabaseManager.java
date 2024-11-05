package com.gare.gbromtool;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;

/**
 * Manages the connection to and initialisation of the database.
 * Handles table creation, population of reference data, and database lifecycle.
 *
 * @author Thomas Robinson 23191795
 */
public class DatabaseManager {

    private static final String USER_NAME = "gbdb";
    private static final String PASSWORD = "eNgWdpvYusgyyARnVYokKRhukjDeihYebMfp3pXqDEeJH5p9zzNivJwZ7RpDfu4KRNyg5Wab";
    private static final String JDBC_URL = "jdbc:derby:GbRomDB;create=true;dataEncoding=UTF8";
    private Connection conn;

    private static DatabaseManager instance;

    /**
     * Gets or creates the singleton instance of DatabaseManager.
     * Creates a new connection if none exists or if current connection is
     * closed.
     *
     * @return the DatabaseManager instance
     * @throws SQLException if database connection fails
     */
    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null || instance.conn == null || instance.conn.isClosed()) {
            instance = new DatabaseManager();
        }
        if (instance.conn == null) {
            throw new SQLException("Failed to establish database connection");
        }
        return instance;
    }

    private DatabaseManager() throws SQLException {
        establishConnection();
        if (conn != null) {
            createTables();
        }
    }

    /**
     * Gets the current database connection.
     *
     * @return the active database Connection
     */
    public Connection getConnection() {
        return this.conn;
    }

    /**
     * Establishes connection to the embedded Derby database.
     * Creates a new database if one doesn't exist.
     *
     * @throws SQLException if connection fails
     */
    private void establishConnection() throws SQLException {
        if (this.conn == null) {
            try {
                Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
                conn = DriverManager.getConnection(JDBC_URL, USER_NAME, PASSWORD);
                System.out.println("Connected to the embedded database successfully.");
            } catch (ClassNotFoundException e) {
                String message = "Derby driver not found: " + e.getMessage();
                System.out.println(message);
                throw new SQLException(message, e);
            } catch (SQLException ex) {
                String message = "Error connecting to the database: " + ex.getMessage();
                System.out.println(message);
                throw ex;
            }
        }
    }

    /**
     * Closes all database connections.
     * Should be called when shutting down the application.
     */
    public void closeConnections() {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("Database connection closed.");
            } catch (SQLException ex) {
                System.out.println("Error closing database connection: " + ex.getMessage());
            }
        }
    }

    /**
     * Checks if a table already exists.
     *
     * @param tableName the name of the table to check
     * @return true if the table already exists, false otherwise
     * @throws SQLException if metadata query fails
     */
    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet resultSet = meta.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"});
        return resultSet.next();
    }

    public void initialiseDatabase() throws SQLException {
        createTables();
    }

    /**
     * Completely removes the database.
     * Attempts proper shutdown before deleting files.
     * Should only be used for testing or complete reset.
     */
    public static void dropDatabase() {
        if (instance != null) {
            instance.closeConnections();
            instance = null;
        }

        try {
            // Attempt to shut down the database properly
            try {
                DriverManager.getConnection("jdbc:derby:GbRomDB;shutdown=true");
            } catch (SQLException e) {
                // Shutdown always throws an exception, this is normal
                if (e.getSQLState().equals("08006")) {
                    System.out.println("Database shut down normally");
                }
            }

            // Delete database files
            File dbDirectory = new File("GbRomDB");
            if (dbDirectory.exists()) {
                deleteDirectory(dbDirectory);
            }
        } catch (Exception e) {
            System.out.println("Error during database cleanup: " + e.getMessage());
        }
    }

    /**
     * Recursively deletes a directory and its contents.
     *
     * @param directory the directory to delete
     */
    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                }
                file.delete();
            }
        }
        directory.delete();
    }

    /**
     * Cleans all tables in database by deleting all records.
     * This affects both reference tables and the Collection table.
     * Tables are emptied but not dropped.
     *
     * @throws SQLException if deletion operations fail
     */
    public void cleanDatabase() throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            String[] tables = {
                "Collection", "CartridgeType", "ROMSize",
                "RAMSize", "NewLicenseeCode", "OldLicenseeCode"
            };

            for (String table : tables) {
                if (tableExists(table)) {
                    stmt.execute("DELETE FROM " + table);
                }
            }

            // Commit the deletions
            conn.commit();
        }
    }

    /**
     * Cleans and recreates reference tables.
     * Drops and recreates CartridgeType, ROMSize, RAMSize, and Licensee Code
     * tables.
     * Does not affect the Collection table.
     *
     * @throws SQLException if table recreation fails
     */
    private void cleanReferenceTables() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            String[] referenceTables = {
                "CartridgeType", "ROMSize", "RAMSize",
                "NewLicenseeCode", "OldLicenseeCode"
            };

            for (String table : referenceTables) {
                if (tableExists(table)) {
                    stmt.execute("DROP TABLE " + table);
                }

                switch (table) {
                    case "CartridgeType" ->
                        stmt.executeUpdate("CREATE TABLE CartridgeType ("
                                + "type_code CHAR(2) FOR BIT DATA PRIMARY KEY,"
                                + "type_description VARCHAR(100)"
                                + ")");
                    case "ROMSize" ->
                        stmt.executeUpdate("CREATE TABLE ROMSize ("
                                + "size_code INT PRIMARY KEY,"
                                + "size_kib INT,"
                                + "num_banks INT"
                                + ")");
                    case "RAMSize" ->
                        stmt.executeUpdate("CREATE TABLE RAMSize ("
                                + "size_code INT PRIMARY KEY,"
                                + "size_kib INT,"
                                + "num_banks INT"
                                + ")");
                    case "NewLicenseeCode" ->
                        stmt.executeUpdate("CREATE TABLE NewLicenseeCode ("
                                + "licensee_code CHAR(2) FOR BIT DATA PRIMARY KEY,"
                                + "licensee_code_ascii VARCHAR(2),"
                                + "publisher VARCHAR(100)"
                                + ")");
                    case "OldLicenseeCode" ->
                        stmt.executeUpdate("CREATE TABLE OldLicenseeCode ("
                                + "licensee_code CHAR(2) FOR BIT DATA PRIMARY KEY,"
                                + "publisher VARCHAR(100)"
                                + ")");
                }

                conn.commit();
            }
        }
    }

    /**
     * Creates the database tables if they don't exist.
     * Reference tables are recreated and repopulated with each run.
     * Collection table is preserved between runs and only created if missing.
     *
     * @throws SQLException if table creation or population fails
     */
    private void createTables() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Clean and recreate reference tables
            cleanReferenceTables();

            // Create Collection table if it doesn't exist (blank by default)
            if (!tableExists("Collection")) {
                stmt.executeUpdate("CREATE TABLE Collection ("
                        + "title VARCHAR(100),"
                        + "name VARCHAR(200),"
                        + "mft_code VARCHAR(4),"
                        + "type_code CHAR(2) FOR BIT DATA,"
                        + "rom_rev CHAR(2) FOR BIT DATA,"
                        + "rom_size_code INT,"
                        + "ram_size_code INT,"
                        + "sgb_flag BOOLEAN,"
                        + "cgb_flag CHAR(2) FOR BIT DATA,"
                        + "dest_code INT,"
                        + "licensee_code CHAR(2) FOR BIT DATA,"
                        + "head_chksm CHAR(2) FOR BIT DATA,"
                        + "head_chksm_valid BOOLEAN,"
                        + "global_chksm CHAR(2) FOR BIT DATA,"
                        + "global_chksm_valid BOOLEAN,"
                        + "boot_logo_valid BOOLEAN,"
                        + "PRIMARY KEY (title, rom_rev, global_chksm)"
                        + ")");
                conn.commit();
            }

            // Populate tables with initial data
            populateCartridgeType();
            populateROMSize(stmt);
            populateRAMSize(stmt);
            populateNewLicenseeCode();
            populateOldLicenseeCode();
        }
    }

    /**
     * Helper method to insert Cartridge Type data into the database.
     * Converts hex string to binary data for storage.
     *
     * @param pstmt prepared statement for insertion
     * @param hex cartridge type code in hex format
     * @param description human-readable description of the cartridge type
     * @throws SQLException if insertion fails
     */
    private void insertCartridgeType(PreparedStatement pstmt, String hex, String description) throws SQLException {
        pstmt.setBytes(1, HexFormat.of().parseHex(hex));
        pstmt.setString(2, description);
        pstmt.executeUpdate();
    }

    /**
     * Populates the CartridgeType table with all known Cartridge Types.
     * Each type includes a binary type code and its description.
     *
     * @throws SQLException if population fails
     */
    private void populateCartridgeType() throws SQLException {
        String insertQuery = "INSERT INTO CartridgeType (type_code, type_description) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            insertCartridgeType(pstmt, "00", "ROM ONLY");
            insertCartridgeType(pstmt, "01", "MBC1");
            insertCartridgeType(pstmt, "02", "MBC1+RAM");
            insertCartridgeType(pstmt, "03", "MBC1+RAM+BATTERY");
            insertCartridgeType(pstmt, "05", "MBC2");
            insertCartridgeType(pstmt, "06", "MBC2+BATTERY");
            insertCartridgeType(pstmt, "08", "ROM+RAM");
            insertCartridgeType(pstmt, "09", "ROM+RAM+BATTERY");
            insertCartridgeType(pstmt, "0B", "MMM01");
            insertCartridgeType(pstmt, "0C", "MMM01+RAM");
            insertCartridgeType(pstmt, "0D", "MMM01+RAM+BATTERY");
            insertCartridgeType(pstmt, "0F", "MBC3+TIMER+BATTERY");
            insertCartridgeType(pstmt, "10", "MBC3+TIMER+RAM+BATTERY");
            insertCartridgeType(pstmt, "11", "MBC3");
            insertCartridgeType(pstmt, "12", "MBC3+RAM");
            insertCartridgeType(pstmt, "13", "MBC3+RAM+BATTERY");
            insertCartridgeType(pstmt, "19", "MBC5");
            insertCartridgeType(pstmt, "1A", "MBC5+RAM");
            insertCartridgeType(pstmt, "1B", "MBC5+RAM+BATTERY");
            insertCartridgeType(pstmt, "1C", "MBC5+RUMBLE");
            insertCartridgeType(pstmt, "1D", "MBC5+RUMBLE+RAM");
            insertCartridgeType(pstmt, "1E", "MBC5+RUMBLE+RAM+BATTERY");
            insertCartridgeType(pstmt, "20", "MBC6");
            insertCartridgeType(pstmt, "22", "MBC7+SENSOR+RUMBLE+RAM+BATTERY");
            insertCartridgeType(pstmt, "FC", "POCKET CAMERA");
            insertCartridgeType(pstmt, "FD", "BANDAI TAMA5");
            insertCartridgeType(pstmt, "FE", "HuC3");
            insertCartridgeType(pstmt, "FF", "HuC1+RAM+BATTERY");
        }
    }

    /**
     * Populates the ROMSize table with standard ROM sizes.
     * Each entry includes size code, size in KiB, and number of banks.
     * Sizes range from 32 KiB (2 banks) to 8 MiB (512 banks).
     *
     * @param stmt statement for executing SQL
     * @throws SQLException if population fails
     */
    private void populateROMSize(Statement stmt) throws SQLException {
        stmt.executeUpdate("INSERT INTO ROMSize VALUES (0, 32, 2)");     // 32 KiB,  2 banks
        stmt.executeUpdate("INSERT INTO ROMSize VALUES (1, 64, 4)");     // 64 KiB,  4 banks
        stmt.executeUpdate("INSERT INTO ROMSize VALUES (2, 128, 8)");    // 128 KiB, 8 banks
        stmt.executeUpdate("INSERT INTO ROMSize VALUES (3, 256, 16)");   // 256 KiB, 16 banks
        stmt.executeUpdate("INSERT INTO ROMSize VALUES (4, 512, 32)");   // 512 KiB, 32 banks
        stmt.executeUpdate("INSERT INTO ROMSize VALUES (5, 1024, 64)");  // 1 MiB,   64 banks
        stmt.executeUpdate("INSERT INTO ROMSize VALUES (6, 2048, 128)"); // 2 MiB,   128 banks
        stmt.executeUpdate("INSERT INTO ROMSize VALUES (7, 4096, 256)"); // 4 MiB,   256 banks
        stmt.executeUpdate("INSERT INTO ROMSize VALUES (8, 8192, 512)"); // 8 MiB,   512 banks
    }

    /**
     * Populates the RAMSize table with standard RAM sizes.
     * Each entry includes size code, size in KiB, and number of banks.
     * Sizes range from no RAM to 128 KiB (16 banks of 8 KiB).
     *
     * @param stmt statement for executing SQL
     * @throws SQLException if population fails
     */
    private void populateRAMSize(Statement stmt) throws SQLException {
        stmt.executeUpdate("INSERT INTO RAMSize VALUES (0, 0, 0)");      // No RAM
        stmt.executeUpdate("INSERT INTO RAMSize VALUES (1, 2, 1)");      // 2 KiB,   unused (1 bank presumed)
        stmt.executeUpdate("INSERT INTO RAMSize VALUES (2, 8, 1)");      // 8 KiB,   1 x 8 KiB bank
        stmt.executeUpdate("INSERT INTO RAMSize VALUES (3, 32, 4)");     // 32 KiB,  4 x 8 KiB banks
        stmt.executeUpdate("INSERT INTO RAMSize VALUES (4, 128, 16)");   // 128 KiB, 16 x 8 KiB banks
        stmt.executeUpdate("INSERT INTO RAMSize VALUES (5, 64, 8)");     // 64 KiB,  8 x 8 KiB banks
    }

    /**
     * Helper method to insert New Licensee Code as binary.
     * Converts hex string to binary data for storage.
     *
     * @param pstmt prepared statement for insertion
     * @param ascii ASCII representation of the Licensee Code
     * @param publisher the publisher name
     * @throws SQLException if insertion fails
     */
    private void insertNewLicenseeCode(PreparedStatement pstmt, String ascii, String publisher) throws SQLException {
        pstmt.setBytes(1, ascii.getBytes(StandardCharsets.US_ASCII));
        pstmt.setString(2, ascii);
        pstmt.setString(3, publisher);
        pstmt.executeUpdate();
    }

    /**
     * Populates the NewLicenseeCode table with publisher information.
     * Used for ROMs with 2-byte Licensee Codes.
     * Each entry includes Licensee Code in hex (via helper method) and ASCII,
     * and publisher name.
     *
     * @throws SQLException if population fails
     */
    private void populateNewLicenseeCode() throws SQLException {
        String insertQuery = "INSERT INTO NewLicenseeCode (licensee_code, licensee_code_ascii, publisher) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            insertNewLicenseeCode(pstmt, "00", "None");
            insertNewLicenseeCode(pstmt, "01", "Nintendo");
            insertNewLicenseeCode(pstmt, "02", "Rocket Games");
            insertNewLicenseeCode(pstmt, "08", "Capcom");
            insertNewLicenseeCode(pstmt, "09", "Hot B Co.");
            insertNewLicenseeCode(pstmt, "0A", "Jaleco");
            insertNewLicenseeCode(pstmt, "0B", "Coconuts Japan");
            insertNewLicenseeCode(pstmt, "0C", "Coconuts Japan/G.X.Media");
            insertNewLicenseeCode(pstmt, "0H", "Starfish");
            insertNewLicenseeCode(pstmt, "0L", "Warashi Inc.");
            insertNewLicenseeCode(pstmt, "0N", "Nowpro");
            insertNewLicenseeCode(pstmt, "0P", "Game Village");
            insertNewLicenseeCode(pstmt, "0Q", "IE Institute");
            insertNewLicenseeCode(pstmt, "13", "Electronic Arts Japan");
            insertNewLicenseeCode(pstmt, "18", "Hudson Soft Japan");
            insertNewLicenseeCode(pstmt, "19", "B-AI");
            insertNewLicenseeCode(pstmt, "1A", "Yonoman");
            insertNewLicenseeCode(pstmt, "1H", "Yojigen");
            insertNewLicenseeCode(pstmt, "1M", "Microcabin Corporation");
            insertNewLicenseeCode(pstmt, "1N", "Dazz");
            insertNewLicenseeCode(pstmt, "1P", "Creatures Inc.");
            insertNewLicenseeCode(pstmt, "1Q", "TDK Deep Impresion");
            insertNewLicenseeCode(pstmt, "20", "KSS");
            insertNewLicenseeCode(pstmt, "22", "Planning Office WADA");
            insertNewLicenseeCode(pstmt, "28", "Kemco Japan");
            insertNewLicenseeCode(pstmt, "2D", "Visit");
            insertNewLicenseeCode(pstmt, "2H", "Ubisoft Japan");
            insertNewLicenseeCode(pstmt, "2K", "NEC InterChannel");
            insertNewLicenseeCode(pstmt, "2L", "Tam");
            insertNewLicenseeCode(pstmt, "2M", "Jordan");
            insertNewLicenseeCode(pstmt, "2N", "Smilesoft");
            insertNewLicenseeCode(pstmt, "2P", "The Pokémon Company");
            insertNewLicenseeCode(pstmt, "30", "Viacom New Media");
            insertNewLicenseeCode(pstmt, "34", "Magifact");
            insertNewLicenseeCode(pstmt, "35", "Hect");
            insertNewLicenseeCode(pstmt, "36", "Codemasters");
            insertNewLicenseeCode(pstmt, "37", "GAGA Communications");
            insertNewLicenseeCode(pstmt, "38", "Laguna");
            insertNewLicenseeCode(pstmt, "39", "Telstar Fun and Games");
            insertNewLicenseeCode(pstmt, "3:", "Nintendo/Mani");
            insertNewLicenseeCode(pstmt, "3E", "Gremlin Graphics");
            insertNewLicenseeCode(pstmt, "41", "Ubi Soft Entertainment");
            insertNewLicenseeCode(pstmt, "42", "Sunsoft");
            insertNewLicenseeCode(pstmt, "47", "Spectrum Holobyte");
            insertNewLicenseeCode(pstmt, "4D", "Malibu Games");
            insertNewLicenseeCode(pstmt, "4F", "Eidos/U.S. Gold");
            insertNewLicenseeCode(pstmt, "4G", "Playmates Inc.");
            insertNewLicenseeCode(pstmt, "4J", "Fox Interactive");
            insertNewLicenseeCode(pstmt, "4K", "Time Warner Interactive");
            insertNewLicenseeCode(pstmt, "4S", "Black Pearl");
            insertNewLicenseeCode(pstmt, "4X", "GT Interactive");
            insertNewLicenseeCode(pstmt, "4Y", "Rare");
            insertNewLicenseeCode(pstmt, "4Z", "Crave Entertainment");
            insertNewLicenseeCode(pstmt, "50", "Absolute Entertainment");
            insertNewLicenseeCode(pstmt, "51", "Acclaim");
            insertNewLicenseeCode(pstmt, "52", "Activision");
            insertNewLicenseeCode(pstmt, "54", "Take 2 Interactive");
            insertNewLicenseeCode(pstmt, "55", "Hi Tech Expressions");
            insertNewLicenseeCode(pstmt, "56", "LJN");
            insertNewLicenseeCode(pstmt, "58", "Mattel");
            insertNewLicenseeCode(pstmt, "5A", "Mindscape/Red Orb Ent.");
            insertNewLicenseeCode(pstmt, "5D", "Midway");
            insertNewLicenseeCode(pstmt, "5F", "American Softworks");
            insertNewLicenseeCode(pstmt, "5G", "Majesco Sales Inc");
            insertNewLicenseeCode(pstmt, "5H", "3DO");
            insertNewLicenseeCode(pstmt, "5K", "Hasbro");
            insertNewLicenseeCode(pstmt, "5L", "NewKidCo");
            insertNewLicenseeCode(pstmt, "5M", "Telegames");
            insertNewLicenseeCode(pstmt, "5N", "Metro3D");
            insertNewLicenseeCode(pstmt, "5P", "Vatical Entertainment");
            insertNewLicenseeCode(pstmt, "5Q", "LEGO Media");
            insertNewLicenseeCode(pstmt, "5T", "Cryo Interactive");
            insertNewLicenseeCode(pstmt, "5V", "Agetec Inc.");
            insertNewLicenseeCode(pstmt, "5W", "Red Storm Ent./BKN Ent.");
            insertNewLicenseeCode(pstmt, "5X", "Microids");
            insertNewLicenseeCode(pstmt, "5Z", "Conspiracy Entertainment Corp.");
            insertNewLicenseeCode(pstmt, "60", "Titus Interactive Studios");
            insertNewLicenseeCode(pstmt, "61", "Virgin Interactive");
            insertNewLicenseeCode(pstmt, "64", "LucasArts Entertainment");
            insertNewLicenseeCode(pstmt, "67", "Ocean");
            insertNewLicenseeCode(pstmt, "69", "Electronic Arts");
            insertNewLicenseeCode(pstmt, "6F", "Electro Brain");
            insertNewLicenseeCode(pstmt, "6G", "The Learning Company");
            insertNewLicenseeCode(pstmt, "6H", "BBC");
            insertNewLicenseeCode(pstmt, "6J", "Software 2000");
            insertNewLicenseeCode(pstmt, "6L", "BAM! Entertainment");
            insertNewLicenseeCode(pstmt, "6M", "Studio 3");
            insertNewLicenseeCode(pstmt, "6P", "Ravensburger Interactive Media GmbH");
            insertNewLicenseeCode(pstmt, "6Q", "Classified Games");
            insertNewLicenseeCode(pstmt, "6R", "Sound Source Interactive");
            insertNewLicenseeCode(pstmt, "6S", "TDK Mediactive");
            insertNewLicenseeCode(pstmt, "6T", "Interactive Imagination");
            insertNewLicenseeCode(pstmt, "6U", "DreamCatcher");
            insertNewLicenseeCode(pstmt, "6V", "JoWood Productions");
            insertNewLicenseeCode(pstmt, "6X", "Wannado Edition");
            insertNewLicenseeCode(pstmt, "6Y", "LSP");
            insertNewLicenseeCode(pstmt, "6Z", "ITE Media");
            insertNewLicenseeCode(pstmt, "70", "Infogrames");
            insertNewLicenseeCode(pstmt, "71", "Interplay");
            insertNewLicenseeCode(pstmt, "72", "JVC Musical Industries Inc");
            insertNewLicenseeCode(pstmt, "75", "SCI");
            insertNewLicenseeCode(pstmt, "78", "THQ");
            insertNewLicenseeCode(pstmt, "79", "Accolade");
            insertNewLicenseeCode(pstmt, "7D", "Universal Interactive Studios");
            insertNewLicenseeCode(pstmt, "7F", "Kemco");
            insertNewLicenseeCode(pstmt, "7G", "Rage Software");
            insertNewLicenseeCode(pstmt, "7H", "Encore");
            insertNewLicenseeCode(pstmt, "7K", "BVM");
            insertNewLicenseeCode(pstmt, "7L", "Simon & Schuster Interactive");
            insertNewLicenseeCode(pstmt, "87", "Tsukuda Original");
            insertNewLicenseeCode(pstmt, "8B", "Bulletproof Software");
            insertNewLicenseeCode(pstmt, "8C", "Vic Tokai Inc.");
            insertNewLicenseeCode(pstmt, "8F", "I\"Max");
            insertNewLicenseeCode(pstmt, "8J", "General Entertainment");
            insertNewLicenseeCode(pstmt, "8K", "Japan System Supply");
            insertNewLicenseeCode(pstmt, "8M", "CyberFront");
            insertNewLicenseeCode(pstmt, "8N", "Success");
            insertNewLicenseeCode(pstmt, "8P", "SEGA Japan");
            insertNewLicenseeCode(pstmt, "91", "Chun Soft");
            insertNewLicenseeCode(pstmt, "92", "Video System");
            insertNewLicenseeCode(pstmt, "93", "BEC");
            insertNewLicenseeCode(pstmt, "99", "Victor Interactive Software");
            insertNewLicenseeCode(pstmt, "9A", "Nichibutsu/Nihon Bussan");
            insertNewLicenseeCode(pstmt, "9B", "Tecmo");
            insertNewLicenseeCode(pstmt, "9C", "Imagineer");
            insertNewLicenseeCode(pstmt, "9H", "Bottom Up");
            insertNewLicenseeCode(pstmt, "9K", "Syscom");
            insertNewLicenseeCode(pstmt, "9L", "Hasbro Japan");
            insertNewLicenseeCode(pstmt, "9M", "Jaguar");
            insertNewLicenseeCode(pstmt, "9N", "Marvelous Entertainment");
            insertNewLicenseeCode(pstmt, "A0", "Telenet");
            insertNewLicenseeCode(pstmt, "A1", "Hori");
            insertNewLicenseeCode(pstmt, "A4", "Konami");
            insertNewLicenseeCode(pstmt, "A7", "Takara");
            insertNewLicenseeCode(pstmt, "A9", "Technos Japan Corp.");
            insertNewLicenseeCode(pstmt, "AD", "Toho");
            insertNewLicenseeCode(pstmt, "AF", "Namco");
            insertNewLicenseeCode(pstmt, "AH", "J-Wing");
            insertNewLicenseeCode(pstmt, "AK", "KID");
            insertNewLicenseeCode(pstmt, "AL", "MediaFactory");
            insertNewLicenseeCode(pstmt, "AM", "BIOX Co., Ltd./GAPS Inc.");
            insertNewLicenseeCode(pstmt, "AN", "Lay-Up");
            insertNewLicenseeCode(pstmt, "AP", "Infogrames Hudson");
            insertNewLicenseeCode(pstmt, "AQ", "Kiratto. Ludic Inc");
            insertNewLicenseeCode(pstmt, "B0", "Acclaim Japan");
            insertNewLicenseeCode(pstmt, "B1", "ASCII");
            insertNewLicenseeCode(pstmt, "B2", "Bandai");
            insertNewLicenseeCode(pstmt, "B4", "Enix");
            insertNewLicenseeCode(pstmt, "BA", "Culture Brain");
            insertNewLicenseeCode(pstmt, "BB", "Sunsoft");
            insertNewLicenseeCode(pstmt, "BF", "Sammy");
            insertNewLicenseeCode(pstmt, "BG", "Magical");
            insertNewLicenseeCode(pstmt, "BJ", "Compile");
            insertNewLicenseeCode(pstmt, "BL", "MTO");
            insertNewLicenseeCode(pstmt, "BM", "XING Entertainment");
            insertNewLicenseeCode(pstmt, "BN", "Sunrise Interactive");
            insertNewLicenseeCode(pstmt, "BP", "Global A Entertainment");
            insertNewLicenseeCode(pstmt, "C0", "Taito");
            insertNewLicenseeCode(pstmt, "C6", "Tonkin House");
            insertNewLicenseeCode(pstmt, "C8", "Koei");
            insertNewLicenseeCode(pstmt, "CB", "Vapinc/NTVIC");
            insertNewLicenseeCode(pstmt, "CE", "FCI/Pony Canyon");
            insertNewLicenseeCode(pstmt, "CF", "Angel");
            insertNewLicenseeCode(pstmt, "CJ", "BOSS Communication");
            insertNewLicenseeCode(pstmt, "CK", "Axela");
            insertNewLicenseeCode(pstmt, "CN", "NEC Interchannel");
            insertNewLicenseeCode(pstmt, "CP", "Enterbrain");
            insertNewLicenseeCode(pstmt, "D4", "Ask Kodansa");
            insertNewLicenseeCode(pstmt, "D6", "Naxat");
            insertNewLicenseeCode(pstmt, "D9", "Banpresto");
            insertNewLicenseeCode(pstmt, "DA", "TOMY");
            insertNewLicenseeCode(pstmt, "DD", "NCS");
            insertNewLicenseeCode(pstmt, "DE", "Human Entertainment");
            insertNewLicenseeCode(pstmt, "DF", "Altron Corporation");
            insertNewLicenseeCode(pstmt, "DH", "Gaps Inc.");
            insertNewLicenseeCode(pstmt, "DJ", "Epoch Co., Ltd.");
            insertNewLicenseeCode(pstmt, "DK", "Kodansha");
            insertNewLicenseeCode(pstmt, "DL", "Digital Kids");
            insertNewLicenseeCode(pstmt, "DN", "ELF");
            insertNewLicenseeCode(pstmt, "DP", "Prime System");
            insertNewLicenseeCode(pstmt, "E2", "Yutaka");
            insertNewLicenseeCode(pstmt, "E5", "Epoch");
            insertNewLicenseeCode(pstmt, "E7", "Athena");
            insertNewLicenseeCode(pstmt, "E8", "Asmik Ace Entertainment Inc.");
            insertNewLicenseeCode(pstmt, "E9", "Natsume");
            insertNewLicenseeCode(pstmt, "EA", "King Records");
            insertNewLicenseeCode(pstmt, "EB", "Atlus");
            insertNewLicenseeCode(pstmt, "EJ", "See old licensee code");
            insertNewLicenseeCode(pstmt, "EL", "Spike");
            insertNewLicenseeCode(pstmt, "EN", "Alphadream Corporation");
            insertNewLicenseeCode(pstmt, "EP", "Sting Entertainment");
            insertNewLicenseeCode(pstmt, "EQ", "Omega Project Co., Ltd.");
            insertNewLicenseeCode(pstmt, "FB", "Psygnosis");
            insertNewLicenseeCode(pstmt, "FG", "Jupiter Corporation");
            insertNewLicenseeCode(pstmt, "RX", "Li Cheng");
            insertNewLicenseeCode(pstmt, "S5", "SouthPeak Interactive");
            insertNewLicenseeCode(pstmt, "XX", "Rocket Games");
        }
    }

    /**
     * Helper method to insert Old Licensee Code data into the database.
     * Converts hex string to binary data for storage.
     *
     * @param pstmt prepared statement for insertion
     * @param hex licensee code in hex format
     * @param publisher the publisher name
     * @throws SQLException if insertion fails
     */
    private void insertOldLicenseeCode(PreparedStatement pstmt, String hex, String publisher) throws SQLException {
        pstmt.setBytes(1, HexFormat.of().parseHex(hex));
        pstmt.setString(2, publisher);
        pstmt.executeUpdate();
    }

    /**
     * Populates the OldLicenseeCode table with publisher information.
     * Used for ROMs with 1-byte licensee codes.
     * Each entry includes Licensee Code in hex and publisher name.
     *
     * @throws SQLException if population fails
     */
    private void populateOldLicenseeCode() throws SQLException {
        // Add Old Licensee Codes
        String insertQuery = "INSERT INTO OldLicenseeCode (licensee_code, publisher) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            insertOldLicenseeCode(pstmt, "00", "None");
            insertOldLicenseeCode(pstmt, "01", "Nintendo");
            insertOldLicenseeCode(pstmt, "08", "Capcom");
            insertOldLicenseeCode(pstmt, "09", "HOT-B");
            insertOldLicenseeCode(pstmt, "0A", "Jaleco");
            insertOldLicenseeCode(pstmt, "0B", "Coconuts Japan");
            insertOldLicenseeCode(pstmt, "0C", "Elite Systems");
            insertOldLicenseeCode(pstmt, "13", "EA (Electronic Arts)");
            insertOldLicenseeCode(pstmt, "18", "Hudson Soft");
            insertOldLicenseeCode(pstmt, "19", "B-AI");
            insertOldLicenseeCode(pstmt, "1A", "Yanoman");
            insertOldLicenseeCode(pstmt, "1D", "Japan Clary");
            insertOldLicenseeCode(pstmt, "1F", "Virgin Games Ltd.");
            insertOldLicenseeCode(pstmt, "24", "PCM Complete");
            insertOldLicenseeCode(pstmt, "25", "San-X");
            insertOldLicenseeCode(pstmt, "28", "Kemco");
            insertOldLicenseeCode(pstmt, "29", "SETA Corporation");
            insertOldLicenseeCode(pstmt, "30", "Infogrames");
            insertOldLicenseeCode(pstmt, "31", "Nintendo");
            insertOldLicenseeCode(pstmt, "32", "Bandai");
            insertOldLicenseeCode(pstmt, "33", "See new licensee code");
            insertOldLicenseeCode(pstmt, "34", "Konami");
            insertOldLicenseeCode(pstmt, "35", "HectorSoft");
            insertOldLicenseeCode(pstmt, "38", "Capcom");
            insertOldLicenseeCode(pstmt, "39", "Banpresto");
            insertOldLicenseeCode(pstmt, "3C", "Entertainment Interactive (stub)");
            insertOldLicenseeCode(pstmt, "3E", "Gremlin Graphics");
            insertOldLicenseeCode(pstmt, "41", "Ubi Soft");
            insertOldLicenseeCode(pstmt, "42", "Atlus");
            insertOldLicenseeCode(pstmt, "44", "Malibu Interactive");
            insertOldLicenseeCode(pstmt, "46", "Angel");
            insertOldLicenseeCode(pstmt, "47", "Spectrum HoloByte");
            insertOldLicenseeCode(pstmt, "49", "Irem");
            insertOldLicenseeCode(pstmt, "4A", "Virgin Games Ltd.");
            insertOldLicenseeCode(pstmt, "4D", "Malibu Interactive");
            insertOldLicenseeCode(pstmt, "4F", "U.S. Gold");
            insertOldLicenseeCode(pstmt, "50", "Absolute");
            insertOldLicenseeCode(pstmt, "51", "Acclaim Entertainment");
            insertOldLicenseeCode(pstmt, "52", "Activision");
            insertOldLicenseeCode(pstmt, "53", "Sammy USA Corporation");
            insertOldLicenseeCode(pstmt, "54", "GameTek");
            insertOldLicenseeCode(pstmt, "55", "Park Place");
            insertOldLicenseeCode(pstmt, "56", "LJN");
            insertOldLicenseeCode(pstmt, "57", "Matchbox");
            insertOldLicenseeCode(pstmt, "59", "Milton Bradley Company");
            insertOldLicenseeCode(pstmt, "5A", "Mindscape");
            insertOldLicenseeCode(pstmt, "5B", "Romstar");
            insertOldLicenseeCode(pstmt, "5C", "TAXAN");
            insertOldLicenseeCode(pstmt, "5D", "Tradewest");
            insertOldLicenseeCode(pstmt, "60", "Titus Interactive");
            insertOldLicenseeCode(pstmt, "61", "Virgin Games, Inc.");
            insertOldLicenseeCode(pstmt, "67", "Ocean Software");
            insertOldLicenseeCode(pstmt, "69", "EA (Electronic Arts)");
            insertOldLicenseeCode(pstmt, "6E", "Elite Systems");
            insertOldLicenseeCode(pstmt, "6F", "Electro Brain");
            insertOldLicenseeCode(pstmt, "70", "Infogrames");
            insertOldLicenseeCode(pstmt, "71", "Interplay Entertainment");
            insertOldLicenseeCode(pstmt, "72", "JVC Musical Industries, Inc.");
            insertOldLicenseeCode(pstmt, "73", "Parker Brothers");
            insertOldLicenseeCode(pstmt, "75", "The Sales Curve Limited");
            insertOldLicenseeCode(pstmt, "78", "THQ");
            insertOldLicenseeCode(pstmt, "79", "Accolade");
            insertOldLicenseeCode(pstmt, "7A", "Triffix Entertainment");
            insertOldLicenseeCode(pstmt, "7C", "MicroProse");
            insertOldLicenseeCode(pstmt, "7F", "Kemco");
            insertOldLicenseeCode(pstmt, "80", "Misawa Entertainment");
            insertOldLicenseeCode(pstmt, "83", "LOZC G.");
            insertOldLicenseeCode(pstmt, "86", "Tokuma Shoten");
            insertOldLicenseeCode(pstmt, "8B", "Bullet-Proof Software");
            insertOldLicenseeCode(pstmt, "8C", "Vic Tokai Corp.");
            insertOldLicenseeCode(pstmt, "8E", "Ape Inc.");
            insertOldLicenseeCode(pstmt, "8F", "I'Max");
            insertOldLicenseeCode(pstmt, "91", "Chunsoft Co.");
            insertOldLicenseeCode(pstmt, "92", "Video System");
            insertOldLicenseeCode(pstmt, "93", "Tsubaraya Productions");
            insertOldLicenseeCode(pstmt, "95", "Varie");
            insertOldLicenseeCode(pstmt, "96", "Yonezawa PR21");
            insertOldLicenseeCode(pstmt, "97", "Kemco");
            insertOldLicenseeCode(pstmt, "99", "Arc");
            insertOldLicenseeCode(pstmt, "9A", "Nihon Bussan");
            insertOldLicenseeCode(pstmt, "9B", "Tecmo");
            insertOldLicenseeCode(pstmt, "9C", "Imagineer");
            insertOldLicenseeCode(pstmt, "9D", "Banpresto");
            insertOldLicenseeCode(pstmt, "9F", "Nova Games");
            insertOldLicenseeCode(pstmt, "A1", "Hori Electric");
            insertOldLicenseeCode(pstmt, "A2", "Bandai");
            insertOldLicenseeCode(pstmt, "A4", "Konami");
            insertOldLicenseeCode(pstmt, "A6", "Kawada ");
            insertOldLicenseeCode(pstmt, "A7", "Takara");
            insertOldLicenseeCode(pstmt, "A9", "Technos Japan");
            insertOldLicenseeCode(pstmt, "AA", "Victor Musical Industries, Inc.");
            insertOldLicenseeCode(pstmt, "AC", "Toei Animation");
            insertOldLicenseeCode(pstmt, "AD", "Toho");
            insertOldLicenseeCode(pstmt, "AF", "Namco");
            insertOldLicenseeCode(pstmt, "B0", "Acclaim Entertainment");
            insertOldLicenseeCode(pstmt, "B1", "ASCII Corporation");
            insertOldLicenseeCode(pstmt, "B2", "Bandai");
            insertOldLicenseeCode(pstmt, "B4", "Square Enix");
            insertOldLicenseeCode(pstmt, "B6", "HAL Laboratory");
            insertOldLicenseeCode(pstmt, "B7", "SNK");
            insertOldLicenseeCode(pstmt, "B9", "Pony Canyon");
            insertOldLicenseeCode(pstmt, "BA", "Culture Brain");
            insertOldLicenseeCode(pstmt, "BB", "Sunsoft");
            insertOldLicenseeCode(pstmt, "BD", "Sony Imagesoft");
            insertOldLicenseeCode(pstmt, "BF", "Sammy Corporation");
            insertOldLicenseeCode(pstmt, "C0", "Taito");
            insertOldLicenseeCode(pstmt, "C2", "Kemco");
            insertOldLicenseeCode(pstmt, "C3", "Square");
            insertOldLicenseeCode(pstmt, "C4", "Tokuma Shoten");
            insertOldLicenseeCode(pstmt, "C5", "Data East");
            insertOldLicenseeCode(pstmt, "C6", "Tonkin House");
            insertOldLicenseeCode(pstmt, "C8", "Koei");
            insertOldLicenseeCode(pstmt, "C9", "UFL");
            insertOldLicenseeCode(pstmt, "CA", "Ultra Games");
            insertOldLicenseeCode(pstmt, "CB", "VAP, Inc.");
            insertOldLicenseeCode(pstmt, "CC", "Use Corporation");
            insertOldLicenseeCode(pstmt, "CD", "Meldac");
            insertOldLicenseeCode(pstmt, "CE", "Pony Canyon");
            insertOldLicenseeCode(pstmt, "CF", "Angel");
            insertOldLicenseeCode(pstmt, "D0", "Taito");
            insertOldLicenseeCode(pstmt, "D1", "SOFEL (Software Engineering Lab)");
            insertOldLicenseeCode(pstmt, "D2", "Quest");
            insertOldLicenseeCode(pstmt, "D3", "Sigma Enterprises");
            insertOldLicenseeCode(pstmt, "D4", "ASK Kodansha Co.");
            insertOldLicenseeCode(pstmt, "D6", "Naxat Soft");
            insertOldLicenseeCode(pstmt, "D7", "Copya System");
            insertOldLicenseeCode(pstmt, "D9", "Banpresto");
            insertOldLicenseeCode(pstmt, "DA", "Tomy");
            insertOldLicenseeCode(pstmt, "DB", "LJN Japan");
            insertOldLicenseeCode(pstmt, "DD", "Nippon Computer Systems");
            insertOldLicenseeCode(pstmt, "DE", "Human Entertainment");
            insertOldLicenseeCode(pstmt, "DF", "Altron");
            insertOldLicenseeCode(pstmt, "E0", "Jaleco");
            insertOldLicenseeCode(pstmt, "E1", "Towa Chiki");
            insertOldLicenseeCode(pstmt, "E2", "Yutaka");
            insertOldLicenseeCode(pstmt, "E3", "Varie");
            insertOldLicenseeCode(pstmt, "E5", "Epoch");
            insertOldLicenseeCode(pstmt, "E7", "Athena");
            insertOldLicenseeCode(pstmt, "E8", "Asmik Ace Entertainment");
            insertOldLicenseeCode(pstmt, "E9", "Natsume");
            insertOldLicenseeCode(pstmt, "EA", "King Records");
            insertOldLicenseeCode(pstmt, "EB", "Atlus");
            insertOldLicenseeCode(pstmt, "EC", "Epic/Sony Records");
            insertOldLicenseeCode(pstmt, "EE", "IGS");
            insertOldLicenseeCode(pstmt, "F0", "A Wave");
            insertOldLicenseeCode(pstmt, "F3", "Extreme Entertainment");
            insertOldLicenseeCode(pstmt, "FF", "LJN");
        }
    }

}
