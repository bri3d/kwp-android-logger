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
            put((byte)0x01, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    int rpm = (bytes[0] * bytes[1]) / 5;
                    return new MeasurementValue(Integer.toString(rpm), "rpm");
                }
            });
            put((byte)0x02, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    int percent = (bytes[0] * bytes[1]) / 500;
                    return new MeasurementValue(Integer.toString(percent), "%");
                }
            });
            put((byte)0x03, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    int deg = (bytes[0] * bytes[1]) / 500;
                    return new MeasurementValue(Integer.toString(deg), "deg");
                }
            });
            put((byte) 0x04, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    String unit;
                    if (bytes[1] > 127) {

                        unit = "deg ATDC";
                    }
                    else {
                        unit = "deg BTDC";
                    }
                    int output = (Math.abs(bytes[1] - 127) * bytes[0])/100;
                    return new MeasurementValue(Integer.toString(output), unit);
                }
            });
            put((byte) 0x05, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    int output = ((bytes[0]*bytes[1])/10) - (bytes[0] * 10);
                    return new MeasurementValue(Integer.toString(output), "deg C");
                }
            });
            put((byte) 0x06, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    int output = (bytes[0]*bytes[1])/1000;
                    return new MeasurementValue(Integer.toString(output), "V");
                }
            });
            put((byte) 0x07, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    int output = (bytes[0]*bytes[1])/100;
                    return new MeasurementValue(Integer.toString(output), "km/h");
                }
            });
            put((byte) 0x08, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    int output = (bytes[0]*bytes[1])/10;
                    return new MeasurementValue(Integer.toString(output), "-");
                }
            });
            put((byte) 0x09, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    int output = ((bytes[1]-127)*bytes[0])/50;
                    return new MeasurementValue(Integer.toString(output), "deg");
                }
            });
            put((byte) 0x0A, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    String output;
                    if(bytes[1] == 0) {
                        output = "COLD";
                    } else {
                        output = "WARM";
                    }
                    return new MeasurementValue(output, "-");
                }
            });
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
            return new MeasurementValue(HexUtil.bytesToHexString(byteValue), "Raw");
        }
    }

    public MeasurementValue(String value, String label) {
        this.stringValue = value;
        this.stringLabel = label;
    }
}
