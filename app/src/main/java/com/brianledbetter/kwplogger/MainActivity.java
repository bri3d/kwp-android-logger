package com.brianledbetter.kwplogger;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends ActionBarActivity {
    ScheduledExecutorService m_pollTemperature = Executors.newSingleThreadScheduledExecutor();
    DiagnosticReceiver m_receiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ToggleButton toggle = (ToggleButton) findViewById(R.id.connectionToggle);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    stopConnection();
                } else {
                    startConnection();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startConnection() {
        BluetoothAdapter b = BluetoothAdapter.getDefaultAdapter();

        if (!b.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
        }

        Intent startIntent = new Intent(this, DiagnosticsService.class);
        startIntent.setAction(DiagnosticsService.START_DIAGNOSTICS_SERVICE);
        startIntent.putExtra(DiagnosticsService.INIT_ADDRESS, 0x2);
        startIntent.putExtra(DiagnosticsService.REMOTE_ADDRESS, 0x1A);
        startService(startIntent);
    }

    public void stopConnection() {
        m_pollTemperature.shutdown();
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
        m_pollTemperature.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        Intent startIntent = new Intent(getApplicationContext(), DiagnosticsService.class);
                        startIntent.setAction(DiagnosticsService.POLL_DIAGNOSTICS_SERVICE);
                        startIntent.putExtra(DiagnosticsService.MEASUREMENT_GROUP, 0x6);
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
                TextView valueView = (TextView)findViewById(R.id.valueValue);
                ParcelableMeasurementValues values = intent.getParcelableExtra(DiagnosticsService.VALUE_STRING);
                valueView.setText(values.measurementValues.get(0).stringValue);
                valueView = (TextView)findViewById(R.id.valueLabel);
                valueView.setText(values.measurementValues.get(0).stringLabel);
            }
        }
    }
}
