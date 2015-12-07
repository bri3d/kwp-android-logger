package com.brianledbetter.kwplogger;

import android.os.Parcel;
import android.os.Parcelable;

import com.brianledbetter.kwplogger.KWP2000.MeasurementValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by b3d on 12/7/15.
 */

public class ParcelableMeasurementValues implements Parcelable {
    public List<MeasurementValue> measurementValues;
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(measurementValues.size());
        for (MeasurementValue mv : measurementValues)
        {
            out.writeString(mv.stringValue);
            out.writeString(mv.stringLabel);
        }
    }

    public static final Parcelable.Creator<ParcelableMeasurementValues> CREATOR
            = new Parcelable.Creator<ParcelableMeasurementValues>() {
        public ParcelableMeasurementValues createFromParcel(Parcel in) {
            return new ParcelableMeasurementValues(in);
        }

        public ParcelableMeasurementValues[] newArray(int size) {
            return new ParcelableMeasurementValues[size];
        }
    };

    private ParcelableMeasurementValues(Parcel in) {
        int numberOfValues = in.readInt();
        measurementValues = new ArrayList<MeasurementValue>(numberOfValues);
        for (int i = 0; i < numberOfValues; i++) {
            MeasurementValue mv = new MeasurementValue(in.readString(), in.readString());
            measurementValues.add(mv);
        }
    }

    public ParcelableMeasurementValues(List<MeasurementValue> mv) {
        measurementValues = mv;
    }
}
