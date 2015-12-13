package com.brianledbetter.kwplogger;

import com.brianledbetter.kwplogger.KWP2000.HexUtil;
import com.brianledbetter.kwplogger.KWP2000.KWP2000IO;
import com.brianledbetter.kwplogger.KWP2000.KWPException;

import java.util.Arrays;
import java.util.List;

/**
 * Created by b3d on 12/12/15.
 */
public class KWP2000TestIO implements KWP2000IO {
    private List<byte[]> m_readBytes;
    private List<byte[]> m_expectedWriteBytes;

    public KWP2000TestIO(List<byte[]> readBytes, List<byte[]> expectedWriteBytes) {
        m_readBytes = readBytes;
        m_expectedWriteBytes = expectedWriteBytes;
    }

    @Override
    public byte[] readBytes() throws KWPException {
        if (m_readBytes.size() == 0) {
            throw new KWPException("Trying to read bytes but none available!");
        }
        return m_readBytes.remove(0);
    }

    @Override
    public void writeBytes(byte[] bytesToWrite) throws KWPException {
        if(m_expectedWriteBytes.size() == 0) {
            throw new KWPException("Wrote more bytes than expected!");
        }
        byte[] expectedBytes = m_expectedWriteBytes.remove(0);
        if (!Arrays.equals(expectedBytes, bytesToWrite)) {
            throw new KWPException("Expected we were going to write " + HexUtil.bytesToHexString(expectedBytes) + " but wrote " + HexUtil.bytesToHexString(bytesToWrite));
        }
    }

    @Override
    public void startKWPIO(byte initAddress, byte controllerAddress) throws KWPException {

    }


}
