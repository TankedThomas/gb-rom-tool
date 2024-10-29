package com.gare.gbromtool;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * This is a helper class for splitting the title and manufacturer code when
 * the latter exists.
 *
 * Attempts to solve the problem where some CGB ROMs have a manufacturer code
 * added to the end of the allotted title string without any obvious separation
 * in the ROM header.
 *
 * Based on Python code from FlashGBX by Lesserkuma.
 * https://github.com/lesserkuma/FlashGBX/blob/master/FlashGBX/RomFileDMG.py
 *
 * @author Thomas Robinson 23191795
 */
public class RomTitle {

    private final String title;
    private final String manufacturerCode;

    public RomTitle(String title, String manufacturerCode) {
        this.title = title;
        this.manufacturerCode = manufacturerCode;
    }

    public String getTitle() {
        return title;
    }

    public String getManufacturerCode() {
        return manufacturerCode;
    }

    public boolean hasManufacturerCode() {
        return !manufacturerCode.isEmpty();
    }

    /**
     * This method splits the manufacturer code from the ROM title.
     *
     * @param romData
     * @return the ROM title and manufacturer code as two strings in a RomTitle
     */
    public static RomTitle parseTitle(byte[] romData) {
        boolean isCGB = (romData[0x143] == (byte) 0x80 || romData[0x143] == (byte) 0xC0);
        byte[] titleBytes = new byte[15];
        System.arraycopy(romData, 0x134, titleBytes, 0, 15);

        System.out.println("Raw Title bytes: " + HexFormat.of().formatHex(titleBytes));

        if (isCGB) {
            // Get last 4 bytes as potential manufacturer code
            String fullTitle = new String(titleBytes, StandardCharsets.US_ASCII).trim();

            // Check if the last 4 characters could be a manufacturer code
            if (fullTitle.length() >= 4) {
                String potentialCode = fullTitle.substring(fullTitle.length() - 4);
                System.out.println("Checking Manufacturer Code: " + potentialCode); // Debug

                if (isValidManufacturerCode(potentialCode)) {
                    // Get actual title (everything before manufacturer code)
                    String actualTitle = fullTitle.substring(0, fullTitle.length() - 4).trim();

                    System.out.println("Valid Manufacturer Code found"); // Debug
                    System.out.println("Title: " + actualTitle); // Debug
                    System.out.println("Manufacturer Code: " + potentialCode); // Debug

                    return new RomTitle(actualTitle, potentialCode);
                }
            }

            System.out.println("Invalid Manufacturer Code"); // Debug
        }
        // If not CGB or no valid manufacturer code, return full title
        return new RomTitle(new String(titleBytes, StandardCharsets.US_ASCII).trim(), "");
    }

    private static boolean isValidManufacturerCode(String code) {
        if (code == null || code.length() != 4) {
            System.out.println("Manufacturer Code null or wrong length: "
                    + (code == null ? "null" : code.length())); // Debug
            return false;
        }

        // Check if the string contains only alphanumeric characters
        for (char c : code.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                System.out.println("Non-alphanumeric character found: " + c); // Debug
                return false;
            }
        }

        // First character must be one of: A, B, H, K, V
        // Last character must be one of: A, B, D, E, F, I, J, K, P, S, U, X, Y
        char first = code.charAt(0);
        char last = code.charAt(3);

        boolean firstValid = "ABHKV".indexOf(first) >= 0;
        boolean lastValid = "ABDEFIJKPSUXY".indexOf(last) >= 0;

        System.out.println(String.format("Code: %s, First: %c (%b), Last: %c (%b)",
                code, first, firstValid, last, lastValid)); // Debug

        return firstValid && lastValid;
    }
}
