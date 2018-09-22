package com.mars_skyrunner.myband;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;


import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandResultCallback;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.InvalidBandVersionException;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandAltimeterEvent;
import com.microsoft.band.sensors.BandAltimeterEventListener;
import com.microsoft.band.sensors.BandAmbientLightEvent;
import com.microsoft.band.sensors.BandAmbientLightEventListener;
import com.microsoft.band.sensors.BandBarometerEvent;
import com.microsoft.band.sensors.BandBarometerEventListener;
import com.microsoft.band.sensors.BandCaloriesEvent;
import com.microsoft.band.sensors.BandCaloriesEventListener;
import com.microsoft.band.sensors.BandContactEvent;
import com.microsoft.band.sensors.BandContactEventListener;
import com.microsoft.band.sensors.BandDistanceEvent;
import com.microsoft.band.sensors.BandDistanceEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandPedometerEvent;
import com.microsoft.band.sensors.BandPedometerEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.BandSensorEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.BandUVEvent;
import com.microsoft.band.sensors.BandUVEventListener;
import com.microsoft.band.sensors.GsrSampleRate;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.SampleRate;
import com.microsoft.band.sensors.UVIndexLevel;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import static com.mars_skyrunner.myband.MainActivity.client;
import static com.mars_skyrunner.myband.MainActivity.mListView;
import static com.mars_skyrunner.myband.MainActivity.saveDataButton;

/**
 * Permorm MSBand Sensors suscription by using an AsyncTask to perform the
 * processing
 */

public class BandSensorsSubscriptionLoader extends android.content.AsyncTaskLoader<ConnectionState> {

    /**
     * Tag for log messages
     */
    private static final String LOG_TAG = BandSensorsSubscriptionLoader.class.getName();

    Context mContext;

    private boolean heartRateChecked = false;
    private boolean rrIntervalChecked = false;
    String gyroSampleRateSelection;
    String gsrSampleRateSelection;
    String accSampleRateSelection;

    /**
     * Constructs a new {@link BandSensorsSubscriptionLoader}.
     *
     * @param context          of the activity
     */

    public BandSensorsSubscriptionLoader(Context context) {

        super(context);
        mContext = context;

    }

    @Override
    protected void onStartLoading() {

        Log.v(LOG_TAG, "onStartLoading()");
        forceLoad();
    }


    @Override
    public void deliverResult(ConnectionState data) {
        super.deliverResult(data);

        if(heartRateChecked || rrIntervalChecked){
            if (!(client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED)) {
                Log.v(LOG_TAG, "UserConsent NOT GRANTED");
                ConsentDialog dialog = new ConsentDialog(mContext);
                dialog.show();
            }else{
                Log.v(LOG_TAG, "UserConsent.GRANTED");
            }
        }


    }



    /**
     * This is on a background thread.
     */



    @Override
    public ConnectionState loadInBackground() {

        ConnectionState answer = null;

        Log.v(LOG_TAG, "BandSensorsSubscriptionLoader doInBackground");

        try {

            String bandStts = "";

            ConnectionState clientState = getConnectedBandClient();

            if (ConnectionState.CONNECTED == clientState) {

                answer = ConnectionState.CONNECTED;
                bandStts = "Band is connected.";

                //Kicks off BandConnectionService
                Intent sendObjectIntent = new Intent(mContext, BandConnectionService.class);
                mContext.startService(sendObjectIntent);

                Log.v(LOG_TAG, "getConnectedBandClient(): bandStts: " + bandStts);


                CheckBox hrSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.HEART_RATE_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "HEART_RATE_SENSOR: " + hrSensorCheckBox.isChecked());

                if (hrSensorCheckBox.isChecked()) {

                    heartRateChecked = true;

                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        try {
                            client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                        } catch (BandException e) {
                            appendToUI("Sensor reading error", Constants.HEART_RATE);
                        }

                    } else {
                        Log.v(LOG_TAG, "client.getSensorManager().getCurrentHeartRateConsent() =! UserConsent.GRANTED");
                    }

                }

                CheckBox rrSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.RR_INTERVAL_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "RR_INTERVAL_SENSOR: " + rrSensorCheckBox.isChecked());

                if (rrSensorCheckBox.isChecked()) {

                    rrIntervalChecked = true;

                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {

                        Log.v(LOG_TAG, "client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED");


                        try {

                            client.getSensorManager().registerRRIntervalEventListener(mRRIntervalEventListener);

                        } catch (BandException e) {
                            e.printStackTrace();
                            appendToUI("Sensor reading error", Constants.RR_INTERVAL);

                        }

                    } else {

                        Log.v(LOG_TAG, "client.getSensorManager().getCurrentHeartRateConsent() =! UserConsent.GRANTED");

                    }

                }

                CheckBox accSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.ACCELEROMETER_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "ACCELEROMETER_SENSOR: " + accSensorCheckBox.isChecked());

                if (accSensorCheckBox.isChecked()) {
                    try {

                        Spinner accSensorSampleRateSelector = (Spinner) mListView.getChildAt(Constants.ACCELEROMETER_SENSOR).findViewById(R.id.sample_rate_spinner);
                        accSampleRateSelection = accSensorSampleRateSelector.getSelectedItem().toString();
                        Log.v(LOG_TAG, "sampleRateSelection: " + accSampleRateSelection);

                            /*
                            *  MS128 : A value representing a sample rate of every 128 milliseconds
                               MS16 : A value representing a sample rate of every 16 milliseconds
                               MS32 : A value representing a sample rate of every 32 milliseconds
                            * */

                        client.getSensorManager().registerAccelerometerEventListener(mAccelerometerEventListener, getSampleRate(accSampleRateSelection));

                    } catch (BandIOException e) {
                        e.printStackTrace();
                        appendToUI("Sensor reading error", Constants.ACCELEROMETER);
                    }
                }


                CheckBox altSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.ALTIMETER_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "ALTIMETER_SENSOR: " + altSensorCheckBox.isChecked());

                if (altSensorCheckBox.isChecked()) {

                    try {
                        client.getSensorManager().registerAltimeterEventListener(mAltimeterEventListener);
                    } catch (BandIOException e) {
                        appendToUI("Sensor reading error", Constants.ALTIMETER);
                        e.printStackTrace();
                    }
                }


                CheckBox ambLightSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.AMBIENT_LIGHT_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "AMBIENT_LIGHT_SENSOR: " + ambLightSensorCheckBox.isChecked());

                if (ambLightSensorCheckBox.isChecked()) {
                    try {
                        client.getSensorManager().registerAmbientLightEventListener(mAmbientLightEventListener);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                        appendToUI("Sensor reading error", Constants.AMBIENT_LIGHT);
                    }
                }

                CheckBox barSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.BAROMETER_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "BAROMETER_SENSOR: " + barSensorCheckBox.isChecked());

                if (barSensorCheckBox.isChecked()) {

                    try {
                        client.getSensorManager().registerBarometerEventListener(mBarometerEventListener);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                        appendToUI("Sensor reading error", Constants.BAROMETER);
                    }
                }


                CheckBox gsrSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.GSR_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "GSR_SENSOR: " + gsrSensorCheckBox.isChecked());


                if (gsrSensorCheckBox.isChecked()) {
                    try {

                        Spinner gsrSensorSampleRateSelector = (Spinner) mListView.getChildAt(Constants.GSR_SENSOR).findViewById(R.id.sample_rate_spinner);
                        gsrSampleRateSelection = gsrSensorSampleRateSelector.getSelectedItem().toString();
                        Log.v(LOG_TAG, "gsrSampleRateSelection: " + gsrSampleRateSelection);

                        client.getSensorManager().registerGsrEventListener(mGsrEventListener, getGsrSampleRate(gsrSampleRateSelection));
                    } catch (BandIOException e) {
                        e.printStackTrace();
                        appendToUI("Sensor reading error", Constants.GSR);
                    }
                }


                CheckBox calSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.CALORIES_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "CALORIES_SENSOR: " + calSensorCheckBox.isChecked());

                if (calSensorCheckBox.isChecked()) {
                    try {
                        client.getSensorManager().registerCaloriesEventListener(mCaloriesEventListener);
                    } catch (BandIOException e) {
                        appendToUI("Sensor reading error", Constants.CALORIES);
                        e.printStackTrace();
                    }
                }


                CheckBox contactSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.BAND_CONTACT_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "BAND_CONTACT_SENSOR: " + contactSensorCheckBox.isChecked());

                if (contactSensorCheckBox.isChecked()) {
                    try {
                        client.getSensorManager().registerContactEventListener(mContactEventListener);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                        appendToUI("Sensor reading error", Constants.BAND_CONTACT);
                    }

                }


                CheckBox distSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.DISTANCE_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "DISTANCE_SENSOR: " + distSensorCheckBox.isChecked());

                if (distSensorCheckBox.isChecked()) {
                    try {
                        client.getSensorManager().registerDistanceEventListener(mDistanceEventListener);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                        appendToUI("Sensor reading error", Constants.DISTANCE);
                    }
                }


                CheckBox gyroSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.GYROSCOPE_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "GYROSCOPE_SENSOR: " + gyroSensorCheckBox.isChecked());

                if (gyroSensorCheckBox.isChecked()) {
                    try {

                        Spinner gyroSensorSampleRateSelector = (Spinner) mListView.getChildAt(Constants.GYROSCOPE_SENSOR).findViewById(R.id.sample_rate_spinner);
                        gyroSampleRateSelection = gyroSensorSampleRateSelector.getSelectedItem().toString();
                        Log.v(LOG_TAG, "gyroSampleRateSelection: " + gyroSampleRateSelection);

                        client.getSensorManager().registerGyroscopeEventListener(mGyroscopeEventListener, getSampleRate(gyroSampleRateSelection));
                    } catch (BandIOException e) {
                        e.printStackTrace();
                        appendToUI("Sensor reading error", Constants.GYROSCOPE);
                    }
                }


                CheckBox pedSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.PEDOMETER_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "PEDOMETER_SENSOR: " + pedSensorCheckBox.isChecked());

                if (pedSensorCheckBox.isChecked()) {
                    try {
                        client.getSensorManager().registerPedometerEventListener(mPedometerEventListener);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                        appendToUI("Sensor reading error", Constants.PEDOMETER);
                    }
                }


                CheckBox skinTempSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.SKIN_TEMPERATURE_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "SKIN_TEMPERATURE_SENSOR: " + skinTempSensorCheckBox.isChecked());

                if (skinTempSensorCheckBox.isChecked()) {
                    try {
                        client.getSensorManager().registerSkinTemperatureEventListener(mSkinTemperatureListener);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                        appendToUI("Sensor reading error", Constants.SKIN_TEMPERATURE);
                    }
                }


                CheckBox uvSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.UV_LEVEL_SENSOR).findViewById(R.id.sensor_checkbox);
                Log.v(LOG_TAG, "UV_LEVEL_SENSOR: " + uvSensorCheckBox.isChecked());

                if (uvSensorCheckBox.isChecked()) {
                    try {
                        client.getSensorManager().registerUVEventListener(mUVEventListener);
                    } catch (BandIOException e) {
                        e.printStackTrace();
                        appendToUI("Sensor reading error", Constants.UV_LEVEL);
                    }
                }

                Log.v(LOG_TAG, bandStts);
                appendToUI(bandStts, Constants.BAND_STATUS);

            } else {

                answer = clientState;

            }

            Log.v(LOG_TAG,"answer: " + answer.toString());

        } catch (BandException e) {

            String exceptionMessage = "";

            switch (e.getErrorType()) {
                case UNSUPPORTED_SDK_VERSION_ERROR:
                    Log.e(LOG_TAG,"Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.");
                    exceptionMessage = "SDK Version unsupported";
                    break;
                case SERVICE_ERROR:
                    Log.e(LOG_TAG,"Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.");
                    exceptionMessage = "Microsoft Health BandService is not available.";
                    break;
                default:
                    exceptionMessage = "Unknown error occured: " + e.getMessage() ;
                    break;
            }

            Log.e(LOG_TAG, exceptionMessage);
            appendToUI(exceptionMessage, Constants.BAND_STATUS);

            Log.e(LOG_TAG,"BandException: " + e.toString());

        } catch (Exception e) {
            Log.e(LOG_TAG, "BandSensorsSubscriptionTask: " + e.getMessage());



        }

        return answer;


    }



//EVENT LISTENERS


    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {

                String sensorName = mContext.getResources().getString(R.string.heart_rate);
                String sensorValue =
                        String.format("%d, %s", event.getHeartRate(), event.getQuality());
                //1 : beats per minute
                //2 : Quality

                appendToUI(sensorValue, Constants.HEART_RATE);
                //Log.w(LOG_TAG,sensorName + " checks SaveButton");

                if (saveDataButton.isChecked()) {

                    createSensorReadingObject(sensorName, sensorValue, getSensorSamplingRate(sensorName));
                }
            }
        }
    };


    private BandUVEventListener mUVEventListener = new BandUVEventListener() {
        @Override
        public void onBandUVChanged(BandUVEvent bandUVEvent) {
            if (bandUVEvent != null) {

                UVIndexLevel level = bandUVEvent.getUVIndexLevel();

                String event = new StringBuilder()
                        .append(level.toString()).toString();

                Log.v(LOG_TAG, "mUVEventListener: " + event);

                appendToUI(event, Constants.UV_LEVEL);

                String sensorName = mContext.getResources().getString(R.string.uv);
                //Log.w(LOG_TAG,sensorName + " checks SaveButton");

                if (saveDataButton.isChecked()) {

                    createSensorReadingObject(sensorName, event, getSensorSamplingRate(sensorName));
                }

            }
        }
    };


    private BandSkinTemperatureEventListener mSkinTemperatureListener =
            new BandSkinTemperatureEventListener() {

                @Override
                public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent bandSkinTemperatureEvent) {
                    if (bandSkinTemperatureEvent != null) {

                        double temp = bandSkinTemperatureEvent.getTemperature();
                        DecimalFormat df = new DecimalFormat("0.00");

                        String event = new StringBuilder()
                                .append(df.format(temp)).toString();

                        //1.  Temp in °C

                        appendToUI(event, Constants.SKIN_TEMPERATURE);

                        String sensorName = mContext.getResources().getString(R.string.skin_temperature);
                        //Log.w(LOG_TAG,sensorName + " checks SaveButton");

                        if (saveDataButton.isChecked()) {

                            createSensorReadingObject(sensorName, event, getSensorSamplingRate(sensorName));
                        }
                    }
                }
            };

    private BandPedometerEventListener mPedometerEventListener = new BandPedometerEventListener() {


        @Override
        public void onBandPedometerChanged(BandPedometerEvent bandPedometerEvent) {
            if (bandPedometerEvent != null) {

                long totalSteps = bandPedometerEvent.getTotalSteps();

                String event = new StringBuilder()
                        .append(String.format("%d", totalSteps)).toString();
                //1.TotalSteps = # steps

                appendToUI(event, Constants.PEDOMETER);


                String sensorName = mContext.getResources().getString(R.string.pedometer);
                //Log.w(LOG_TAG,sensorName + " checks SaveButton");

                if (saveDataButton.isChecked()) {

                    createSensorReadingObject(sensorName, event, getSensorSamplingRate(sensorName));
                }

            }
        }
    };


    private BandGyroscopeEventListener mGyroscopeEventListener = new BandGyroscopeEventListener() {
        @Override
        public void onBandGyroscopeChanged(BandGyroscopeEvent bandGyroscopeEvent) {
            if (bandGyroscopeEvent != null) {


                String event = new StringBuilder()
                        .append(String.format("%f,", bandGyroscopeEvent.getAngularVelocityX()))
                        .append(String.format("%f,", bandGyroscopeEvent.getAngularVelocityY()))
                        .append(String.format("%f", bandGyroscopeEvent.getAngularVelocityZ())).toString();

                //1.ωX = in  °/s
                //2.ωY = in  °/s
                //3.ωZ = in  °/s

                appendToUI(event, Constants.GYROSCOPE);

                String sensorName = mContext.getResources().getString(R.string.gyroscope);
                //Log.w(LOG_TAG,sensorName + " 7");

                if (saveDataButton.isChecked()) {

                    createSensorReadingObject(sensorName, event, getSensorSamplingRate(sensorName));
                }


            }
        }
    };

    private BandDistanceEventListener mDistanceEventListener = new BandDistanceEventListener() {
        @Override
        public void onBandDistanceChanged(BandDistanceEvent bandDistanceEvent) {
            if (bandDistanceEvent != null) {

                String event = null;

                try {
                    event = new StringBuilder()





                            .append( bandDistanceEvent.getMotionType().toString() + ",")
                            .append(String.format("%d,", bandDistanceEvent.getDistanceToday()))
                            .append(String.format("%f,", bandDistanceEvent.getPace()))
                            .append(String.format("%f", bandDistanceEvent.getPace())).toString();

                    //1.Band MotionType
                    //2.Total Distance Today in cm
                    //3.Band Pace in  ms/m
                    //4.Band Speed in  cm/s

                } catch (InvalidBandVersionException e) {
                    event = e.toString();

                }

                appendToUI(event, Constants.DISTANCE);

                String sensorName = mContext.getResources().getString(R.string.distance);
                //Log.w(LOG_TAG,sensorName + " checks SaveButton");

                if (saveDataButton.isChecked()) {

                    createSensorReadingObject(sensorName, event, getSensorSamplingRate(sensorName));
                }


            }
        }
    };

    private void appendToUI(String value, String sensor) {

        Intent appendToUiIntent = new Intent(Constants.DISPLAY_VALUE);
        appendToUiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        appendToUiIntent.putExtra(Constants.SENSOR,sensor);
        appendToUiIntent.putExtra(Constants.VALUE,value);
        mContext.sendBroadcast(appendToUiIntent);

    }

    private BandContactEventListener mContactEventListener = new BandContactEventListener() {
        @Override
        public void onBandContactChanged(BandContactEvent bandContactEvent) {
            if (bandContactEvent != null) {

                String event = bandContactEvent.getContactState().toString();
                appendToUI(event, Constants.BAND_CONTACT);

//                String sensorName = mContext.getResources().getString(R.string.distance);
//
//                        if (saveDataButton.isChecked()) {
//
//                            createSensorReadingObject(sensorName, event, getSensorSamplingRate(sensorName));
//                        }
//



            }
        }
    };

    private BandRRIntervalEventListener mRRIntervalEventListener = new BandRRIntervalEventListener() {
        @Override
        public void onBandRRIntervalChanged(final BandRRIntervalEvent event) {
            if (event != null) {

                String value = String.format("%.3f", event.getInterval());
                appendToUI(value, Constants.RR_INTERVAL);

                //1.Interval in seconds

                String sensorName = mContext.getResources().getString(R.string.rr_interval);
                //7Log.w(LOG_TAG,sensorName + " checks SaveButton");

                if (saveDataButton.isChecked()) {

                    createSensorReadingObject(sensorName, value, getSensorSamplingRate(sensorName));
                }

            }
        }
    };


    private BandCaloriesEventListener mCaloriesEventListener = new BandCaloriesEventListener() {
        @Override
        public void onBandCaloriesChanged(BandCaloriesEvent bandCaloriesEvent) {
            if (bandCaloriesEvent != null) {
                String caloriesEvent = String.format("%d", bandCaloriesEvent.getCalories());
                appendToUI(caloriesEvent, Constants.CALORIES);

                //1.Calories

                String sensorName = mContext.getResources().getString(R.string.calories);
                //Log.w(LOG_TAG,sensorName + " checks SaveButton");

                if (saveDataButton.isChecked()) {

                    createSensorReadingObject(sensorName, caloriesEvent, getSensorSamplingRate(sensorName));
                }

            }

        }
    };

    private BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {
                String gsrEvent = String.format("%d", event.getResistance());
                appendToUI(gsrEvent, Constants.GSR);

                //1.GSR in kOhms

                if (saveDataButton.isChecked()) {

                    String sensorName = mContext.getResources().getString(R.string.gsr);
                    //Log.w(LOG_TAG,sensorName + " checks SaveButton");
                    createSensorReadingObject(sensorName, gsrEvent, getSensorSamplingRate(sensorName));
                }

            }
        }
    };

    private BandAccelerometerEventListener mAccelerometerEventListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {

                String stts = String.format("%.3f, %.3f,  %.3f ", event.getAccelerationX(),
                        event.getAccelerationY(), event.getAccelerationZ());

                //1. X in g's
                //2. Y in g's
                //3. Z in g's

                appendToUI(stts, Constants.ACCELEROMETER);


                String sensorName = mContext.getResources().getString(R.string.accelerometer);
                //Log.w(LOG_TAG,sensorName + " 7);

                if (saveDataButton.isChecked()) {

                    createSensorReadingObject(sensorName, stts, getSensorSamplingRate(sensorName));
                }


            }
        }
    };

    private BandAltimeterEventListener mAltimeterEventListener = new BandAltimeterEventListener() {
        @Override
        public void onBandAltimeterChanged(final BandAltimeterEvent event) {
            if (event != null) {

                String value = new StringBuilder()
                        .append(String.format("%d,", event.getTotalGain()))
                        .append(String.format("%d,", event.getTotalLoss()))
                        .append(String.format("%d,", (event.getTotalGain() - event.getTotalLoss())))
                        .append(String.format("%d,", event.getSteppingGain()))





                        .append(String.format("%d,", event.getSteppingLoss()))
                        .append(String.format("%d,", event.getStepsAscended()))
                        .append(String.format("%d,", event.getStepsDescended()))
                        .append(String.format("%f,", event.getRate()))
                        .append(String.format("%d,", event.getFlightsAscended()))
                        .append(String.format("%d", event.getFlightsDescended())).toString();

                //1.Total Gain  in cms
                //2.Total Loss in cms
                //3.Total Elevation Difference in cms
                //4.Stepping Gain in cms
                //5. Stepping Loss in cms
                //6.Steps Ascended
                //7.Steps Descended
                //8.Rate in cm/s
                //9.Flights of Stairs Ascended
                //10.Flights of Stairs Descended

                appendToUI(value, Constants.ALTIMETER);

                String sensorName = mContext.getResources().getString(R.string.altimeter);
                //Log.w(LOG_TAG,sensorName + " checks SaveButton");

                if (saveDataButton.isChecked()) {

                    createSensorReadingObject(sensorName, value, getSensorSamplingRate(sensorName));
                }

            }
        }
    };



    private BandAmbientLightEventListener mAmbientLightEventListener = new BandAmbientLightEventListener() {
        @Override
        public void onBandAmbientLightChanged(final BandAmbientLightEvent event) {
            if (event != null) {

                String value =String.format("%d", event.getBrightness());

                //1.AmbientLight in lux

                appendToUI(value, Constants.AMBIENT_LIGHT);

                String sensorName = mContext.getResources().getString(R.string.ambient_light);
                //Log.w(LOG_TAG,sensorName + " checks SaveButton");

                if (saveDataButton.isChecked()) {

                    createSensorReadingObject(sensorName, value, getSensorSamplingRate(sensorName));
                }

            }
        }
    };


    private BandBarometerEventListener mBarometerEventListener = new BandBarometerEventListener() {
        @Override
        public void onBandBarometerChanged(final BandBarometerEvent event) {
            if (event != null) {

                String barometerEvent =
                        String.format("%.3f, %.2f", event.getAirPressure(), event.getTemperature());

                //1.Air Pressure in hPa
                //2.Temperature in  Celsius

                appendToUI(barometerEvent, Constants.BAROMETER);


                String sensorName = mContext.getResources().getString(R.string.barometer);

                //Log.w(LOG_TAG,sensorName + " checks SaveButton");

                if (saveDataButton.isChecked()) {

                    createSensorReadingObject(sensorName, barometerEvent, getSensorSamplingRate(sensorName));
                }

            }
        }
    };

    private void createSensorReadingObject(String sensorName , String sensorValue, String sensorSampleRate){


        long currentTime = System.currentTimeMillis();
        String mDate = new SimpleDateFormat("d MMM yyyy").format(currentTime);
        String time = new SimpleDateFormat("HH:mm:ss").format(currentTime);

        Log.v(LOG_TAG,"currentTime: " + time);
        SensorReading sensorReading = new SensorReading(mContext,sensorName,sensorValue,sensorSampleRate,mDate,time);


        Intent sendObjectIntent = new Intent(mContext, DbInsertionService.class);
        sendObjectIntent.putExtra(Constants.SERVICE_EXTRA,sensorReading);
        mContext.startService(sendObjectIntent);

    }

    private String getSensorSamplingRate(String sensorName){

        String sampleRate = "";

        //if the sensor is Heart Rate, Skin Temp.,UV,Barometer or Altimeter, sample rate is 1hz
        //if the sensor is Ambient light, sample rate is 2hz
        // else , value change

        switch (sensorName) {
            case "heart rate":
                sampleRate = "1"; // hz
                break;

            case "rr interval":
                sampleRate = "Value change";
                break;

            case "accelerometer":
                sampleRate = accSampleRateSelection;  // hz
                break;

            case "altimeter":
                sampleRate = "1"; // hz
                break;

            case "ambient light":
                sampleRate = "2"; // hz
                break;

            case "barometer":
                sampleRate = "1"; // hz
                break;

            case "GSR":
                sampleRate = gsrSampleRateSelection ; // hz
                break;

            case "calories":
                sampleRate = "Value change";
                break;

            case "distance":
                sampleRate = "Value change";
                break;

//            case "band contact":
//                resourceID = R.id.band_contact_sensorview;
//                break;

            case "gyroscope":
                sampleRate = gyroSampleRateSelection; // hz
                break;

            case "pedometer":
                sampleRate = "Value change";
                break;

            case "skin temperature":
                sampleRate = "1"; // hz
                break;

            case "uv level":
                sampleRate = "1"; // hz
                break;

        }


        return sampleRate;

    }

    private ConnectionState getConnectedBandClient() throws InterruptedException, BandException {

        Log.v(LOG_TAG, "getConnectedBandClient");

        Log.v(LOG_TAG, "client == null");
        BandInfo[] devices = BandClientManager.getInstance().getPairedBands();

        if (devices.length == 0) {

            Log.v(LOG_TAG, "devices.length == 0");

            return client.getConnectionState();

        } else {
            Log.v(LOG_TAG, "devices.length =! 0");
        }

        client = BandClientManager.getInstance().create(mContext, devices[0]);

        Log.v(LOG_TAG, "pairedBand : " + devices[0].getName());

        appendToUI("Band is connecting...", Constants.BAND_STATUS);


        ConnectionState state  = client.getConnectionState();

        try {
            state = client.connect().await(1,java.util.concurrent.TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            Log.e(LOG_TAG,"TimeoutException: " + e.toString());
        }

        return state;
    }



    private GsrSampleRate getGsrSampleRate(String sampleRateSelection) {

        GsrSampleRate sampleRate = null;

   /*
                            * MS200: A value representing a sample rate of every 200 milliseconds
                              MS5000 : A value representing a sample rate of every 5000 milliseconds
                            * */

        String answer = "";

        switch (sampleRateSelection){
            case "5": // = 1 / 0.2 s
                sampleRate = GsrSampleRate.MS200;
                answer = "GsrSampleRate.MS200";

                break;
            case "0.2": // = 1 / 5 s
                sampleRate = GsrSampleRate.MS5000;
                answer = "GsrSampleRate.MS5000";
                break;
        }

        Log.w(LOG_TAG,"getGsrSampleRate : " + answer);
        return sampleRate;
    }




    private SampleRate getSampleRate(String sampleRateSelection) {

        SampleRate sampleRate = null;
                 /*
                            *  MS128 : A value representing a sample rate of every 128 milliseconds
                               MS16 : A value representing a sample rate of every 16 milliseconds
                               MS32 : A value representing a sample rate of every 32 milliseconds

                            * */

        String answer ="";

        switch (sampleRateSelection){
            case "8": // = 1 / 0.128 s
                sampleRate = SampleRate.MS128;
                answer = "SampleRate.MS128";
                break;
            case "31":// = 1 / 0.032 s
                sampleRate = SampleRate.MS32;
                answer = "SampleRate.MS32";
                break;
            case "62":// = 1 / 0.016 s
                sampleRate = SampleRate.MS16;
                answer = "SampleRate.MS16";
                break;
        }

        Log.w(LOG_TAG,"getSampleRate : " + answer);
        return sampleRate;
    }


    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {

            try {
                if (getConnectedBandClient() == ConnectionState.CONNECTED) {

                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                            }
                        });
                    }
                } else {
                    appendToUI("Band isn't connected. Please try again.", Constants.BAND_STATUS);
                }

            } catch (BandException e) {
                String exceptionMessage = "";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage, Constants.BAND_STATUS);

            } catch (Exception e) {
                appendToUI(e.getMessage(), Constants.BAND_STATUS);
            }
            return null;
        }
    }


    private class ConsentDialog extends AppCompatDialog {

        public ConsentDialog(Context context) {

            super(context);

            setContentView(R.layout.consent_dialog);

            final WeakReference<Activity> reference = new WeakReference<Activity>((Activity) mContext);

            Button okButton = (Button) findViewById(R.id.btnConsent);
            okButton.setOnClickListener(new View.OnClickListener() {
                @SuppressWarnings("unchecked")
                @Override
                public void onClick(View v) {
                    new HeartRateConsentTask().execute(reference);
                    stopButtonClicked();
                    dismiss();
                }
            });

        }

        private void stopButtonClicked() {

            Intent resetReadingsIntent = new Intent(Constants.RESET_SENSOR_READING);
            resetReadingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mContext.sendBroadcast(resetReadingsIntent);
        }

    }
}