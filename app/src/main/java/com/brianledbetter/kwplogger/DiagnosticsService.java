package com.brianledbetter.kwplogger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import com.brianledbetter.kwplogger.KWP2000.DiagnosticSession;
import com.brianledbetter.kwplogger.KWP2000.DiagnosticTroubleCode;
import com.brianledbetter.kwplogger.KWP2000.ECUIdentification;
import com.brianledbetter.kwplogger.KWP2000.ELMIO;
import com.brianledbetter.kwplogger.KWP2000.HexUtil;
import com.brianledbetter.kwplogger.KWP2000.KWP2000IO;
import com.brianledbetter.kwplogger.KWP2000.KWPException;
import com.brianledbetter.kwplogger.KWP2000.MeasurementValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

/**
 * Created by b3d on 12/6/15.
 */
public class DiagnosticsService extends PermanentService {
    public static final String START_DIAGNOSTICS_SERVICE = "com.brianledbetter.kwplogger.StartService";
    public static final String READ_MEMORY_SERVICE = "com.brianledbetter.kwplogger.ReadMemoryService";
    public static final String POLL_DIAGNOSTICS_SERVICE = "com.brianledbetter.kwplogger.PollService";
    public static final String END_DIAGNOSTICS_SERVICE = "com.brianledbetter.kwplogger.EndService";
    public static final String READ_CODES_SERVICE = "com.brianledbetter.kwplogger.ReadCodesService";
    public static final String RESET_CLUSTER_SERVICE = "com.brianledbetter.kwplogger.ResetClusterService";

    public static final String ECU_ID_STRING = "ecuID";
    public static final String VALUE_STRING = "value";
    public static final String CODES_STRING = "codes";

    public static final String MEASUREMENT_GROUP = "measurementGroup";
    public static final String MEMORY_ADDRESS = "memoryAddress";
    public static final String MEMORY_SIZE = "memorySize";
    public static final String INIT_ADDRESS = "initAddress";
    public static final String REMOTE_ADDRESS = "remoteAddress";
    public static final String BLUETOOTH_DEVICE = "bluetoothDevice";
    public static final String ERROR_STRING = "error";

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
            String bluetoothDevice = intent.getStringExtra(BLUETOOTH_DEVICE);
            Log.d("KWP", "Starting connection!");
            startConnection(initAddress, remoteAddress, bluetoothDevice);
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
        if (intent.getAction().equals(READ_MEMORY_SERVICE)) {
            Log.d("KWP", "Reading Memory");
            int address = intent.getIntExtra(MEMORY_ADDRESS, 0x1);
            int size = intent.getIntExtra(MEMORY_SIZE, 0x1);
            readMemory(address, size);
        }
        if (intent.getAction().equals(READ_CODES_SERVICE)) {
            Log.d("KWP", "Reading Codes");
            readCodes();
        }
        if (intent.getAction().equals(RESET_CLUSTER_SERVICE)) {
            Log.d("KWP", "Resetting cluster...");
            resetCluster(intent.getStringExtra(BLUETOOTH_DEVICE));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        endConnection();
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

    private void startConnection(int initAddress, int remoteAddress, String bluetoothDevice) {
        try {
            if(!connectBluetooth(initAddress, remoteAddress, bluetoothDevice)) return;
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
            broadcastError("Issue opening ECU. Is the key on? " + e.toString());
        }
    }

    private boolean connectBluetooth(int initAddress, int remoteAddress, String bluetoothDeviceAddress) {
        BluetoothAdapter b = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice bluetoothDevice = b.getRemoteDevice(bluetoothDeviceAddress);
        try {
            m_btSocket = bluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
            Log.d("KWP", "Created RFComm Service");
        } catch (IOException e) {
            Log.d("KWP", "RFComm Creation Failed");
            broadcastError("Issue opening connection. Is the ELM327 device on? " + e.toString());
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
            broadcastError("Issue opening connection. Is the ELM327 device on? " + e.toString());
            try {
                m_btSocket.close();
                return false;
            } catch (IOException e2) { // J A V A
                broadcastError("Issue closing connection. Is the ELM327 device on? " + e.toString());
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

    private void securityLogin() {
        if(!m_isConnected) {
            return;
        }
        try {
            m_kwp.securityLogin();
        } catch (KWPException e)
        {
            Log.d("KWP", "Failed to login due to " + e.toString());
        }
    }

    private void readMemory(int address, int length) {
        if (!m_isConnected) {
            return;
        }
        try {
            Log.d("KWP", "Read memory : " + HexUtil.bytesToHexString(m_kwp.readMemoryByAddress(address, length)));
        } catch (KWPException e)
        {
            Log.d("KWP", "Failed to read memory due to " + e.toString());
        }
    }

    private void readCodes() {
        if(!m_isConnected) {
            return;
        }
        try {
            List<DiagnosticTroubleCode> dtcs = m_kwp.readDTCs();
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(DiagnosticCodesActivity.DiagnosticReceiver.CODES_RESP);
            broadcastIntent.putExtra(VALUE_STRING, new ParcelableDTC(dtcs));
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            sendBroadcast(broadcastIntent);

        } catch (KWPException e)
        {
            broadcastError("Issue reading DTCs. Is the key on? " + e.toString());
        }
    }

    private void resetCluster(String bluetoothDevice) {
        int initAddress = 0x61;
        int remoteAddress = 0x97;
        startConnection(initAddress, remoteAddress, bluetoothDevice);
        if(m_kwp == null) return;
        try {
            m_kwp.clearCayenneClusterServiceIndicator();
        } catch (KWPException e)
        {
            broadcastError("Issue clearing cluster. Is the key on? " + e.toString());
        }
    }

    private void broadcastError(String errorMessage) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.DiagnosticReceiver.FAILURE_RESP);
        broadcastIntent.putExtra(ERROR_STRING, errorMessage);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(broadcastIntent);
    }
}
