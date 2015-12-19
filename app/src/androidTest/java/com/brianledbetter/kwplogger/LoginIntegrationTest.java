package com.brianledbetter.kwplogger;

import com.brianledbetter.kwplogger.KWP2000.DiagnosticSession;
import com.brianledbetter.kwplogger.KWP2000.KWP2000IO;
import com.brianledbetter.kwplogger.KWP2000.KWPException;

import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * Created by b3d on 12/12/15.
 */
public class LoginIntegrationTest extends TestCase {
    private KWP2000IO m_testIO;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ArrayList<byte[]> readBytes = new ArrayList<byte[]>();
        ArrayList<byte[]> writeBytes = new ArrayList<byte[]>();
        writeBytes.add(new byte[]{(byte)0x1A,(byte)0x92});
        writeBytes.add(new byte[]{(byte)0x27,(byte)0x01});
        writeBytes.add(new byte[] {(byte)0x27,(byte)0x02,(byte)0xB1,(byte)0x85,(byte)0xF9,(byte)0x6E});
        readBytes.add(new byte[] {(byte)0x5A,(byte)0x92,(byte)0x30,(byte)0x32,(byte)0x36,(byte)0x31,(byte)0x53});
        readBytes.add(new byte[] {(byte)0x67,(byte)0x01,(byte)0x01,(byte)0xAA,(byte)0x20,(byte)0xC4});
        readBytes.add(new byte[] {(byte)0x67,(byte)0x02});
        m_testIO = new KWP2000TestIO(readBytes, writeBytes);
    }

    public void testLogin() {
        DiagnosticSession testSession = new DiagnosticSession(m_testIO);
        try {
            assertEquals(testSession.securityLogin(), true);
        } catch (KWPException e) {
            fail(e.toString());
        }


    }

}
