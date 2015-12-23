package com.brianledbetter.kwplogger;

import android.os.Parcel;
import android.os.Parcelable;

import com.brianledbetter.kwplogger.KWP2000.DiagnosticTroubleCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by b3d on 12/22/15.
 */
public class ParcelableDTC implements Parcelable {
    public List<DiagnosticTroubleCode> dtcs;
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(dtcs.size());
        for (DiagnosticTroubleCode dtc : dtcs)
        {
            out.writeInt(dtc.codeNumber);
            out.writeInt(dtc.statusCode);
        }
    }

    public static final Parcelable.Creator<ParcelableDTC> CREATOR
            = new Parcelable.Creator<ParcelableDTC>() {
        public ParcelableDTC createFromParcel(Parcel in) {
            return new ParcelableDTC(in);
        }

        public ParcelableDTC[] newArray(int size) {
            return new ParcelableDTC[size];
        }
    };

    private ParcelableDTC(Parcel in) {
        int numberOfValues = in.readInt();
        dtcs = new ArrayList<DiagnosticTroubleCode>(numberOfValues);
        for (int i = 0; i < numberOfValues; i++) {
            DiagnosticTroubleCode dtc = new DiagnosticTroubleCode(in.readInt(), in.readInt());
            dtcs.add(dtc);
        }
    }

    public ParcelableDTC(List<DiagnosticTroubleCode> mv) {
        dtcs = mv;
    }
}
