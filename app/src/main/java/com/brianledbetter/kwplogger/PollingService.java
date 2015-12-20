package com.brianledbetter.kwplogger;

import android.content.Intent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by b3d on 12/19/15.
 */
public class PollingService extends PermanentService {
    // Ensures only one ScheduledExecutor is running at once.

    public static final String START_POLL_DIAGNOSTICS_SERVICE = "com.brianledbetter.kwplogger.START_POLL";
    public static final String STOP_POLL_DIAGNOSTICS_SERVICE = "com.brianledbetter.kwplogger.STOP_POLL";
    public static final String MEASUREMENT_GROUP = "measurementGroup";

    private int m_measurementGroup = 1;
    private ScheduledExecutorService m_pollTemperature = Executors.newSingleThreadScheduledExecutor();

    public PollingService() {
        super("com.brianledbetter.kwplogger.PollingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        if (intent.getAction().equals(START_POLL_DIAGNOSTICS_SERVICE)) {
            m_pollTemperature.shutdownNow();
            m_pollTemperature = Executors.newSingleThreadScheduledExecutor();
            m_measurementGroup = intent.getIntExtra(MEASUREMENT_GROUP, 1);
            m_pollTemperature.scheduleAtFixedRate
                    (new Runnable() {
                        public void run() {
                            Intent startIntent = new Intent(getApplicationContext(), DiagnosticsService.class);
                            startIntent.setAction(DiagnosticsService.POLL_DIAGNOSTICS_SERVICE);
                            startIntent.putExtra(DiagnosticsService.MEASUREMENT_GROUP, m_measurementGroup);
                            startService(startIntent);
                        }
                    }, 0, 250, TimeUnit.MILLISECONDS);
        }
        if (intent.getAction().equals(STOP_POLL_DIAGNOSTICS_SERVICE)) {
            m_pollTemperature.shutdownNow();
            stopSelf();

        }
    }
}
