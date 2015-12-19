package com.brianledbetter.kwplogger;

import android.app.Activity;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.brianledbetter.kwplogger.KWP2000.MeasurementValue;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by b3d on 12/7/15.
 */
public class DetailedMeasurementActivity extends Activity implements BluetoothPickerDialogFragment.BluetoothDialogListener {
    private DiagnosticReceiver m_receiver = null;
    private int m_selectedMeasurementGroup = 1;
    private boolean m_isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);
        ToggleButton toggle = (ToggleButton) findViewById(R.id.connectionToggle);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!m_isConnected) {
                        startConnection();
                    }
                } else {
                    stopConnection();
                }
            }
        });
        NumberPicker numberPicker = (NumberPicker) findViewById(R.id.numberPicker);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                m_selectedMeasurementGroup = numberPicker.getValue();
                schedulePolling();
            }
        });
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(255);
        numberPicker.setValue(1);
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
        return false;
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String selectedDevice) {
        Intent startIntent = new Intent(this, DiagnosticsService.class);
        startIntent.setAction(DiagnosticsService.START_DIAGNOSTICS_SERVICE);
        String initAddress = ((EditText)findViewById(R.id.initAddress)).getText().toString();
        String remoteAddress = ((EditText)findViewById(R.id.controllerAddress)).getText().toString();
        startIntent.putExtra(DiagnosticsService.INIT_ADDRESS, Integer.parseInt(initAddress));
        startIntent.putExtra(DiagnosticsService.REMOTE_ADDRESS, Integer.parseInt(remoteAddress));
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

        Object[] devices = b.getBondedDevices().toArray();

        BluetoothPickerDialogFragment bpdf = new BluetoothPickerDialogFragment();
        bpdf.mPossibleDevices = devices;
        bpdf.show(getFragmentManager(), "BluetoothPickerDialogFragment");
    }

    public void stopConnection() {
        m_isConnected = false;
        Intent stopIntent = new Intent(this, PollingService.class);
        stopIntent.setAction(PollingService.STOP_POLL_DIAGNOSTICS_SERVICE);
        stopService(stopIntent);
        Intent stopBluetoothIntent = new Intent(this, DiagnosticsService.class);
        stopBluetoothIntent.setAction(DiagnosticsService.END_DIAGNOSTICS_SERVICE);
        stopService(stopBluetoothIntent);
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
        if (!m_isConnected) return;
        Intent startIntent = new Intent(getApplicationContext(), PollingService.class);
        startIntent.setAction(PollingService.START_POLL_DIAGNOSTICS_SERVICE);
        startIntent.putExtra(PollingService.MEASUREMENT_GROUP, m_selectedMeasurementGroup);
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
                m_isConnected = true;
                schedulePolling();
            } else if(intent.getAction().equals(MEASUREMENT_RESP)) {
                ParcelableMeasurementValues values = intent.getParcelableExtra(DiagnosticsService.VALUE_STRING);
                for (int i = 0; i < values.measurementValues.size(); i++) {
                    TextView valueLabel = (TextView)findViewById(getResources().getIdentifier("value" + (i + 1), "id", getPackageName()));
                    TextView labelLabel = (TextView)findViewById(getResources().getIdentifier("label" + (i + 1), "id", getPackageName()));
                    MeasurementValue value = values.measurementValues.get(i);
                    if (valueLabel != null) // Guards against too many measured values coming back.
                        valueLabel.setText(value.stringValue);
                    if (labelLabel != null)
                        labelLabel.setText(value.stringLabel);
                }
            } else if(intent.getAction().equals(FAILURE_RESP)) {
                Toast.makeText(getApplicationContext(), "ERROR! " + intent.getStringExtra(DiagnosticsService.ERROR_STRING), Toast.LENGTH_LONG).show();
                stopConnection();
            }
        }
    }
}
