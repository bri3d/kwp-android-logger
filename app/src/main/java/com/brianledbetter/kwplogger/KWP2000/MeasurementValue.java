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
                    double rpm = (bytes[0] * bytes[1]) / 5.0;
                    return new MeasurementValue(Double.toString(rpm), "rpm");
                }
            });
            put((byte)0x02, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double percent = (bytes[0] * bytes[1]) / 500.0;
                    return new MeasurementValue(Double.toString(percent), "%");
                }
            });
            put((byte)0x03, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double deg = (bytes[0] * bytes[1]) / 500.0;
                    return new MeasurementValue(Double.toString(deg), "deg");
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
                    double output = ((bytes[0]*bytes[1])/10.0) - (bytes[0] * 10.0);
                    return new MeasurementValue(Double.toString(output), "deg C");
                }
            });
            put((byte) 0x06, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double output = (bytes[0]*bytes[1])/1000.0;
                    return new MeasurementValue(Double.toString(output), "V");
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
                    double output = (bytes[0]*bytes[1])/10.0;
                    return new MeasurementValue(Double.toString(output), "-");
                }
            });
            put((byte) 0x09, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double output = ((bytes[1]-127)*bytes[0])/50.0;
                    return new MeasurementValue(Double.toString(output), "deg");
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
            put((byte)0x0B, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = ((bytes[1] - 128) * bytes[0] / 10000.0) + 1;
                    return new MeasurementValue(Double.toString(out), "-");
                }
            });
            put((byte)0x0C, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = (bytes[0] * bytes[1]) / 1000.0;
                    return new MeasurementValue(Double.toString(out), "ohm");
                }
            });
            put((byte)0x0D, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = ((bytes[1] - 127) * bytes[0] / 1000.0);
                    return new MeasurementValue(Double.toString(out), "mm");
                }
            });
            put((byte)0x0E, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = (bytes[0] * bytes[1]) / 200.0;
                    return new MeasurementValue(Double.toString(out), "bar");
                }
            });
            put((byte)0x0F, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = (bytes[0] * bytes[1]) / 100.0;
                    return new MeasurementValue(Double.toString(out), "ms");
                }
            });
            put((byte)0x10, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    int out = bytes[1] & bytes[0];
                    return new MeasurementValue(Integer.toBinaryString(out), "binary");
                }
            });
            put((byte)0x11, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    String out = bytes[0] + " " + bytes[1];
                    return new MeasurementValue(out, "string");
                }
            });
            put((byte)0x12, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = .04 * bytes[0] * bytes[1];
                    return new MeasurementValue(Double.toString(out), "mbar");
                }
            });
            put((byte)0x13, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = bytes[0] * bytes[1] / 100.0;
                    return new MeasurementValue(Double.toString(out), "L");
                }
            });
            put((byte)0x14, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = bytes[0] * (bytes[1] - 128) / 128.0;
                    return new MeasurementValue(Double.toString(out), "%");
                }
            });
            put((byte)0x15, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = bytes[0] * bytes[1] / 1000.0;
                    return new MeasurementValue(Double.toString(out), "V");
                }
            });
            put((byte)0x16, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = bytes[0] * bytes[1] / 1000.0;
                    return new MeasurementValue(Double.toString(out), "ms");
                }
            });
            put((byte)0x17, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = bytes[1] / 256.0 / bytes[0];
                    return new MeasurementValue(Double.toString(out), "%");
                }
            });
            put((byte)0x18, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = bytes[0] * bytes[1] / 1000.0;
                    return new MeasurementValue(Double.toString(out), "A");
                }
            });
            put((byte)0x19, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    double out = (bytes[1] * 256 + bytes[1]) / 182.0;
                    return new MeasurementValue(Double.toString(out), "g/sec");
                }
            });
            put((byte)0x1A, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    int temperatureDegreesC = bytes[1] - bytes[0];
                    return new MeasurementValue(Integer.toString(temperatureDegreesC), "deg C");
                }
            });
            put((byte) 0x1B, new MeasurementValueParser() {
                @Override
                public MeasurementValue parseBytes(byte[] bytes) {
                    String unit;
                    if (bytes[1] > 128) {

                        unit = "deg BTDC";
                    }
                    else {
                        unit = "deg ATDC";
                    }
                    int output = (Math.abs(bytes[1] - 128) * bytes[0])/100;
                    return new MeasurementValue(Integer.toString(output), unit);
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
