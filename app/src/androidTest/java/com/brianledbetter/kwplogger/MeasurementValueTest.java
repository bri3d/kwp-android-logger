package com.brianledbetter.kwplogger;

import com.brianledbetter.kwplogger.KWP2000.MeasurementValue;

import junit.framework.TestCase;

/**
 * Created by b3d on 12/7/15.
 */
public class MeasurementValueTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void test1ATemperature() {
        MeasurementValue testResult = MeasurementValue.parseValue(new byte[] { 0x1A, 0x32, 0x54 });
        assertEquals(testResult.stringLabel, "deg C");
        assertEquals(testResult.stringValue, "34");
    }

    public void test02Percent() {
        MeasurementValue testResult = MeasurementValue.parseValue(new byte[] { 0x02, -22, 0x01 });
        assertEquals(testResult.stringLabel, "%");
        assertEquals(testResult.stringValue, "0.468");
    }
}
