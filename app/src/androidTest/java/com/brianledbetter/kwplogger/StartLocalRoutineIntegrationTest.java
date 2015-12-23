package com.brianledbetter.kwplogger;

import com.brianledbetter.kwplogger.KWP2000.DiagnosticSession;
import com.brianledbetter.kwplogger.KWP2000.KWP2000IO;
import com.brianledbetter.kwplogger.KWP2000.KWPException;

import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * Created by b3d on 12/23/15.
 */
public class StartLocalRoutineIntegrationTest  extends TestCase {
    private KWP2000IO m_testIO;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ArrayList<byte[]> readBytes = new ArrayList<byte[]>();
        ArrayList<byte[]> writeBytes = new ArrayList<byte[]>();
        writeBytes.add(new byte[]{(byte) 0x31, (byte) 0x1A, (byte) 0x01, (byte) 0x02});
        readBytes.add(new byte[]{(byte) 0x61, (byte) 0x01, (byte) 0x01, (byte) 0xAA, (byte) 0x20, (byte) 0xC4});
        m_testIO = new KWP2000TestIO(readBytes, writeBytes);
    }

    public void testLocalRoutine() {
        DiagnosticSession testSession = new DiagnosticSession(m_testIO);
        try {
            assertEquals(testSession.startLocalRoutine((byte)0x1A, new byte[] {0x1, 0x2}), true);
        } catch (KWPException e) {
            fail(e.toString());
        }


    }
}