package com.brianledbetter.kwplogger;

import android.app.DialogFragment;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.brianledbetter.kwplogger.KWP2000.DiagnosticTroubleCode;

/**
 * Created by b3d on 12/22/15.
 */
public class DiagnosticCodesActivity extends ListActivity implements BluetoothPickerDialogFragment.BluetoothDialogListener {
    DiagnosticReceiver m_receiver = null;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.diagnostic_codes);
        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setChecked(StateSingleton.getInstance().getIsConnecting());
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!StateSingleton.getInstance().getIsConnecting()) {
                        StateSingleton.getInstance().setIsConnecting(true);
                        startConnection();
                    }
                } else {
                    stopConnection();
                }
            }
        });
        String[] values = new String[] { "Not Connected" };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, values);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(m_receiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceivers();
    }

    private void registerReceivers() {
        IntentFilter dataFilter = new IntentFilter(DiagnosticReceiver.CODES_RESP);
        dataFilter.addCategory(Intent.CATEGORY_DEFAULT);
        IntentFilter failureFilter = new IntentFilter(DiagnosticReceiver.FAILURE_RESP);
        failureFilter.addCategory(Intent.CATEGORY_DEFAULT);
        m_receiver = new DiagnosticReceiver();
        registerReceiver(m_receiver, dataFilter);
        registerReceiver(m_receiver, failureFilter);
    }

    public void startConnection() {
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

    public void stopConnection() {
        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setChecked(false);
        StateSingleton.getInstance().setIsConnected(false);
        StateSingleton.getInstance().setIsConnecting(false);
        Intent stopBluetoothIntent = new Intent(this, DiagnosticsService.class);
        stopBluetoothIntent.setAction(DiagnosticsService.END_DIAGNOSTICS_SERVICE);
        startService(stopBluetoothIntent);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String selectedDevice) {
        Intent startIntent = new Intent(this, DiagnosticsService.class);
        startIntent.setAction(DiagnosticsService.START_DIAGNOSTICS_SERVICE);
        startIntent.putExtra(DiagnosticsService.INIT_ADDRESS, 0x1);
        startIntent.putExtra(DiagnosticsService.REMOTE_ADDRESS, 0x10);
        startIntent.putExtra(DiagnosticsService.BLUETOOTH_DEVICE, selectedDevice);
        startService(startIntent);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setChecked(false);
    }

    public class DiagnosticReceiver extends BroadcastReceiver {
        public static final String CODES_RESP = "com.brianledbetter.kwplogger.CODES";
        public static final String FAILURE_RESP = "com.brianledbetter.kwplogger.FAILURE";
        public static final String ECU_RESP = "com.brianledbetter.kwplogger.ECU_ID";

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ECU_RESP)) {
                StateSingleton.getInstance().setIsConnected(true);
                Intent codesIntent = new Intent(DiagnosticCodesActivity.this, DiagnosticsService.class);
                codesIntent.setAction(DiagnosticsService.READ_CODES_SERVICE);
                startService(codesIntent);
            }
            else if (intent.getAction().equals(CODES_RESP)) {
                StateSingleton.getInstance().setIsConnected(true);
                ParcelableDTC dtcs = intent.getParcelableExtra(DiagnosticsService.CODES_STRING);
                String[] codesStrings = new String[dtcs.dtcs.size()];
                int i = 0;
                for (DiagnosticTroubleCode dtc : dtcs.dtcs) {
                    codesStrings[i] = Integer.toString(dtc.statusCode);
                    i++;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(DiagnosticCodesActivity.this,
                        android.R.layout.simple_list_item_1, codesStrings);
                setListAdapter(adapter);
            } else if(intent.getAction().equals(FAILURE_RESP)) {
                Toast.makeText(getApplicationContext(), "ERROR! " + intent.getStringExtra(DiagnosticsService.ERROR_STRING), Toast.LENGTH_LONG).show();
                stopConnection();
            }
        }
    }

}
