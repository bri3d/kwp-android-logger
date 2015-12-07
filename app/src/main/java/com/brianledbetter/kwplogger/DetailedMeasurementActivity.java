package com.brianledbetter.kwplogger;

import android.app.Activity;
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
import android.widget.ToggleButton;

import com.brianledbetter.kwplogger.KWP2000.MeasurementValue;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by b3d on 12/7/15.
 */
public class DetailedMeasurementActivity extends Activity {
    ScheduledExecutorService m_pollMeasurement = Executors.newSingleThreadScheduledExecutor();
    DiagnosticReceiver m_receiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);
        ToggleButton toggle = (ToggleButton) findViewById(R.id.connectionToggle);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startConnection();
                } else {
                    stopConnection();
                }
            }
        });
        NumberPicker numberPicker = (NumberPicker) findViewById(R.id.numberPicker);
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

    public void startConnection() {
        BluetoothAdapter b = BluetoothAdapter.getDefaultAdapter();

        if (!b.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
        }

        Intent startIntent = new Intent(this, DiagnosticsService.class);
        startIntent.setAction(DiagnosticsService.START_DIAGNOSTICS_SERVICE);
        String initAddress = ((EditText)findViewById(R.id.initAddress)).getText().toString();
        String remoteAddress = ((EditText)findViewById(R.id.controllerAddress)).getText().toString();
        startIntent.putExtra(DiagnosticsService.INIT_ADDRESS, Integer.parseInt(initAddress));
        startIntent.putExtra(DiagnosticsService.REMOTE_ADDRESS, Integer.parseInt(remoteAddress));
        startService(startIntent);
    }

    public void stopConnection() {
        m_pollMeasurement.shutdown();
        Intent stopIntent = new Intent(this, DiagnosticsService.class);
        stopIntent.setAction(DiagnosticsService.END_DIAGNOSTICS_SERVICE);
        stopService(stopIntent);
    }

    private void registerReceivers() {
        IntentFilter ecuFilter = new IntentFilter(DiagnosticReceiver.ECU_RESP);
        ecuFilter.addCategory(Intent.CATEGORY_DEFAULT);
        IntentFilter dataFilter = new IntentFilter(DiagnosticReceiver.MEASUREMENT_RESP);
        dataFilter.addCategory(Intent.CATEGORY_DEFAULT);
        m_receiver = new DiagnosticReceiver();
        registerReceiver(m_receiver, ecuFilter);
        registerReceiver(m_receiver, dataFilter);
    }

    private void schedulePolling() {
        m_pollMeasurement.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        Intent startIntent = new Intent(getApplicationContext(), DiagnosticsService.class);
                        startIntent.setAction(DiagnosticsService.POLL_DIAGNOSTICS_SERVICE);
                        NumberPicker numberPicker = (NumberPicker) findViewById(R.id.numberPicker);
                        startIntent.putExtra(DiagnosticsService.MEASUREMENT_GROUP, numberPicker.getValue());
                        startService(startIntent);
                    }
                }, 0, 250, TimeUnit.MILLISECONDS);
    }

    public class DiagnosticReceiver extends BroadcastReceiver {
        public static final String ECU_RESP = "com.brianledbetter.kwplogger.ECU_ID";
        public static final String MEASUREMENT_RESP = "com.brianledbetter.kwplogger.MEASUREMENT";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == ECU_RESP) {
                TextView ecuIDView = (TextView)findViewById(R.id.partNumber);
                ecuIDView.setText(intent.getStringExtra(DiagnosticsService.ECU_ID_STRING));
                schedulePolling();
            } else if(intent.getAction() == MEASUREMENT_RESP) {
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
            }
        }
    }
}
