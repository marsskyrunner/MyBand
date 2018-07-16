package com.mars_skyrunner.myband;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class CreateSensorReadingObjectService extends IntentService {

    Context mContext;

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

        extra = intent.getExtras().getBundle(Constants.SERVICE_EXTRA);

        String date = extra.getString(Constants.SENSOR_DATE);
        String time = extra.getString(Constants.SENSOR_TIME);
        String rate = extra.getString(Constants.SENSOR_RATE);
        String name = extra.getString(Constants.SENSOR_NAME);
        String value = extra.getString(Constants.SENSOR_VALUE);

        Log.v(LOG_TAG,"date: " +  date);
        Log.v(LOG_TAG,"time: " +  time);
        Log.v(LOG_TAG,"rate: " +  rate);
        Log.v(LOG_TAG,"name: " +  name);
        Log.v(LOG_TAG,"value: " + value);

        SensorReading sensorReading = new SensorReading(mContext,name,value,rate,date,time);

        //(Context context, String sensorName, String sensorReading, String rate, String  date, String time){
                //

        Intent stopReadingIntent = new Intent(Constants.SENSOR_READING_OBJECT_RECEIVER);
        //stopReadingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        stopReadingIntent.putExtra(Constants.SERVICE_EXTRA,sensorReading);
       sendBroadcast(stopReadingIntent);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "service " + extra + " starting", Toast.LENGTH_SHORT).show();

        Log.w(LOG_TAG,"onStartCommand: " + "service  starting");

        mContext = getBaseContext();
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.e(LOG_TAG,"onDestroy()service ");

    }
}
