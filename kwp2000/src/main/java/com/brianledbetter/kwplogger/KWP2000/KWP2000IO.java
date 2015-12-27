package com.brianledbetter.kwplogger.KWP2000;

/**
 * Created by b3d on 12/2/15.
 */
public interface KWP2000IO {
    void startKWPIO(byte initAddress, byte controllerAddress) throws KWPException;
    void writeBytes(byte[] bytesToWrite) throws KWPException;
    byte[] readBytes() throws KWPException;
}
