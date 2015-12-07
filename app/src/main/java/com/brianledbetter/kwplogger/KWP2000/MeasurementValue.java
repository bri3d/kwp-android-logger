package com.brianledbetter.kwplogger.KWP2000;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by b3d on 12/6/15.
 */
public class MeasurementValue {
    public String stringValue;
    public String stringLabel;
    public static final Map<Byte, MeasurementValueParser> valueParsers = new HashMap<Byte, MeasurementValueParser>() {
        { // I WANT LAMBDAS
            put((byte)0x1A, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    int temperatureDegreesC = bytes[1] - bytes[0];
                    return new MeasurementValue(Integer.toString(temperatureDegreesC), "degC");
                }
            });
        }
    };

    public static MeasurementValue parseValue(byte[] byteValue) {
        MeasurementValueParser mvp = valueParsers.get(byteValue[0]);
        if (mvp != null) {
            return mvp.parseBytes(Arrays.copyOfRange(byteValue, 1, 3));
        } else {
            return new MeasurementValue("Error", "Error");
        }
    }

    public MeasurementValue(String value, String label) {
        this.stringValue = value;
        this.stringLabel = label;
    }
}
