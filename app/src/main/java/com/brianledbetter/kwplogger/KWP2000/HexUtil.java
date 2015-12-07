package com.brianledbetter.kwplogger.KWP2000;

/**
 * Created by b3d on 12/6/15.
 */
public class HexUtil {
    public static String bytesToHexString(byte[] bytes) {
        // ELM takes ASCII encoded bytes
        StringBuilder sb = new StringBuilder((bytes.length * 3) + 1);
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        sb.append("\r");
        return sb.toString();
    }
}
