package com.brianledbetter.kwplogger;

import com.brianledbetter.kwplogger.KWP2000.MeasurementValue;
import com.brianledbetter.kwplogger.KWP2000.VAGSeedKeyLogin;

import junit.framework.TestCase;

/**
 * Created by b3d on 12/10/15.
 */
public class LoginTest extends TestCase {
        @Override
        protected void setUp() throws Exception {
            super.setUp();
        }

        public void testLogin() {
            int ecuID = (0x30 + 0x32 + 0x36 + 0x31 + 0x53) & 0x3F;
            int seed = 0x01AA20C4;
            int key = VAGSeedKeyLogin.calculateKey(ecuID, seed);
            assertEquals(key, 0xB185F96E);
        }
}
