package com.brianledbetter.kwplogger.KWP2000;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by b3d on 12/2/15.
 */
public class ELMIO implements KWP2000IO {
    private static final byte ELM_TERMINAL = '>';
    private static final long TIMEOUT = 10000;
    private InputStream m_in;
    private OutputStream m_out;
    private List<String> m_inputLines;

    public ELMIO(InputStream inputStream, OutputStream outputStream) {
        this.m_in = inputStream;
        this.m_out = outputStream;
        this.m_inputLines = new ArrayList<String>();
    }

    @Override
    public void startKWPIO(byte initAddress, byte controllerAddress) throws KWPException {
        try {
            String controllerString = String.format("%02X ", initAddress);
            String addressString = String.format("%02X", controllerAddress);
            writeString("AT Z"); // Reset
            Thread.sleep(250);
            bufferData();
            writeString("AT E0"); // Disable echo
            readOK();
            writeString("AT AL"); // Allow long messages
            readOK();
            writeString("AT SP 0"); // Autodetect protocol
            readOK();
            writeString("AT IIA " + controllerString); // Set KWP initialization address to address
            readOK();
            writeString("AT SH 80 " + addressString + " F1"); // Set ELM header to 80 <address> F1 - this sets up 4-byte header comm between dev F1 (tester) -> dev XX
            readOK();
            Thread.sleep(250);
        } catch (InterruptedException e) {
            throw new KWPException("Initialization timed out!");
        }
    }

    public void readOK() throws KWPException {
        bufferData();
        for (String line : m_inputLines) {
            if (line.startsWith("OK")) {
                m_inputLines.remove(line);
                return;
            }
        }
        Log.d("ELMIO", "Failed to get an OK out of " + m_inputLines.toString());
        throw new KWPException("Failed to get an OK response from ELM!");
    }

    @Override
    public byte[] readBytes() throws KWPException {
        final long start = System.currentTimeMillis();
        bufferData();
        while(true) {
            if (m_inputLines.size() > 0) {
                for(String line : m_inputLines) {
                    // Go hunting for the first valid bytes in the buffer.
                    // This is here because sometimes ELM likes to tell us random human-readable garbage (SEARCHING..., NO DATA, etc.)
                    byte[] candidate = parseByteLine(line);
                    if (candidate.length > 0) {
                        Log.d("ELMIO", "Got bytes from line " + HexUtil.bytesToHexString(candidate));
                        m_inputLines.remove(line);
                        return candidate;
                    }
                }
            }
            if (start + TIMEOUT > System.currentTimeMillis())
                break;
            bufferData();
        }
        Log.d("ELMIO", "Read timed out " + m_inputLines.toString());
        throw new KWPException("Reading data timed out.");
    }

    public boolean writeString(String stringToWrite) {
        try {
            m_out.write((stringToWrite + "\r").getBytes(Charset.forName("US-ASCII")));
            Log.d("ELMIO", "Sent string data " + stringToWrite);
            return true;
        } catch (IOException e) {
            Log.d("ELMIO", "Failed to send " + e.toString());
            return false;
        }
    }

    @Override
    public void writeBytes(byte[] bytesToWrite) throws KWPException {
        String hexString = HexUtil.bytesToHexString(bytesToWrite);
        try {
            m_out.write(hexString.getBytes(Charset.forName("US-ASCII")));
            Log.d("ELMIO", "Sent byte data " + hexString);
            return;
        } catch (IOException e) {
            Log.d("ELMIO", "Failed to send byte data " + e.toString());
            throw new KWPException("Failed to write ELM byte data to stream!");
        }
    }

    private void bufferData() throws KWPException {
        final StringBuilder inputBuffer = new StringBuilder(256);
        final long start = System.currentTimeMillis();
        while (true) {
            if (start + TIMEOUT < System.currentTimeMillis()) {
                break;
            }
            try {
                if (m_in.available() == 0) {
                    Thread.yield();
                    continue;
                }
                byte dataIn = (byte)m_in.read();
                if (dataIn == ELM_TERMINAL) {
                    break;
                }
                inputBuffer.append((char)dataIn);
            } catch (IOException e) {
                Log.d("ELMIO", "Failed to read " + e.getMessage());
                throw new KWPException("Failed to read from buffer!");
            }
        }
        String[] currentLines = inputBuffer.toString().split("\r");
        for (String line : currentLines) {
            if (line.trim().length() > 0) {
                m_inputLines.add(line.trim());
                Log.d("ELMIO", "Added line to output buffer: " + line);
            }
        }
    }

    private byte[] parseByteLine(String byteLine) {
        byteLine = byteLine.replaceAll("\\s", ""); // Remove whitespace
        if (!byteLine.matches("([0-9A-F])+")) { // Check for hexish-ness
            return new byte[0];
        }
        if(byteLine.startsWith("AT")) {
            return new byte[0];
        }
        if(byteLine.startsWith("ELM327")) {
            return new byte[0];
        }
        if(byteLine.startsWith("BUS")) {
            return new byte[0];
        }

        int len = byteLine.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(byteLine.charAt(i), 16) << 4)
                    + Character.digit(byteLine.charAt(i+1), 16));
        }
        return data;
    }
}
