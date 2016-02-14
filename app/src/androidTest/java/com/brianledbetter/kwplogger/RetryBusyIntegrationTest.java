package com.brianledbetter.kwplogger;

import com.brianledbetter.kwplogger.KWP2000.DiagnosticSession;
import com.brianledbetter.kwplogger.KWP2000.KWP2000IO;
import com.brianledbetter.kwplogger.KWP2000.KWPException;

import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * Created by b3d on 2/14/16.
 */
public class RetryBusyIntegrationTest extends TestCase {
    private KWP2000IO m_testIO;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ArrayList<byte[]> readBytes = new ArrayList<byte[]>();
        ArrayList<byte[]> writeBytes = new ArrayList<byte[]>();
        writeBytes.add(new byte[]{(byte)0x10,(byte)0x89});
        writeBytes.add(new byte[]{(byte)0x1A,(byte)0x9B});
        readBytes.add(new byte[] {(byte)0x50,(byte)0x89});
        readBytes.add(new byte[] {(byte)0x7F,(byte)0x1A,(byte)0x78});
        readBytes.add(new byte[] {(byte)0x5A,(byte)0x9B,(byte)0x37,(byte)0x4C,(byte)0x35,(byte)0x39,(byte)0x32,(byte)0x30,(byte)0x39,(byte)0x38,(byte)0x30,(byte)0x43,(byte)0x20,(byte)0x20});
        m_testIO = new KWP2000TestIO(readBytes, writeBytes);
    }

    public void testRetry() {
        DiagnosticSession testSession = new DiagnosticSession(m_testIO);
        try {
            testSession.startVWDiagnosticSession();
            assertEquals(testSession.readECUIdentification().hardwareNumber, "7L5920980C");
        } catch (KWPException e) {
            fail(e.toString());
        }
       // 5 A 9 B 37 4 C 35 39 32 30 39 38 30 43 20 20 33 30 31 30 03 00 3 B 64 00 00 00 01 F5 C4 4 B
       // 4F 4D 42 49 49 4E 53 54 52 55 4D 45 4E 54 20 52 42 34 20
    }
}
