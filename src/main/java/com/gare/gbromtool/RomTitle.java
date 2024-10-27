package com.gare.gbromtool;

/**
 * This is a helper class for splitting the title and manufacturer code when
 * the latter exists.
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
}
