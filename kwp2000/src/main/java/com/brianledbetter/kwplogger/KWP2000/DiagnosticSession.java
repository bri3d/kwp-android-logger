package com.brianledbetter.kwplogger.KWP2000;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by b3d on 12/2/15.
 */
public class DiagnosticSession {
    private boolean m_connected = false;
    private KWP2000IO m_IO;

    public DiagnosticSession(KWP2000IO kwpInterface) {
        this.m_IO = kwpInterface;
    }

    public boolean startVWDiagnosticSession() throws KWPException {
        return startSession((byte)0x89); // 0x89 : VW Diagnostic Session
    }

    public int getSeedEcuID() throws KWPException {
        byte[] byteBuffer = new byte[]{(byte) 0x1A, (byte) 0x92}; // SystemSupplierSpecific ECUID request to get vendor partno
        m_IO.writeBytes(byteBuffer);
        byte[] resultingBytes = readBytes();
        if (resultingBytes[0] != (byte)0x7F) // 7F : Negative Response
        {
            byte[] ecuID = Arrays.copyOfRange(resultingBytes, 2, 7);
            int res = 0;
            for (int i = 0; i < 5; i++) {
                res += ecuID[i] & 0xFF;
            }
            res = res & 0x3F;
            return res;
        } else {
            throw new KWPException("Failed to read vendor part number : " + HexUtil.bytesToHexString(resultingBytes));
        }
    }

    public int getSecurityAccessSeed() throws KWPException {
        byte[] byteBuffer = new byte[]{(byte) 0x27, (byte) 0x01}; // 0x27 0x01 : SecurityAccess Get Seed
        m_IO.writeBytes(byteBuffer);
        byte[] resultingBytes = readBytes();
        if (resultingBytes[0] != (byte)0x7F) // 7F : Negative Response
        {
            byte[] seedBytes = Arrays.copyOfRange(resultingBytes, 2, 6);
            return HexUtil.bytesToInt(seedBytes);
        } else {
            throw new KWPException("Failed to read seed : " + HexUtil.bytesToHexString(resultingBytes));
        }
    }

    public boolean loginWithKey(int key) throws KWPException {
        // 0x27 0x02 : SecurityAccess Log In
        byte[] byteBuffer = new byte[] {(byte) 0x27, (byte) 0x02, (byte)((key >> 24 ) & 0xff), (byte)((key >> 16 ) & 0xff), (byte)((key >> 8 ) & 0xff), (byte)(key & 0xff)};
        m_IO.writeBytes(byteBuffer);
        byte[] resultingBytes = readBytes();
        if (resultingBytes[0] != (byte)0x7F) // 7F : Negative Response
        {
           return true;
        } else {
            throw new KWPException("Failed to log in : " + HexUtil.bytesToHexString(resultingBytes));
        }
    }

    public boolean stopSession() throws KWPException {
        byte[] byteBuffer = new byte[] {(byte)0x82}; // 0x82 : Log off
        m_IO.writeBytes(byteBuffer);
        byte[] resultingBytes = readBytes();
        if (resultingBytes[0] != (byte)0x7F) // 7F : Negative Response
        {
            m_connected = false;
            return true;
        } else {
            return false;
        }
    }

    public boolean startSession(byte sessionKind) throws KWPException {
        byte[] byteBuffer = new byte[] {(byte)0x10, sessionKind}; // 0x10 : Initialize Session
        m_IO.writeBytes(byteBuffer);
        byte[] resultingBytes = readBytes();
        if (resultingBytes[0] != (byte)0x7F) // 7F : Negative Response
        {
            m_connected = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean securityLogin() throws KWPException {
        int ecuID = getSeedEcuID();
        int seed = getSecurityAccessSeed();
        int key = VAGSeedKeyLogin.calculateKey(ecuID, seed);
        return loginWithKey(key);
    }

    public List<MeasurementValue> readIdentifier(int identifierIndex) throws KWPException {
        byte[] byteBuffer = new byte[] { (byte)0x21, (byte) identifierIndex};
        m_IO.writeBytes(byteBuffer);
        return parseIdentifierValues(readBytes());
    }

    private List<MeasurementValue> parseIdentifierValues(byte[] identifierData) {
        List<MeasurementValue> measuredValues = new ArrayList<MeasurementValue>();
        if(!(identifierData[0] == (byte) 0x7F)) { // Do not run on a negative response
            for(int i = 2; i < identifierData.length - 3; i+= 3) { // Returned values should be 3 bytes
                byte[] blockByteValue = Arrays.copyOfRange(identifierData, i, i + 3);
                measuredValues.add(MeasurementValue.parseValue(blockByteValue));
            }
        }
        return measuredValues;
    }

    public ECUIdentification readECUIdentification() throws KWPException {
        byte[] byteBuffer = new byte[] { (byte)0x1A, (byte) 0x9B };
        m_IO.writeBytes(byteBuffer);
        byte[] byteReturn = readBytes();
        if (byteReturn[0] == (byte)0x7F) {
            return null;
        }

        ECUIdentification ecuID = new ECUIdentification();
        ecuID.hardwareNumber = new String(Arrays.copyOfRange(byteReturn, 2, 12));
        ecuID.softwareNumber = new String(Arrays.copyOfRange(byteReturn, 13, byteReturn.length));
        return ecuID;
    }

    public byte[] readMemoryByAddress(int address, int bytes) throws KWPException
    {
        byte[] byteBuffer = new byte[] {(byte)0x23, (byte)((address >> 16 ) & 0xff), (byte)((address >> 8 ) & 0xff), (byte)((address) & 0xff), (byte)bytes}; // 0x23 : ReadMemoryAtAddress
        m_IO.writeBytes(byteBuffer);
        byte[] resultingBytes = readBytes();
        if (resultingBytes[0] != (byte)0x7F) // 7F : Negative Response
        {
           return Arrays.copyOfRange(resultingBytes, 2, 2+bytes);
        } else {
            throw new KWPException("Failed to read memory : " + HexUtil.bytesToHexString(resultingBytes));
        }
    }

    public List<DiagnosticTroubleCode> readDTCs() throws KWPException {
        byte[] byteBuffer = new byte[] {(byte) 0x18, (byte) 0x00, (byte) 0xff, (byte) 0x00};
        m_IO.writeBytes(byteBuffer);
        byte[] resultingBytes = readBytes();
        if (resultingBytes[0] != (byte)0x7F) // 7F : Negative Response
        {
            return parseDTCs(resultingBytes);
        } else {
            throw new KWPException("Failed to read DTCs " + HexUtil.bytesToHexString(resultingBytes));
        }
    }

    private List<DiagnosticTroubleCode> parseDTCs(byte[] resultBytes) {
        List<DiagnosticTroubleCode> storedDTCs = new ArrayList<DiagnosticTroubleCode>();
        if(!(resultBytes[0] == (byte) 0x7F)) { // Do not run on a negative response
            for(int i = 2; i < resultBytes.length - 3; i+= 3) { // Returned values should be 3 bytes
                byte[] blockByteValue = Arrays.copyOfRange(resultBytes, i, i + 3);
                storedDTCs.add(DiagnosticTroubleCode.parseDTC(blockByteValue));
            }
        }
        return storedDTCs;
    }

    public boolean clearDTCs() throws KWPException {
        byte[] byteBuffer = new byte[] {(byte) 0x14, (byte) 0xff, (byte) 0x00};
        m_IO.writeBytes(byteBuffer);
        byte[] resultingBytes = readBytes();
        if (resultingBytes[0] != (byte)0x7F) // 7F : Negative Response
        {
            return true;
        } else {
            throw new KWPException("Failed to clear DTCs " + HexUtil.bytesToHexString(resultingBytes));
        }
    }

    public boolean writeDynamicallyDefinedIdentifier(int numberOfRecords, int ddliNumber, byte[] byteLengths, int[] byteAddresses) throws KWPException {
        if(!m_connected || numberOfRecords > 20 || ddliNumber > 15 || byteAddresses.length > numberOfRecords)
            return false;
        // 6 bytes per record, 2 bytes to set up
        byte[] byteBuffer = new byte[(numberOfRecords * 6) + 2];
        byteBuffer[0] = (byte)0x0C; // 0x0C : KWP2000 DynamicallyDefineLocalIdentifier service
        byteBuffer[1] = (byte)(0xF0 + ddliNumber); // 0xF0 + index : DDLI Identifier
        for(int i = 0; i < numberOfRecords; i++) {
            byteBuffer[(i * 6) + 0] = 0x03; // DDLI type 0x3 : Define by Memory Address
            byteBuffer[(i * 6) + 1] = (byte)i; // Position in DDLI table
            byteBuffer[(i * 6) + 2] = byteLengths[i]; // Size of memory to read
            byteBuffer[(i * 6) + 3] = (byte)((byteAddresses[i] >> 16) & 0xFF); // High byte
            byteBuffer[(i * 6) + 4] = (byte)((byteAddresses[i] >> 8) & 0xFF); // Middle byte
            byteBuffer[(i * 6) + 5] = (byte)((byteAddresses[i]) & 0xFF); // Low byte
        }
        m_IO.writeBytes(byteBuffer);
        byte[] resultingBytes = readBytes();
        return (resultingBytes[0] == (byte)0x6C); // 6C : Positive response status
    }

    public boolean startLocalRoutine(byte localRoutine, byte[] params) throws KWPException {
        byte[] byteBuffer = new byte[params.length + 2];
        byteBuffer[0] = (byte)0x31; // KWP2000 startLocalRoutineByIdentifier
        byteBuffer[1] = localRoutine;
        System.arraycopy(params, 0, byteBuffer, 2, params.length);
        m_IO.writeBytes(byteBuffer);
        return(readBytes()[0] != (byte)0x7F);
    }

    public boolean sendTesterPresent() throws KWPException {
        byte[] byteBuffer = new byte[] { (byte)0x3E };
        m_IO.writeBytes(byteBuffer);
        return(readBytes()[0] != (byte)0x7F);
    }

    public boolean clearCayenneClusterServiceIndicator() throws KWPException {
        byte[] startB8Routine = new byte[] {(byte)0x1,(byte)0x3};
        byte[] startBARoutine = new byte[] {(byte)0x1,(byte)0x3};
        byte[] startB9Routine = new byte[] {(byte)0x1,(byte)0x3,(byte)0x2};
        byte[] startB9Routine2 = new byte[] {(byte)0x1,(byte)0x3,(byte)0x0,(byte)0x0};
        byte[] startBBRoutine = new byte[] {(byte)0x1,(byte)0x3,(byte)0x0,(byte)0x0,(byte)0x0,(byte)0x0,(byte)0x4,(byte)0x50,(byte)0x10,(byte)0x34};
        boolean flag = true;
        // This procedure writes 0x0 into adaptation channel 0x2, which appears to reset?
        flag &= startLocalRoutine((byte)0xB8, startB8Routine); // Start Adaptation
        flag &= startLocalRoutine((byte)0xBA, startBARoutine); // Read
        flag &= startLocalRoutine((byte)0xB9, startB9Routine); // Select channel 2
        flag &= startLocalRoutine((byte)0xBA, startBARoutine); // Read
        flag &= startLocalRoutine((byte)0xB9, startB9Routine2); // Test data 00 00
        flag &= startLocalRoutine((byte)0xBA, startBARoutine); // Read
        flag &= startLocalRoutine((byte)0xBB, startBBRoutine); // Save data - 2 bytes are data, last 6 are derived from return of 0x1A 0x9B ident, which is static for this module so hardcoded
        flag &= startLocalRoutine((byte)0xBA, startBARoutine); // Read
        flag &= startLocalRoutine((byte)0xB8, startB8Routine); // I think we should actually be calling stopLocalRoutine here...
        if(!flag) return false;
        return true;
    }

    private byte[] readBytes() throws KWPException {
        byte[] responseBytes = m_IO.readBytes();
        if ((responseBytes.length > 2) && (responseBytes[0] == (byte) 0x7F) && (responseBytes[2] == (byte) 0x78)) {
            // Control unit said it is busy (7F 78 response) but WILL respond later, so let it do so.
            return this.readBytes();
        } else {
            return responseBytes;
        }
    }
}
