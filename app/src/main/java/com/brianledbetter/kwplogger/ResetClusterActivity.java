package com.brianledbetter.kwplogger;

import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Toast;

/**
 * Created by b3d on 12/23/15.
 */
public class ResetClusterActivity extends ActionBarActivity implements BluetoothPickerDialogFragment.BluetoothDialogListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_cluster);
    }

    public void startConnection(View v) {
        BluetoothAdapter b = BluetoothAdapter.getDefaultAdapter();

        if (!b.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
        }

        Parcelable[] devices = b.getBondedDevices().toArray(new BluetoothDevice[0]);
        if (devices.length > 0) {
            BluetoothPickerDialogFragment bpdf = new BluetoothPickerDialogFragment();
            bpdf.mPossibleDevices = devices;
            bpdf.show(getFragmentManager(), "BluetoothPickerDialogFragment");
        } else {
            Toast.makeText(getApplicationContext(), "ERROR! " + "No Bluetooth Device available!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String selectedDevice) {
        Intent startIntent = new Intent(this, DiagnosticsService.class);
        startIntent.setAction(DiagnosticsService.RESET_CLUSTER_SERVICE);
        startIntent.putExtra(DiagnosticsService.BLUETOOTH_DEVICE, selectedDevice);
        startService(startIntent);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
    }

}
