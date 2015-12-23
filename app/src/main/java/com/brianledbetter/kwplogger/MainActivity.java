package com.brianledbetter.kwplogger;

import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends ActionBarActivity implements BluetoothPickerDialogFragment.BluetoothDialogListener {
    DiagnosticReceiver m_receiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ToggleButton toggle = (ToggleButton) findViewById(R.id.connectionToggle);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, DetailedMeasurementActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_dtcs) {
            Intent intent = new Intent(this, DiagnosticCodesActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_reset) {
            Intent intent = new Intent(this, ResetClusterActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String selectedDevice) {
        Intent startIntent = new Intent(this, DiagnosticsService.class);
        startIntent.setAction(DiagnosticsService.START_DIAGNOSTICS_SERVICE);
        startIntent.putExtra(DiagnosticsService.INIT_ADDRESS, 0x2);
        startIntent.putExtra(DiagnosticsService.REMOTE_ADDRESS, 0x1A);
        startIntent.putExtra(DiagnosticsService.BLUETOOTH_DEVICE, selectedDevice);
        startService(startIntent);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        ToggleButton toggle = (ToggleButton) findViewById(R.id.connectionToggle);
        toggle.setChecked(false);
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
        ToggleButton toggle = (ToggleButton) findViewById(R.id.connectionToggle);
        toggle.setChecked(false);
        StateSingleton.getInstance().setIsConnected(false);
        StateSingleton.getInstance().setIsConnecting(false);
        Intent stopIntent = new Intent(this, PollingService.class);
        stopIntent.setAction(PollingService.STOP_POLL_DIAGNOSTICS_SERVICE);
        startService(stopIntent);
        Intent stopBluetoothIntent = new Intent(this, DiagnosticsService.class);
        stopBluetoothIntent.setAction(DiagnosticsService.END_DIAGNOSTICS_SERVICE);
        startService(stopBluetoothIntent);
    }

    private void registerReceivers() {
        IntentFilter ecuFilter = new IntentFilter(DiagnosticReceiver.ECU_RESP);
        ecuFilter.addCategory(Intent.CATEGORY_DEFAULT);
        IntentFilter dataFilter = new IntentFilter(DiagnosticReceiver.MEASUREMENT_RESP);
        dataFilter.addCategory(Intent.CATEGORY_DEFAULT);
        IntentFilter failureFilter = new IntentFilter(DiagnosticReceiver.FAILURE_RESP);
        failureFilter.addCategory(Intent.CATEGORY_DEFAULT);
        m_receiver = new DiagnosticReceiver();
        registerReceiver(m_receiver, ecuFilter);
        registerReceiver(m_receiver, dataFilter);
        registerReceiver(m_receiver, failureFilter);
    }

    private void schedulePolling() {
        if (!StateSingleton.getInstance().getIsConnected()) return;
        Intent startIntent = new Intent(getApplicationContext(), PollingService.class);
        startIntent.setAction(PollingService.START_POLL_DIAGNOSTICS_SERVICE);
        startIntent.putExtra(PollingService.MEASUREMENT_GROUP, 0x6);
        startService(startIntent);
    }

    public class DiagnosticReceiver extends BroadcastReceiver {
        public static final String ECU_RESP = "com.brianledbetter.kwplogger.ECU_ID";
        public static final String MEASUREMENT_RESP = "com.brianledbetter.kwplogger.MEASUREMENT";
        public static final String FAILURE_RESP = "com.brianledbetter.kwplogger.FAILURE";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ECU_RESP)) {
                TextView ecuIDView = (TextView)findViewById(R.id.partNumber);
                ecuIDView.setText(intent.getStringExtra(DiagnosticsService.ECU_ID_STRING));
                StateSingleton.getInstance().setIsConnected(true);
                schedulePolling();
            } else if(intent.getAction().equals(MEASUREMENT_RESP)) {
                TextView valueView = (TextView)findViewById(R.id.valueValue);
                ParcelableMeasurementValues values = intent.getParcelableExtra(DiagnosticsService.VALUE_STRING);
                valueView.setText(values.measurementValues.get(0).stringValue);
                valueView = (TextView)findViewById(R.id.valueLabel);
                valueView.setText(values.measurementValues.get(0).stringLabel);
            } else if(intent.getAction().equals(FAILURE_RESP)) {
                Toast.makeText(getApplicationContext(), "ERROR! " + intent.getStringExtra(DiagnosticsService.ERROR_STRING), Toast.LENGTH_LONG).show();
                stopConnection();
            }
        }
    }
}
