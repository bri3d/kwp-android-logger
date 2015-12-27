package com.brianledbetter.kwplogger.KWP2000;

/**
 * Created by b3d on 12/22/15.
 */
public class DiagnosticTroubleCode {
    public int codeNumber;
    public int statusCode;
    public boolean aboveThreshold;
    public boolean belowThreshold;
    public boolean invalidInput;
    public boolean noInput;
    public boolean noReason;
    public boolean complete;
    public boolean wasPresent;
    public boolean isPresent;
    public boolean isCEL;

    public static DiagnosticTroubleCode parseDTC(byte[] bytes) {
        int codeNumber = ((int)bytes[0] & 0xFF) << 8 + (int)bytes[1] & 0xFF;
        int dtcStatus = (int)bytes[2] & 0xFF;
        return new DiagnosticTroubleCode(codeNumber, dtcStatus);
    }

    public DiagnosticTroubleCode(int codeNumber, int dtcStatus) {
        this.codeNumber = codeNumber;
        this.statusCode = dtcStatus;
        this.aboveThreshold = (dtcStatus & 0x1) != 0; // 0b00000001
        this.belowThreshold = (dtcStatus & 0x2) != 0; // 0b00000010
        this.noInput = ((dtcStatus & 0x4) != 0); // 0b00000100
        this.invalidInput = ((dtcStatus & 0x8) != 0); // 0b00001000
        this.noReason = ((dtcStatus & 0xF) == 0); // NOT 0b00001111 - that is, 0x0bxxxx0000
        this.complete = ((dtcStatus & 0x10) != 0); // 0b00010000
        this.wasPresent = ((dtcStatus & 0x20) != 0); // 0b00100000
        this.isPresent = ((dtcStatus & 0x40) != 0); // 0b01000000
        this.isCEL = ((dtcStatus & 0x80) != 0); // 0b10000000
    }


}
