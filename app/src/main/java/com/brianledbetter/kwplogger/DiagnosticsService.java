package com.brianledbetter.kwplogger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.brianledbetter.kwplogger.KWP2000.DiagnosticSession;
import com.brianledbetter.kwplogger.KWP2000.ECUIdentification;
import com.brianledbetter.kwplogger.KWP2000.ELMIO;
import com.brianledbetter.kwplogger.KWP2000.KWP2000IO;
import com.brianledbetter.kwplogger.KWP2000.KWPException;
import com.brianledbetter.kwplogger.KWP2000.MeasurementValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by b3d on 12/6/15.
 */
public class DiagnosticsService extends PermanentService {
    public static final String START_DIAGNOSTICS_SERVICE = "com.brianledbetter.kwplogger.StartService";
    public static final String POLL_DIAGNOSTICS_SERVICE = "com.brianledbetter.kwplogger.PollService";
    public static final String END_DIAGNOSTICS_SERVICE = "com.brianledbetter.kwplogger.EndService";

    public static final String ECU_ID_STRING = "ecuID";
    public static final String VALUE_STRING = "value";

    public static final String MEASUREMENT_GROUP = "measurementGroup";
    public static final String INIT_ADDRESS = "initAddress";
    public static final String REMOTE_ADDRESS = "remoteAddress";

    // this is the "well known" UUID for Bluetooth SPP (Serial Profile).
    private static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothSocket m_btSocket = null;
    private DiagnosticSession m_kwp = null;
    private KWP2000IO m_ELMKWP = null;
    private boolean m_isConnected = false;

    public DiagnosticsService() {
        super("com.brianledbetter.kwplogger.KWP2000Service");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        if (intent.getAction().equals(START_DIAGNOSTICS_SERVICE)) {
            int initAddress = intent.getIntExtra(INIT_ADDRESS, 0x1);
            int remoteAddress = intent.getIntExtra(REMOTE_ADDRESS, 0x10);
            Log.d("KWP", "Starting connection!");
            startConnection(initAddress, remoteAddress);
        }
        if (intent.getAction().equals(POLL_DIAGNOSTICS_SERVICE)) {
            int measurementGroup = intent.getIntExtra(MEASUREMENT_GROUP, 0x1);
            Log.d("KWP", "Polling... " + measurementGroup);
            pollData(measurementGroup);
        }
        if (intent.getAction().equals(END_DIAGNOSTICS_SERVICE)) {
            Log.d("KWP", "Ending connection...");
            endConnection();
        }
    }

    private void endConnection() {
        try {
            m_kwp.stopSession();
            m_btSocket.close();
            m_isConnected = false;
            stopSelf();
        } catch (KWPException e) {
            Log.d("KWP", "Failed to close connection!");
        } catch (IOException e) { // Got to love early 2000s Java, thanks Android
            Log.d("KWP", "I/O error closing connection!");
        } catch (NullPointerException e) {
            Log.d("KWP", "Connection was not fully established before being ended!");
        }
    }

    private void startConnection(int initAddress, int remoteAddress) {
        try {
            if(!connectBluetooth(initAddress, remoteAddress)) return;
            connectKWP2000();
            ECUIdentification ecuID = m_kwp.readECUIdentification();
            Log.d("KWP", "Got string " + ecuID.hardwareNumber + " for hardware number");
            m_isConnected = true;
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MainActivity.DiagnosticReceiver.ECU_RESP);
            broadcastIntent.putExtra(ECU_ID_STRING, ecuID.hardwareNumber);
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            sendBroadcast(broadcastIntent);
        } catch (KWPException e) {
            endConnection();
            Toast.makeText(getApplicationContext(), "ERROR. Is the key on? " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean connectBluetooth(int initAddress, int remoteAddress) {
        BluetoothAdapter b = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = b.getBondedDevices();
        if(pairedDevices.size() == 0)
        {
            Log.d("KWP", "No Candidate Device");
            return false;
        }
        BluetoothDevice firstBluetoothDevice = (BluetoothDevice)pairedDevices.toArray()[0];
        try {
            m_btSocket = firstBluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
            Log.d("KWP", "Created RFComm Service");
        } catch (IOException e) {
            Log.d("KWP", "RFComm Creation Failed");
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
        b.cancelDiscovery();
        try {
            m_btSocket.connect();
            Log.d("KWP", "Serial port opened!");
            OutputStream outStream = m_btSocket.getOutputStream();
            InputStream inStream = m_btSocket.getInputStream();
            m_ELMKWP = new ELMIO(inStream, outStream);
        } catch (IOException e) {
            Log.d("KWP", "RFComm Connection Failed");
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            try {
                m_btSocket.close();
                return false;
            } catch (IOException e2) { // J A V A
                Toast.makeText(getApplicationContext(), e2.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        try {
            m_ELMKWP.startKWPIO((byte)initAddress, (byte)remoteAddress);
        } catch (KWPException e) {
            Log.d("KWP", "Connection failed!");
            return false;
        }
        Log.d("KWP", "KWP Connection Succeeded");
        return true;
    }

    private void connectKWP2000() throws KWPException {
        if(!m_btSocket.isConnected()) {
            Log.d("KWP", "Trying to connect to a closed socket!");
            throw new KWPException("Trying to connect to a closed Bluetooth device!");
        }
        m_kwp = new DiagnosticSession(m_ELMKWP);
        m_kwp.startVWDiagnosticSession();
    }

    private void pollData(int measurementIdentifier) {
        if(!m_isConnected) {
            return;
        }
        try {
            List<MeasurementValue> measurementValues = m_kwp.readIdentifier(measurementIdentifier);
            if (measurementValues.size() > 0) {
                Log.d("KWP", "Got values : ");
                for (int i = 0; i < measurementValues.size(); i++)
                {
                    Log.d("KWP", "Value " + i + " : " + measurementValues.get(i).stringValue + " " + measurementValues.get(i).stringLabel + " for identifier " + measurementIdentifier);
                }
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MainActivity.DiagnosticReceiver.MEASUREMENT_RESP);
                broadcastIntent.putExtra(VALUE_STRING, new ParcelableMeasurementValues(measurementValues));
                broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                sendBroadcast(broadcastIntent);
            }
        }
        catch (KWPException e)
        {
            Log.d("KWP", "Failed to poll due to " + e.toString());
        }
    }
}
