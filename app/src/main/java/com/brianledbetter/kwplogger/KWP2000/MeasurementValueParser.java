package com.brianledbetter.kwplogger.KWP2000;

/**
 * Created by b3d on 12/6/15.
 */
public interface MeasurementValueParser {
    public MeasurementValue parseBytes(int[] bytes); // we use int because of wonder, wonderful signed bytes
}
