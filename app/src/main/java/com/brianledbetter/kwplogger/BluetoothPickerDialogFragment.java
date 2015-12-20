package com.brianledbetter.kwplogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;

/**
 * Created by b3d on 12/19/15.
 */
public class BluetoothPickerDialogFragment extends DialogFragment {
    public String mSelectedDevice;
    public Parcelable[] mPossibleDevices;
    BluetoothDialogListener mListener;

    public interface BluetoothDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog, String selectedDevice);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (BluetoothDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement BluetoothDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mPossibleDevices = savedInstanceState.getParcelableArray("bluetoothDevices");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        CharSequence[] bluetoothDevices = new CharSequence[mPossibleDevices.length];
        for (int i = 0; i < mPossibleDevices.length; i++) {
            bluetoothDevices[i] = ((BluetoothDevice)mPossibleDevices[i]).getName();
        }
        mSelectedDevice = ((BluetoothDevice)mPossibleDevices[0]).getAddress();
        builder.setTitle(R.string.pick_bluetooth)
                .setSingleChoiceItems(bluetoothDevices, 0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                    mSelectedDevice = ((BluetoothDevice)mPossibleDevices[which]).getAddress();
                            }
                        })
                // Set the action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClick(BluetoothPickerDialogFragment.this, mSelectedDevice);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(BluetoothPickerDialogFragment.this);
                    }
                });
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArray("bluetoothDevices", mPossibleDevices);
    }
}
