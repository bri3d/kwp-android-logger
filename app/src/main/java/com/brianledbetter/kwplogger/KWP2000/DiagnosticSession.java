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

    public boolean stopSession() throws KWPException {
        byte[] byteBuffer = new byte[] {(byte)0x82}; // 0x82 : Log off
        m_IO.writeBytes(byteBuffer);
        byte[] resultingBytes = m_IO.readBytes();
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
        byte[] resultingBytes = m_IO.readBytes();
        if (resultingBytes[0] != (byte)0x7F) // 7F : Negative Response
        {
            m_connected = true;
            return true;
        } else {
            return false;
        }
    }

    public List<MeasurementValue> readIdentifier(int identifierIndex) throws KWPException {
        byte[] byteBuffer = new byte[] { (byte)0x21, (byte) identifierIndex};
        m_IO.writeBytes(byteBuffer);
        return parseIdentifierValues(m_IO.readBytes());
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
        byte[] byteReturn = m_IO.readBytes();
        if (byteReturn[0] == (byte)0x7F) {
            return null;
        }
        else
        {
            ECUIdentification ecuID = new ECUIdentification();
            ecuID.hardwareNumber = new String(Arrays.copyOfRange(byteReturn, 2, 12));
            ecuID.softwareNumber = new String(Arrays.copyOfRange(byteReturn, 13, byteReturn.length));
            return ecuID;
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
        byte[] resultingBytes = m_IO.readBytes();
        return (resultingBytes[0] == (byte)0x6C); // 6C : Positive response status
    }
}
