package com.mars_skyrunner.myband;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class CreateSensorReadingObjectService extends IntentService {



    private final String LOG_TAG = CreateSensorReadingObjectService.class.getSimpleName();
    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */

    Bundle extra ;

    public CreateSensorReadingObjectService() {
        super(Constants.CREATE_SENSOR_READING_OBJECT_SERVICE);

        Log.v(LOG_TAG,"CreateSensorReadingObjectService() constructor");
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // Normally we would do some work here, like download a file.
        // For our sample, we just sleep for 5 seconds.




        extra = intent.getExtras().getBundle(Constants.SERVICE_EXTRA);

        Log.v(LOG_TAG,"extra: " + extra.getString(Constants.SENSOR_NAME) );

        for(int i = 0 ; i < 5 ; i ++){

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Restore interrupt status.
                Thread.currentThread().interrupt();
            }
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "service " + extra + " starting", Toast.LENGTH_SHORT).show();

        Log.w(LOG_TAG,"onStartCommand: " + "service  starting");
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Toast.makeText(this, "service" + extra + " destroy", Toast.LENGTH_SHORT).show();

        Log.e(LOG_TAG,"onDestroy(): " + "service " + extra + " destroy");

    }
}
