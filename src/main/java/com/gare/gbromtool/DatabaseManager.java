package com.gare.gbromtool;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

    public DatabaseManager() throws SQLException {
        establishConnection();
        initialiseDatabase();
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
                System.out.println("Derby driver not found: " + e.getMessage());
                throw new SQLException("Derby driver not found", e);
            } catch (SQLException ex) {
                System.out.println("Error connecting to the database: " + ex.getMessage());
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
     * @param tableName
     * @return true if the table already exists, false otherwise
     * @throws SQLException 
     */
    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet resultSet = meta.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"});
        return resultSet.next();
    }

    private void initialiseDatabase() throws SQLException {
        createTables();
    }

    private void createTables() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Create CartridgeType table if it doesn't exist
            if (!tableExists("CartridgeType")) {
                stmt.executeUpdate("CREATE TABLE CartridgeType ("
                        + "type_code BINARY(2) PRIMARY KEY,"
                        // Hex value as binary (00-FF)
                        + "type_description VARCHAR(100),"
                        // Description as a string
                        + ")");
                String createCartridgeType = "CREATE TABLE CartridgeType ("
                        + "type_code BINARY(2) PRIMARY KEY,"
                        + "type_description VARCHAR(100))";
                System.out.println("Executing SQL: " + createCartridgeType);
                stmt.executeUpdate(createCartridgeType);
                conn.commit();
            }

            // Create ROMSize table if it doesn't exist
            if (!tableExists("ROMSize")) {
                stmt.executeUpdate("CREATE TABLE ROMSize ("
                        + "size_code INT PRIMARY KEY,"
                        // Hex code as int (00-08)
                        + "size_kib INT,"
                        // Actual size in KiB
                        + "num_banks INT,"
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
                        + "num_banks INT,"
                        // Number of RAM banks
                        + ")");
                conn.commit();
            }

            // Create NewLicenseeCode table if it doesn't exist
            if (!tableExists("NewLicenseeCode")) {
                stmt.executeUpdate("CREATE TABLE NewLicenseeCode ("
                        + "licensee_code BINARY(2) PRIMARY KEY,"
                        // Hex value as binary (00-FF)
                        + "licensee_code_ascii VARCHAR(2),"
                        // ASCII translation of hex code as string
                        + "publisher VARCHAR(100),"
                        // Publisher name as a string
                        + ")");
                conn.commit();
            }

            // Create OldLicenseeCode table if it doesn't exist
            if (!tableExists("OldLicenseeCode")) {
                stmt.executeUpdate("CREATE TABLE OldLicenseeCode ("
                        + "licensee_code BINARY(2) PRIMARY KEY,"
                        // Hex value as binary (00-FF)
                        + "publisher VARCHAR(100),"
                        // Publisher name as a string
                        + ")");
                conn.commit();
            }

            // Create Collection table if it doesn't exist (blank by default)
            if (!tableExists("Collection")) {
                stmt.executeUpdate("CREATE TABLE Collection ("
                        + "title VARCHAR(100) PRIMARY KEY,"
                        + "type_code BINARY(2),"
                        + "rom_rev BINARY(2),"
                        + "rom_size_code INT,"
                        + "ram_size_code INT,"
                        + "sgb_flag BOOLEAN,"
                        + "cgb_flag BINARY(2),"
                        + "dest_code INT,"
                        + "licensee_code BINARY(2),"
                        + "head_chksm BINARY(2),"
                        + "global_chksm BINARY(2),"
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

    private void populateCartridgeType(Statement stmt) throws SQLException {
        // Add Cartridge Types
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('00'), 'ROM ONLY')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('01'), 'MBC1')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('02'), 'MBC1+RAM')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('03'), 'MBC1+RAM+BATTERY')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('05'), 'MBC2')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('06'), 'MBC2+BATTERY')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('08'), 'ROM+RAM')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('09'), 'ROM+RAM+BATTERY')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('0B'), 'MMM01')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('0C'), 'MMM01+RAM')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('0D'), 'MMM01+RAM+BATTERY')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('0F'), 'MBC3+TIMER+BATTERY')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('10'), 'MBC3+TIMER+RAM+BATTERY')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('11'), 'MBC3')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('12'), 'MBC3+RAM')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('13'), 'MBC3+RAM+BATTERY')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('19'), 'MBC5')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('1A'), 'MBC5+RAM')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('1B'), 'MBC5+RAM+BATTERY')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('1C'), 'MBC5+RUMBLE')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('1D'), 'MBC5+RUMBLE+RAM')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('1E'), 'MBC5+RUMBLE+RAM+BATTERY')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('20'), 'MBC6')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('22'), 'MBC7+SENSOR+RUMBLE+RAM+BATTERY')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('FC'), 'POCKET CAMERA')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('FD'), 'BANDAI TAMA5')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('FE'), 'HuC3')");
        stmt.executeUpdate("INSERT INTO CartridgeType VALUES (UNHEX('FF'), 'HuC1+RAM+BATTERY')");
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

    private void populateNewLicenseeCode(Statement stmt) throws SQLException {
        // Add New Licensee Codes
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3030'), '00', 'None')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3031'), '01', 'Nintendo')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3032'), '02', 'Rocket Games')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3038'), '08', 'Capcom')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3039'), '09', 'Hot B Co.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3041'), '0A', 'Jaleco')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3042'), '0B', 'Coconuts Japan')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3043'), '0C', 'Coconuts Japan/G.X.Media')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3048'), '0H', 'Starfish')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('304C'), '0L', 'Warashi Inc.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('304E'), '0N', 'Nowpro')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3050'), '0P', 'Game Village')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3051'), '0Q', 'IE Institute')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3133'), '13', 'Electronic Arts Japan')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3138'), '18', 'Hudson Soft Japan')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3139'), '19', 'B-AI')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3141'), '1A', 'Yonoman')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3148'), '1H', 'Yojigen')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('314D'), '1M', 'Microcabin Corporation')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('314E'), '1N', 'Dazz')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3150'), '1P', 'Creatures Inc.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3151'), '1Q', 'TDK Deep Impresion')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3230'), '20', 'KSS')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3232'), '22', 'Planning Office WADA')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3238'), '28', 'Kemco Japan')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3244'), '2D', 'Visit')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3248'), '2H', 'Ubisoft Japan')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('324B'), '2K', 'NEC InterChannel')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('324C'), '2L', 'Tam')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('324D'), '2M', 'Jordan')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('324E'), '2N', 'Smilesoft')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3250'), '2P', 'The Pokémon Company')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3330'), '30', 'Viacom New Media')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3334'), '34', 'Magifact')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3335'), '35', 'Hect')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3336'), '36', 'Codemasters')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3337'), '37', 'GAGA Communications')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3338'), '38', 'Laguna')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3339'), '39', 'Telstar Fun and Games')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('333A'), '3:', 'Nintendo/Mani')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3345'), '3E', 'Gremlin Graphics')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3431'), '41', 'Ubi Soft Entertainment')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3432'), '42', 'Sunsoft')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3437'), '47', 'Spectrum Holobyte')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3444'), '4D', 'Malibu Games')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3446'), '4F', 'Eidos/U.S. Gold')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3447'), '4G', 'Playmates Inc.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('344A'), '4J', 'Fox Interactive')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('344B'), '4K', 'Time Warner Interactive')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3453'), '4S', 'Black Pearl')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3458'), '4X', 'GT Interactive')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3459'), '4Y', 'Rare')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('345A'), '4Z', 'Crave Entertainment')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3530'), '50', 'Absolute Entertainment')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3531'), '51', 'Acclaim')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3532'), '52', 'Activision')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3534'), '54', 'Take 2 Interactive')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3535'), '55', 'Hi Tech Expressions')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3536'), '56', 'LJN')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3538'), '58', 'Mattel')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3541'), '5A', 'Mindscape/Red Orb Ent.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3544'), '5D', 'Midway')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3546'), '5F', 'American Softworks')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3547'), '5G', 'Majesco Sales Inc')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3548'), '5H', '3DO')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('354B'), '5K', 'Hasbro')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('354C'), '5L', 'NewKidCo')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('354D'), '5M', 'Telegames')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('354E'), '5N', 'Metro3D')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3550'), '5P', 'Vatical Entertainment')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3551'), '5Q', 'LEGO Media')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3554'), '5T', 'Cryo Interactive')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3556'), '5V', 'Agetec Inc.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3557'), '5W', 'Red Storm Ent./BKN Ent.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3558'), '5X', 'Microids')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('355A'), '5Z', 'Conspiracy Entertainment Corp.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3630'), '60', 'Titus Interactive Studios')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3631'), '61', 'Virgin Interactive')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3634'), '64', 'LucasArts Entertainment')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3637'), '67', 'Ocean')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3639'), '69', 'Electronic Arts')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3646'), '6F', 'Electro Brain')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3647'), '6G', 'The Learning Company')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3648'), '6H', 'BBC')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('364A'), '6J', 'Software 2000')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('364C'), '6L', 'BAM! Entertainment')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('364D'), '6M', 'Studio 3')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3650'), '6P', 'Ravensburger Interactive Media GmbH')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3651'), '6Q', 'Classified Games')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3652'), '6R', 'Sound Source Interactive')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3653'), '6S', 'TDK Mediactive')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3654'), '6T', 'Interactive Imagination')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3655'), '6U', 'DreamCatcher')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3656'), '6V', 'JoWood Productions')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3658'), '6X', 'Wannado Edition')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3659'), '6Y', 'LSP')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('365A'), '6Z', 'ITE Media')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3730'), '70', 'Infogrames')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3731'), '71', 'Interplay')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3732'), '72', 'JVC Musical Industries Inc')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3735'), '75', 'SCI')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3738'), '78', 'THQ')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3739'), '79', 'Accolade')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3744'), '7D', 'Universal Interactive Studios')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3746'), '7F', 'Kemco')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3747'), '7G', 'Rage Software')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3748'), '7H', 'Encore')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('374B'), '7K', 'BVM')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('374C'), '7L', 'Simon & Schuster Interactive')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3837'), '87', 'Tsukuda Original')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3842'), '8B', 'Bulletproof Software')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3843'), '8C', 'Vic Tokai Inc.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3846'), '8F', 'I\'Max')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('384A'), '8J', 'General Entertainment')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('384B'), '8K', 'Japan System Supply')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('384D'), '8M', 'CyberFront')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('384E'), '8N', 'Success')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3850'), '8P', 'SEGA Japan')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3931'), '91', 'Chun Soft')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3932'), '92', 'Video System')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3933'), '93', 'BEC')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3939'), '99', 'Victor Interactive Software')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3941'), '9A', 'Nichibutsu/Nihon Bussan')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3942'), '9B', 'Tecmo')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3943'), '9C', 'Imagineer')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('3948'), '9H', 'Bottom Up')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('394B'), '9K', 'Syscom')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('394C'), '9L', 'Hasbro Japan')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('394D'), '9M', 'Jaguar')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('394E'), '9N', 'Marvelous Entertainment')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4130'), 'A0', 'Telenet')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4131'), 'A1', 'Hori')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4134'), 'A4', 'Konami')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4137'), 'A7', 'Takara')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4139'), 'A9', 'Technos Japan Corp.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4144'), 'AD', 'Toho')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4146'), 'AF', 'Namco')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4148'), 'AH', 'J-Wing')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('414B'), 'AK', 'KID')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('414C'), 'AL', 'MediaFactory')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('414D'), 'AM', 'BIOX Co., Ltd./GAPS Inc.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('414E'), 'AN', 'Lay-Up')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4150'), 'AP', 'Infogrames Hudson')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4151'), 'AQ', 'Kiratto. Ludic Inc')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4230'), 'B0', 'Acclaim Japan')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4231'), 'B1', 'ASCII')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4232'), 'B2', 'Bandai')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4234'), 'B4', 'Enix')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4241'), 'BA', 'Culture Brain')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4242'), 'BB', 'Sunsoft')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4246'), 'BF', 'Sammy')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4247'), 'BG', 'Magical')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('424A'), 'BJ', 'Compile')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('424C'), 'BL', 'MTO')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('424D'), 'BM', 'XING Entertainment')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('424E'), 'BN', 'Sunrise Interactive')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4250'), 'BP', 'Global A Entertainment')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4330'), 'C0', 'Taito')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4336'), 'C6', 'Tonkin House')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4338'), 'C8', 'Koei')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4342'), 'CB', 'Vapinc/NTVIC')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4345'), 'CE', 'FCI/Pony Canyon')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4346'), 'CF', 'Angel')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('434A'), 'CJ', 'BOSS Communication')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('434B'), 'CK', 'Axela')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('434E'), 'CN', 'NEC Interchannel')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4350'), 'CP', 'Enterbrain')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4434'), 'D4', 'Ask Kodansa')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4436'), 'D6', 'Naxat')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4439'), 'D9', 'Banpresto')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4441'), 'DA', 'TOMY')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4444'), 'DD', 'NCS')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4445'), 'DE', 'Human Entertainment')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4446'), 'DF', 'Altron Corporation')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4448'), 'DH', 'Gaps Inc.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('444A'), 'DJ', 'Epoch Co., Ltd.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('444B'), 'DK', 'Kodansha')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('444C'), 'DL', 'Digital Kids')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('444E'), 'DN', 'ELF')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4450'), 'DP', 'Prime System')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4532'), 'E2', 'Yutaka')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4535'), 'E5', 'Epoch')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4537'), 'E7', 'Athena')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4538'), 'E8', 'Asmik Ace Entertainment Inc.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4539'), 'E9', 'Natsume')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4541'), 'EA', 'King Records')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4542'), 'EB', 'Atlus')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('454A'), 'EJ', 'See old licensee code')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('454C'), 'EL', 'Spike')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('454E'), 'EN', 'Alphadream Corporation')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4550'), 'EP', 'Sting Entertainment')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4551'), 'EQ', 'Omega Project Co., Ltd.')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4642'), 'FB', 'Psygnosis')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('4647'), 'FG', 'Jupiter Corporation')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('5258'), 'RX', 'Li Cheng')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('5335'), 'S5', 'SouthPeak Interactive')");
        stmt.execute("INSERT INTO NewLicenseeCode VALUES (UNHEX('5858'), 'XX', 'Rocket Games')");
    }

    private void populateOldLicenseeCode(Statement stmt) throws SQLException {
        // Add Old Licensee Codes
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('00'), 'None')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('01'), 'Nintendo')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('08'), 'Capcom')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('09'), 'HOT-B')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('0A'), 'Jaleco')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('0B'), 'Coconuts Japan')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('0C'), 'Elite Systems')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('13'), 'EA (Electronic Arts)')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('18'), 'Hudson Soft')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('19'), 'B-AI')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('1A'), 'Yanoman')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('1D'), 'Japan Clary')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('1F'), 'Virgin Games Ltd.')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('24'), 'PCM Complete')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('25'), 'San-X')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('28'), 'Kemco')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('29'), 'SETA Corporation')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('30'), 'Infogrames')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('31'), 'Nintendo')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('32'), 'Bandai')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('33'), 'See new licensee code')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('34'), 'Konami')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('35'), 'HectorSoft')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('38'), 'Capcom')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('39'), 'Banpresto')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('3C'), 'Entertainment Interactive (stub)')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('3E'), 'Gremlin Graphics')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('41'), 'Ubi Soft')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('42'), 'Atlus')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('44'), 'Malibu Interactive')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('46'), 'Angel')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('47'), 'Spectrum HoloByte')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('49'), 'Irem')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('4A'), 'Virgin Games Ltd.')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('4D'), 'Malibu Interactive')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('4F'), 'U.S. Gold')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('50'), 'Absolute')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('51'), 'Acclaim Entertainment')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('52'), 'Activision')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('53'), 'Sammy USA Corporation')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('54'), 'GameTek')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('55'), 'Park Place')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('56'), 'LJN')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('57'), 'Matchbox')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('59'), 'Milton Bradley Company')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('5A'), 'Mindscape')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('5B'), 'Romstar')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('5C'), 'TAXAN')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('5D'), 'Tradewest')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('60'), 'Titus Interactive')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('61'), 'Virgin Games, Inc.')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('67'), 'Ocean Software')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('69'), 'EA (Electronic Arts)')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('6E'), 'Elite Systems')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('6F'), 'Electro Brain')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('70'), 'Infogrames')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('71'), 'Interplay Entertainment')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('72'), 'JVC Musical Industries, Inc.')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('73'), 'Parker Brothers')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('75'), 'The Sales Curve Limited')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('78'), 'THQ')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('79'), 'Accolade')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('7A'), 'Triffix Entertainment')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('7C'), 'MicroProse')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('7F'), 'Kemco')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('80'), 'Misawa Entertainment')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('83'), 'LOZC G.')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('86'), 'Tokuma Shoten')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('8B'), 'Bullet-Proof Software')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('8C'), 'Vic Tokai Corp.')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('8E'), 'Ape Inc.')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('8F'), 'I'Max')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('91'), 'Chunsoft Co.')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('92'), 'Video System')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('93'), 'Tsubaraya Productions')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('95'), 'Varie')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('96'), 'Yonezawa PR21')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('97'), 'Kemco')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('99'), 'Arc')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('9A'), 'Nihon Bussan')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('9B'), 'Tecmo')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('9C'), 'Imagineer')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('9D'), 'Banpresto')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('9F'), 'Nova Games')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('A1'), 'Hori Electric')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('A2'), 'Bandai')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('A4'), 'Konami')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('A6'), 'Kawada ')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('A7'), 'Takara')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('A9'), 'Technos Japan')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('AA'), 'Victor Musical Industries, Inc.')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('AC'), 'Toei Animation')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('AD'), 'Toho')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('AF'), 'Namco')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('B0'), 'Acclaim Entertainment')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('B1'), 'ASCII Corporation')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('B2'), 'Bandai')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('B4'), 'Square Enix')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('B6'), 'HAL Laboratory')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('B7'), 'SNK')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('B9'), 'Pony Canyon')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('BA'), 'Culture Brain')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('BB'), 'Sunsoft')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('BD'), 'Sony Imagesoft')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('BF'), 'Sammy Corporation')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('C0'), 'Taito')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('C2'), 'Kemco')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('C3'), 'Square')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('C4'), 'Tokuma Shoten')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('C5'), 'Data East')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('C6'), 'Tonkin House')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('C8'), 'Koei')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('C9'), 'UFL')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('CA'), 'Ultra Games')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('CB'), 'VAP, Inc.')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('CC'), 'Use Corporation')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('CD'), 'Meldac')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('CE'), 'Pony Canyon')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('CF'), 'Angel')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('D0'), 'Taito')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('D1'), 'SOFEL (Software Engineering Lab)')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('D2'), 'Quest')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('D3'), 'Sigma Enterprises')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('D4'), 'ASK Kodansha Co.')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('D6'), 'Naxat Soft')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('D7'), 'Copya System')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('D9'), 'Banpresto')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('DA'), 'Tomy')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('DB'), 'LJN Japan')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('DD'), 'Nippon Computer Systems')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('DE'), 'Human Entertainment')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('DF'), 'Altron')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('E0'), 'Jaleco')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('E1'), 'Towa Chiki')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('E2'), 'Yutaka')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('E3'), 'Varie')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('E5'), 'Epoch')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('E7'), 'Athena')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('E8'), 'Asmik Ace Entertainment')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('E9'), 'Natsume')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('EA'), 'King Records')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('EB'), 'Atlus')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('EC'), 'Epic/Sony Records')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('EE'), 'IGS')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('F0'), 'A Wave')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('F3'), 'Extreme Entertainment')");
        stmt.execute("INSERT INTO OldLicenseeCode VALUES (UNHEX('FF'), 'LJN')");
    }

}
