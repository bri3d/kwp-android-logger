package com.brianledbetter.kwplogger;

import com.brianledbetter.kwplogger.KWP2000.XorSeedKeyLogin;

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
            int seed = 0x0CF4233D;
            int ecuID = 63;
            int key = XorSeedKeyLogin.calculateKey(ecuID,seed);
            assertEquals(key, 0xC1393A1C);
            seed = 0x0332952C;
            key = XorSeedKeyLogin.calculateKey(ecuID,seed);
            assertEquals(key, 0x6652A580);
            seed = 0x10667401;
            key = XorSeedKeyLogin.calculateKey(ecuID,seed);
            assertEquals(key, 0xB3B43B58);
        }
}
