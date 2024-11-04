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
 * This class manages the connection to and initialisation of the database.
 *
 * @author Thomas Robinson 23191795
 */
public class DatabaseManager {

    private static final String USER_NAME = "gbdb";
    private static final String PASSWORD = "eNgWdpvYusgyyARnVYokKRhukjDeihYebMfp3pXqDEeJH5p9zzNivJwZ7RpDfu4KRNyg5Wab";
    private static final String JDBC_URL = "jdbc:derby:GbRomDB;create=true;dataEncoding=UTF8";
    private Connection conn;

    private static DatabaseManager instance;

    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null || instance.conn == null || instance.conn.isClosed()) {
            dropDatabase();
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

    public Connection getConnection() {
        return this.conn;
    }

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
     * This method checks if a table already exists.
     *
     * @param tableName
     * @return true if the table already exists, false otherwise
     * @throws SQLException
     */
    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet resultSet = meta.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"});
        return resultSet.next();
    }

    public void initialiseDatabase() throws SQLException {
        createTables();
    }

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

    public void cleanDatabase() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Check if tables exist before trying to delete from them
            DatabaseMetaData metaData = conn.getMetaData();

            String[] tables = {
                "Collection", "CartridgeType", "ROMSize",
                "RAMSize", "NewLicenseeCode", "OldLicenseeCode"
            };

            for (String table : tables) {
                if (tableExists(table)) {
                    stmt.execute("DELETE FROM " + table);
                }
            }

            // Commit the deletes
            conn.commit();
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Create CartridgeType table if it doesn't exist
            if (!tableExists("CartridgeType")) {
                stmt.executeUpdate("CREATE TABLE CartridgeType ("
                        + "type_code CHAR(2) FOR BIT DATA PRIMARY KEY,"
                        // Hex value as binary (00-FF)
                        + "type_description VARCHAR(100)"
                        // Description as a string
                        + ")");
            }

            // Create ROMSize table if it doesn't exist
            if (!tableExists("ROMSize")) {
                stmt.executeUpdate("CREATE TABLE ROMSize ("
                        + "size_code INT PRIMARY KEY,"
                        // Hex code as int (00-08)
                        + "size_kib INT,"
                        // Actual size in KiB
                        + "num_banks INT"
                        // Number of ROM banks
                        + ")");
            }

            // Create RAMSize table if it doesn't exist
            if (!tableExists("RAMSize")) {
                stmt.executeUpdate("CREATE TABLE RAMSize ("
                        + "size_code INT PRIMARY KEY,"
                        // Hex code as int (00-05)
                        + "size_kib INT,"
                        // Actual size in KiB
                        + "num_banks INT"
                        // Number of RAM banks
                        + ")");
                conn.commit();
            }

            // Create NewLicenseeCode table if it doesn't exist
            if (!tableExists("NewLicenseeCode")) {
                stmt.executeUpdate("CREATE TABLE NewLicenseeCode ("
                        + "licensee_code CHAR(2) FOR BIT DATA PRIMARY KEY,"
                        // Hex value as binary (00-FF)
                        + "licensee_code_ascii VARCHAR(2),"
                        // ASCII translation of hex code as string
                        + "publisher VARCHAR(100)"
                        // Publisher name as a string
                        + ")");
                conn.commit();
            }

            // Create OldLicenseeCode table if it doesn't exist
            if (!tableExists("OldLicenseeCode")) {
                stmt.executeUpdate("CREATE TABLE OldLicenseeCode ("
                        + "licensee_code CHAR(2) FOR BIT DATA PRIMARY KEY,"
                        // Hex value as binary (00-FF)
                        + "publisher VARCHAR(100)"
                        // Publisher name as a string
                        + ")");
                conn.commit();
            }

            // Create Collection table if it doesn't exist (blank by default)
            if (!tableExists("Collection")) {
                stmt.executeUpdate("CREATE TABLE Collection ("
                        + "title VARCHAR(100),"
                        + "name VARCHAR(200),"
                        + "type_code CHAR(2) FOR BIT DATA,"
                        + "rom_rev CHAR(2) FOR BIT DATA,"
                        + "rom_size_code INT,"
                        + "ram_size_code INT,"
                        + "sgb_flag BOOLEAN,"
                        + "cgb_flag CHAR(2) FOR BIT DATA,"
                        + "dest_code INT,"
                        + "licensee_code CHAR(2) FOR BIT DATA,"
                        + "head_chksm CHAR(2) FOR BIT DATA,"
                        + "global_chksm CHAR(2) FOR BIT DATA,"
                        + "PRIMARY KEY (title, rom_rev, global_chksm)"
                        + ")");
                conn.commit();
            }

            // Populate tables with initial data
            populateCartridgeType(stmt);
            populateROMSize(stmt);
            populateRAMSize(stmt);
            populateNewLicenseeCode(stmt);
            populateOldLicenseeCode(stmt);
        }
    }

    /**
     * Helper method to insert Cartridge Type as binary.
     *
     * @param pstmt
     * @param hex
     * @param description
     * @throws SQLException
     */
    private void insertCartridgeType(PreparedStatement pstmt, String hex, String description) throws SQLException {
        pstmt.setBytes(1, HexFormat.of().parseHex(hex));
        pstmt.setString(2, description);
        pstmt.executeUpdate();
    }

    private void populateCartridgeType(Statement stmt) throws SQLException {
        String insertQuery = "INSERT INTO CartridgeType (type_code, type_description) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            // Add Cartridge Types
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

    private void populateROMSize(Statement stmt) throws SQLException {
        // Add ROM sizes
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

    private void populateRAMSize(Statement stmt) throws SQLException {
        // Add RAM sizes
        stmt.executeUpdate("INSERT INTO RAMSize VALUES (0, 0, 0)");      // No RAM
        stmt.executeUpdate("INSERT INTO RAMSize VALUES (1, 2, 1)");      // 2 KiB,   unused (1 bank presumed)
        stmt.executeUpdate("INSERT INTO RAMSize VALUES (2, 8, 1)");      // 8 KiB,   1 x 8 KiB bank
        stmt.executeUpdate("INSERT INTO RAMSize VALUES (3, 32, 4)");     // 32 KiB,  4 x 8 KiB banks
        stmt.executeUpdate("INSERT INTO RAMSize VALUES (4, 128, 16)");   // 128 KiB, 16 x 8 KiB banks
        stmt.executeUpdate("INSERT INTO RAMSize VALUES (5, 64, 8)");     // 64 KiB,  8 x 8 KiB banks
    }

    /**
     * Helper method to insert New Licensee Code as binary.
     *
     * @param pstmt
     * @param hex
     * @param ascii
     * @param publisher
     * @throws SQLException
     */
    private void insertNewLicenseeCode(PreparedStatement pstmt, String hex, String ascii, String publisher) throws SQLException {
        pstmt.setBytes(1, ascii.getBytes(StandardCharsets.US_ASCII));
        pstmt.setString(2, ascii);
        pstmt.setString(3, publisher);
        pstmt.executeUpdate();
    }

    private void populateNewLicenseeCode(Statement stmt) throws SQLException {
        String insertQuery = "INSERT INTO NewLicenseeCode (licensee_code, licensee_code_ascii, publisher) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            // Add New Licensee Codes
            insertNewLicenseeCode(pstmt, "3030", "00", "None");
            insertNewLicenseeCode(pstmt, "3031", "01", "Nintendo");
            insertNewLicenseeCode(pstmt, "3032", "02", "Rocket Games");
            insertNewLicenseeCode(pstmt, "3038", "08", "Capcom");
            insertNewLicenseeCode(pstmt, "3039", "09", "Hot B Co.");
            insertNewLicenseeCode(pstmt, "3041", "0A", "Jaleco");
            insertNewLicenseeCode(pstmt, "3042", "0B", "Coconuts Japan");
            insertNewLicenseeCode(pstmt, "3043", "0C", "Coconuts Japan/G.X.Media");
            insertNewLicenseeCode(pstmt, "3048", "0H", "Starfish");
            insertNewLicenseeCode(pstmt, "304C", "0L", "Warashi Inc.");
            insertNewLicenseeCode(pstmt, "304E", "0N", "Nowpro");
            insertNewLicenseeCode(pstmt, "3050", "0P", "Game Village");
            insertNewLicenseeCode(pstmt, "3051", "0Q", "IE Institute");
            insertNewLicenseeCode(pstmt, "3133", "13", "Electronic Arts Japan");
            insertNewLicenseeCode(pstmt, "3138", "18", "Hudson Soft Japan");
            insertNewLicenseeCode(pstmt, "3139", "19", "B-AI");
            insertNewLicenseeCode(pstmt, "3141", "1A", "Yonoman");
            insertNewLicenseeCode(pstmt, "3148", "1H", "Yojigen");
            insertNewLicenseeCode(pstmt, "314D", "1M", "Microcabin Corporation");
            insertNewLicenseeCode(pstmt, "314E", "1N", "Dazz");
            insertNewLicenseeCode(pstmt, "3150", "1P", "Creatures Inc.");
            insertNewLicenseeCode(pstmt, "3151", "1Q", "TDK Deep Impresion");
            insertNewLicenseeCode(pstmt, "3230", "20", "KSS");
            insertNewLicenseeCode(pstmt, "3232", "22", "Planning Office WADA");
            insertNewLicenseeCode(pstmt, "3238", "28", "Kemco Japan");
            insertNewLicenseeCode(pstmt, "3244", "2D", "Visit");
            insertNewLicenseeCode(pstmt, "3248", "2H", "Ubisoft Japan");
            insertNewLicenseeCode(pstmt, "324B", "2K", "NEC InterChannel");
            insertNewLicenseeCode(pstmt, "324C", "2L", "Tam");
            insertNewLicenseeCode(pstmt, "324D", "2M", "Jordan");
            insertNewLicenseeCode(pstmt, "324E", "2N", "Smilesoft");
            insertNewLicenseeCode(pstmt, "3250", "2P", "The Pokémon Company");
            insertNewLicenseeCode(pstmt, "3330", "30", "Viacom New Media");
            insertNewLicenseeCode(pstmt, "3334", "34", "Magifact");
            insertNewLicenseeCode(pstmt, "3335", "35", "Hect");
            insertNewLicenseeCode(pstmt, "3336", "36", "Codemasters");
            insertNewLicenseeCode(pstmt, "3337", "37", "GAGA Communications");
            insertNewLicenseeCode(pstmt, "3338", "38", "Laguna");
            insertNewLicenseeCode(pstmt, "3339", "39", "Telstar Fun and Games");
            insertNewLicenseeCode(pstmt, "333A", "3:", "Nintendo/Mani");
            insertNewLicenseeCode(pstmt, "3345", "3E", "Gremlin Graphics");
            insertNewLicenseeCode(pstmt, "3431", "41", "Ubi Soft Entertainment");
            insertNewLicenseeCode(pstmt, "3432", "42", "Sunsoft");
            insertNewLicenseeCode(pstmt, "3437", "47", "Spectrum Holobyte");
            insertNewLicenseeCode(pstmt, "3444", "4D", "Malibu Games");
            insertNewLicenseeCode(pstmt, "3446", "4F", "Eidos/U.S. Gold");
            insertNewLicenseeCode(pstmt, "3447", "4G", "Playmates Inc.");
            insertNewLicenseeCode(pstmt, "344A", "4J", "Fox Interactive");
            insertNewLicenseeCode(pstmt, "344B", "4K", "Time Warner Interactive");
            insertNewLicenseeCode(pstmt, "3453", "4S", "Black Pearl");
            insertNewLicenseeCode(pstmt, "3458", "4X", "GT Interactive");
            insertNewLicenseeCode(pstmt, "3459", "4Y", "Rare");
            insertNewLicenseeCode(pstmt, "345A", "4Z", "Crave Entertainment");
            insertNewLicenseeCode(pstmt, "3530", "50", "Absolute Entertainment");
            insertNewLicenseeCode(pstmt, "3531", "51", "Acclaim");
            insertNewLicenseeCode(pstmt, "3532", "52", "Activision");
            insertNewLicenseeCode(pstmt, "3534", "54", "Take 2 Interactive");
            insertNewLicenseeCode(pstmt, "3535", "55", "Hi Tech Expressions");
            insertNewLicenseeCode(pstmt, "3536", "56", "LJN");
            insertNewLicenseeCode(pstmt, "3538", "58", "Mattel");
            insertNewLicenseeCode(pstmt, "3541", "5A", "Mindscape/Red Orb Ent.");
            insertNewLicenseeCode(pstmt, "3544", "5D", "Midway");
            insertNewLicenseeCode(pstmt, "3546", "5F", "American Softworks");
            insertNewLicenseeCode(pstmt, "3547", "5G", "Majesco Sales Inc");
            insertNewLicenseeCode(pstmt, "3548", "5H", "3DO");
            insertNewLicenseeCode(pstmt, "354B", "5K", "Hasbro");
            insertNewLicenseeCode(pstmt, "354C", "5L", "NewKidCo");
            insertNewLicenseeCode(pstmt, "354D", "5M", "Telegames");
            insertNewLicenseeCode(pstmt, "354E", "5N", "Metro3D");
            insertNewLicenseeCode(pstmt, "3550", "5P", "Vatical Entertainment");
            insertNewLicenseeCode(pstmt, "3551", "5Q", "LEGO Media");
            insertNewLicenseeCode(pstmt, "3554", "5T", "Cryo Interactive");
            insertNewLicenseeCode(pstmt, "3556", "5V", "Agetec Inc.");
            insertNewLicenseeCode(pstmt, "3557", "5W", "Red Storm Ent./BKN Ent.");
            insertNewLicenseeCode(pstmt, "3558", "5X", "Microids");
            insertNewLicenseeCode(pstmt, "355A", "5Z", "Conspiracy Entertainment Corp.");
            insertNewLicenseeCode(pstmt, "3630", "60", "Titus Interactive Studios");
            insertNewLicenseeCode(pstmt, "3631", "61", "Virgin Interactive");
            insertNewLicenseeCode(pstmt, "3634", "64", "LucasArts Entertainment");
            insertNewLicenseeCode(pstmt, "3637", "67", "Ocean");
            insertNewLicenseeCode(pstmt, "3639", "69", "Electronic Arts");
            insertNewLicenseeCode(pstmt, "3646", "6F", "Electro Brain");
            insertNewLicenseeCode(pstmt, "3647", "6G", "The Learning Company");
            insertNewLicenseeCode(pstmt, "3648", "6H", "BBC");
            insertNewLicenseeCode(pstmt, "364A", "6J", "Software 2000");
            insertNewLicenseeCode(pstmt, "364C", "6L", "BAM! Entertainment");
            insertNewLicenseeCode(pstmt, "364D", "6M", "Studio 3");
            insertNewLicenseeCode(pstmt, "3650", "6P", "Ravensburger Interactive Media GmbH");
            insertNewLicenseeCode(pstmt, "3651", "6Q", "Classified Games");
            insertNewLicenseeCode(pstmt, "3652", "6R", "Sound Source Interactive");
            insertNewLicenseeCode(pstmt, "3653", "6S", "TDK Mediactive");
            insertNewLicenseeCode(pstmt, "3654", "6T", "Interactive Imagination");
            insertNewLicenseeCode(pstmt, "3655", "6U", "DreamCatcher");
            insertNewLicenseeCode(pstmt, "3656", "6V", "JoWood Productions");
            insertNewLicenseeCode(pstmt, "3658", "6X", "Wannado Edition");
            insertNewLicenseeCode(pstmt, "3659", "6Y", "LSP");
            insertNewLicenseeCode(pstmt, "365A", "6Z", "ITE Media");
            insertNewLicenseeCode(pstmt, "3730", "70", "Infogrames");
            insertNewLicenseeCode(pstmt, "3731", "71", "Interplay");
            insertNewLicenseeCode(pstmt, "3732", "72", "JVC Musical Industries Inc");
            insertNewLicenseeCode(pstmt, "3735", "75", "SCI");
            insertNewLicenseeCode(pstmt, "3738", "78", "THQ");
            insertNewLicenseeCode(pstmt, "3739", "79", "Accolade");
            insertNewLicenseeCode(pstmt, "3744", "7D", "Universal Interactive Studios");
            insertNewLicenseeCode(pstmt, "3746", "7F", "Kemco");
            insertNewLicenseeCode(pstmt, "3747", "7G", "Rage Software");
            insertNewLicenseeCode(pstmt, "3748", "7H", "Encore");
            insertNewLicenseeCode(pstmt, "374B", "7K", "BVM");
            insertNewLicenseeCode(pstmt, "374C", "7L", "Simon & Schuster Interactive");
            insertNewLicenseeCode(pstmt, "3837", "87", "Tsukuda Original");
            insertNewLicenseeCode(pstmt, "3842", "8B", "Bulletproof Software");
            insertNewLicenseeCode(pstmt, "3843", "8C", "Vic Tokai Inc.");
            insertNewLicenseeCode(pstmt, "3846", "8F", "I\"Max");
            insertNewLicenseeCode(pstmt, "384A", "8J", "General Entertainment");
            insertNewLicenseeCode(pstmt, "384B", "8K", "Japan System Supply");
            insertNewLicenseeCode(pstmt, "384D", "8M", "CyberFront");
            insertNewLicenseeCode(pstmt, "384E", "8N", "Success");
            insertNewLicenseeCode(pstmt, "3850", "8P", "SEGA Japan");
            insertNewLicenseeCode(pstmt, "3931", "91", "Chun Soft");
            insertNewLicenseeCode(pstmt, "3932", "92", "Video System");
            insertNewLicenseeCode(pstmt, "3933", "93", "BEC");
            insertNewLicenseeCode(pstmt, "3939", "99", "Victor Interactive Software");
            insertNewLicenseeCode(pstmt, "3941", "9A", "Nichibutsu/Nihon Bussan");
            insertNewLicenseeCode(pstmt, "3942", "9B", "Tecmo");
            insertNewLicenseeCode(pstmt, "3943", "9C", "Imagineer");
            insertNewLicenseeCode(pstmt, "3948", "9H", "Bottom Up");
            insertNewLicenseeCode(pstmt, "394B", "9K", "Syscom");
            insertNewLicenseeCode(pstmt, "394C", "9L", "Hasbro Japan");
            insertNewLicenseeCode(pstmt, "394D", "9M", "Jaguar");
            insertNewLicenseeCode(pstmt, "394E", "9N", "Marvelous Entertainment");
            insertNewLicenseeCode(pstmt, "4130", "A0", "Telenet");
            insertNewLicenseeCode(pstmt, "4131", "A1", "Hori");
            insertNewLicenseeCode(pstmt, "4134", "A4", "Konami");
            insertNewLicenseeCode(pstmt, "4137", "A7", "Takara");
            insertNewLicenseeCode(pstmt, "4139", "A9", "Technos Japan Corp.");
            insertNewLicenseeCode(pstmt, "4144", "AD", "Toho");
            insertNewLicenseeCode(pstmt, "4146", "AF", "Namco");
            insertNewLicenseeCode(pstmt, "4148", "AH", "J-Wing");
            insertNewLicenseeCode(pstmt, "414B", "AK", "KID");
            insertNewLicenseeCode(pstmt, "414C", "AL", "MediaFactory");
            insertNewLicenseeCode(pstmt, "414D", "AM", "BIOX Co., Ltd./GAPS Inc.");
            insertNewLicenseeCode(pstmt, "414E", "AN", "Lay-Up");
            insertNewLicenseeCode(pstmt, "4150", "AP", "Infogrames Hudson");
            insertNewLicenseeCode(pstmt, "4151", "AQ", "Kiratto. Ludic Inc");
            insertNewLicenseeCode(pstmt, "4230", "B0", "Acclaim Japan");
            insertNewLicenseeCode(pstmt, "4231", "B1", "ASCII");
            insertNewLicenseeCode(pstmt, "4232", "B2", "Bandai");
            insertNewLicenseeCode(pstmt, "4234", "B4", "Enix");
            insertNewLicenseeCode(pstmt, "4241", "BA", "Culture Brain");
            insertNewLicenseeCode(pstmt, "4242", "BB", "Sunsoft");
            insertNewLicenseeCode(pstmt, "4246", "BF", "Sammy");
            insertNewLicenseeCode(pstmt, "4247", "BG", "Magical");
            insertNewLicenseeCode(pstmt, "424A", "BJ", "Compile");
            insertNewLicenseeCode(pstmt, "424C", "BL", "MTO");
            insertNewLicenseeCode(pstmt, "424D", "BM", "XING Entertainment");
            insertNewLicenseeCode(pstmt, "424E", "BN", "Sunrise Interactive");
            insertNewLicenseeCode(pstmt, "4250", "BP", "Global A Entertainment");
            insertNewLicenseeCode(pstmt, "4330", "C0", "Taito");
            insertNewLicenseeCode(pstmt, "4336", "C6", "Tonkin House");
            insertNewLicenseeCode(pstmt, "4338", "C8", "Koei");
            insertNewLicenseeCode(pstmt, "4342", "CB", "Vapinc/NTVIC");
            insertNewLicenseeCode(pstmt, "4345", "CE", "FCI/Pony Canyon");
            insertNewLicenseeCode(pstmt, "4346", "CF", "Angel");
            insertNewLicenseeCode(pstmt, "434A", "CJ", "BOSS Communication");
            insertNewLicenseeCode(pstmt, "434B", "CK", "Axela");
            insertNewLicenseeCode(pstmt, "434E", "CN", "NEC Interchannel");
            insertNewLicenseeCode(pstmt, "4350", "CP", "Enterbrain");
            insertNewLicenseeCode(pstmt, "4434", "D4", "Ask Kodansa");
            insertNewLicenseeCode(pstmt, "4436", "D6", "Naxat");
            insertNewLicenseeCode(pstmt, "4439", "D9", "Banpresto");
            insertNewLicenseeCode(pstmt, "4441", "DA", "TOMY");
            insertNewLicenseeCode(pstmt, "4444", "DD", "NCS");
            insertNewLicenseeCode(pstmt, "4445", "DE", "Human Entertainment");
            insertNewLicenseeCode(pstmt, "4446", "DF", "Altron Corporation");
            insertNewLicenseeCode(pstmt, "4448", "DH", "Gaps Inc.");
            insertNewLicenseeCode(pstmt, "444A", "DJ", "Epoch Co., Ltd.");
            insertNewLicenseeCode(pstmt, "444B", "DK", "Kodansha");
            insertNewLicenseeCode(pstmt, "444C", "DL", "Digital Kids");
            insertNewLicenseeCode(pstmt, "444E", "DN", "ELF");
            insertNewLicenseeCode(pstmt, "4450", "DP", "Prime System");
            insertNewLicenseeCode(pstmt, "4532", "E2", "Yutaka");
            insertNewLicenseeCode(pstmt, "4535", "E5", "Epoch");
            insertNewLicenseeCode(pstmt, "4537", "E7", "Athena");
            insertNewLicenseeCode(pstmt, "4538", "E8", "Asmik Ace Entertainment Inc.");
            insertNewLicenseeCode(pstmt, "4539", "E9", "Natsume");
            insertNewLicenseeCode(pstmt, "4541", "EA", "King Records");
            insertNewLicenseeCode(pstmt, "4542", "EB", "Atlus");
            insertNewLicenseeCode(pstmt, "454A", "EJ", "See old licensee code");
            insertNewLicenseeCode(pstmt, "454C", "EL", "Spike");
            insertNewLicenseeCode(pstmt, "454E", "EN", "Alphadream Corporation");
            insertNewLicenseeCode(pstmt, "4550", "EP", "Sting Entertainment");
            insertNewLicenseeCode(pstmt, "4551", "EQ", "Omega Project Co., Ltd.");
            insertNewLicenseeCode(pstmt, "4642", "FB", "Psygnosis");
            insertNewLicenseeCode(pstmt, "4647", "FG", "Jupiter Corporation");
            insertNewLicenseeCode(pstmt, "5258", "RX", "Li Cheng");
            insertNewLicenseeCode(pstmt, "5335", "S5", "SouthPeak Interactive");
            insertNewLicenseeCode(pstmt, "5858", "XX", "Rocket Games");
        }
    }

    /**
     * Helper method to insert Old Licensee Code as binary.
     *
     * @param pstmt
     * @param hex
     * @param publisher
     * @throws SQLException
     */
    private void insertOldLicenseeCode(PreparedStatement pstmt, String hex, String publisher) throws SQLException {
        pstmt.setBytes(1, HexFormat.of().parseHex(hex));
        pstmt.setString(2, publisher);
        pstmt.executeUpdate();
    }

    private void populateOldLicenseeCode(Statement stmt) throws SQLException {
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
