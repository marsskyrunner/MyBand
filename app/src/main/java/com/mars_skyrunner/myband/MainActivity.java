package com.mars_skyrunner.myband;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mars_skyrunner.myband.data.SensorReadingContract;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandPendingResult;
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
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.BandUVEvent;
import com.microsoft.band.sensors.BandUVEventListener;
import com.microsoft.band.sensors.GsrSampleRate;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.SampleRate;
import com.microsoft.band.sensors.UVIndexLevel;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.mars_skyrunner.myband.data.SensorReadingContract.ReadingEntry;



public class MainActivity extends AppCompatActivity {

    public static BandClient client = null;
    final String LOG_TAG = "MainActivity";
    private TextView bandStatusTxt;
    private Button btnStart, btnStop;
    Toolbar toolbar;
    LinearLayout mListView;
    ArrayList<SensorReading> sensorReadings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageButton saveDataButton = (ImageButton) toolbar.findViewById(R.id.save_data_imagebutton);
        saveDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String sensorReadingsStr = "";

                Date date = new Date();

                for(SensorReading sr : sensorReadings){
                    String sensorValue = getSensorReadingViewValue(sr);

                    if(!sensorValue.equals("")){

                        sensorReadingsStr += sr.getSensorName() + " : " + sensorValue + "\n";

                        // Create a ContentValues object where column names are the keys,
                        // and sensorReadings values are the values.

                        ContentValues values = new ContentValues();
                        values.put(ReadingEntry.COLUMN_READING_DATE,new SimpleDateFormat("d MMM yyyy").format(date));
                        values.put(ReadingEntry.COLUMN_READING_TIME,new SimpleDateFormat("HH:mm:ss").format(date));
                        values.put(ReadingEntry.COLUMN_SENSOR_NAME,sr.getSensorName() );
                        values.put(ReadingEntry.COLUMN_SENSOR_VALUE,sensorValue );

                        Uri newUri;

                        // This is a NEW record, so insert a new record into the provider,
                        // returning the content URI for the new record.
                        newUri = getContentResolver().insert(ReadingEntry.CONTENT_URI, values);

                        // Show a toast message depending on whether or not the insertion was successful.
                        if (newUri == null) {
                            // If the new content URI is null, then there was an error with insertion.
                            Toast.makeText(MainActivity.this, getString(R.string.sensor_data_saving_failed), Toast.LENGTH_SHORT).show();
                        } else {
                            // Otherwise, the insertion was successful and we can display a toast.
                            Toast.makeText(MainActivity.this, getString(R.string.sensor_data_saving_success), Toast.LENGTH_SHORT).show();

                        }
                    }

                }

                Log.w(LOG_TAG,"saveDataButton");
                Log.w(LOG_TAG,"sensorReadingsStr" + sensorReadingsStr);


                Log.v(LOG_TAG, "SDK_INT: " + android.os.Build.VERSION.SDK_INT);


                File dir = getOutputDirectory();

                File saveFile = getCsvOutputFile(dir,date);



            }
        });

        //Register broadcast receiver to reset activity if any checkbox is selected
        registerReceiver(resetSensorReadingReceiver, new IntentFilter(Constants.RESET_SENSOR_READING));

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);

        mListView = (LinearLayout) findViewById(R.id.sensor_list);
        initSensorListView();

        bandStatusTxt = (TextView) toolbar.findViewById(R.id.band_status);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.v(LOG_TAG, "btnStart onClick");
                clearSensorTextViews();
                new BandSensorsSubscriptionTask().execute();

            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopButtonClicked();

            }
        });

    }

    private File getCsvOutputFile(File dir, Date date) {



        String timeStamp = new SimpleDateFormat("yyMMddHHmmss").format(date);

        // the name of the file to export with
        String filename = "dp_" +  timeStamp + ".csv";
        Log.v(LOG_TAG, "getCsvOutputFile: filename: " + filename);

         return new File(dir, filename);
    }


    /**
     * if there is no SD card, create new directory objects to make directory on device
     */
    private File getOutputDirectory() {

        Log.v(LOG_TAG,"getOutputDirectory");

        File directory = null;

        if (Environment.getExternalStorageState() == null) {
            //create new file directory object

            Log.v(LOG_TAG,"getExternalStorageState() == null");

            directory = new File(Environment.getDataDirectory()
                    + "/Myband/");

            // if no directory exists, create new directory
            if (!directory.exists()) {
                Log.v(LOG_TAG,"directory dont exist");
                directory.mkdir();
            }else{
                Log.v(LOG_TAG,"directory exist");
            }


            // if phone DOES have sd card
        } else if (Environment.getExternalStorageState() != null) {

            Log.v(LOG_TAG,"getExternalStorageState() != null");


            // search for directory on SD card
            directory = new File(Environment.getExternalStorageDirectory()
                    + "/Myband/");
            // if no directory exists, create new directory
            if (!directory.exists()) {
                Log.v(LOG_TAG,"directory dont exist");
                directory.mkdir();
            }else{
                Log.v(LOG_TAG,"directory exist");
            }
        }// end of SD card checking

        return directory;

    }


    private String getSensorReadingViewValue(SensorReading sr) {

        String value ;
        int resourceID = 0 ;

        //Log.v(LOG_TAG,"getSensorReadingViewValue: sr.getSensorName(): " + sr.getSensorName());

        switch (sr.getSensorName()){
            case "heart rate":
                resourceID = R.id.heart_rate_sensorview;
                break;

            case "rr interval":
                resourceID = R.id.rr_interval_sensorview;
                break;

            case "accelerometer":
                resourceID = R.id.accelerometer_sensorview;
                break;

            case "altimeter":
                resourceID = R.id.altimeter_sensorview;
                break;

            case "ambient light":
                resourceID = R.id.ambient_light_sensorview;
                break;

            case "barometer":
                resourceID = R.id.barometer_sensorview;
                break;

            case "GSR":
                resourceID = R.id.gsr_sensorview;
                break;

            case "calories":
                resourceID = R.id.calories_sensorview;
                break;

            case "distance":
                resourceID = R.id.distance_sensorview;
                break;

//            case "band contact":
//                resourceID = R.id.band_contact_sensorview;
//                break;

            case "gyroscope":
                resourceID = R.id.gyroscope_sensorview;
                break;

            case "pedometer":
                resourceID = R.id.pedometer_sensorview;
                break;

            case "skin temperature":
                resourceID = R.id.skin_temperature_sensorview;
                break;

            case "uv level":
                resourceID = R.id.uv_sensorview;
                break;

                default:
                    resourceID = 0;
                    break;
        }

        SensorReadingView sensorReadingView = null;
        TextView sensorValueTextView = null;

        if(resourceID != 0){
            sensorReadingView = (SensorReadingView) findViewById(resourceID);
            sensorValueTextView =(TextView) sensorReadingView.findViewById(R.id.sensor_value);
        }

        if(sensorValueTextView != null){
            value = sensorValueTextView.getText().toString();
        }else{
            value  = "";
        }

        //Log.v(LOG_TAG,"getSensorReadingViewValue: " + value);

        return value;
    }

    private void initSensorListView() {

        sensorReadings = new ArrayList<>();
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.heart_rate), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.rr_interval), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.accelerometer), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.altimeter), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.ambient_light), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.barometer), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.gsr), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.calories), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.distance), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.contact), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.gyroscope), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.pedometer), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.skin_temperature), ""));
        sensorReadings.add(new SensorReading(this, getResources().getString(R.string.uv), ""));

        populateSensorList();

    }

    private void populateSensorList() {

        for (SensorReading sr : sensorReadings) {

            SensorReadingView v = new SensorReadingView(this, sr);
            mListView.addView(v);

        }

        for (int i = 0; i < sensorReadings.size(); i++) {

            View sensorView = mListView.getChildAt(i);

            switch (i) {
                case Constants.HEART_RATE_SENSOR:
                    sensorView.setId(R.id.heart_rate_sensorview);
                    break;

                case Constants.RR_INTERVAL_SENSOR:
                    sensorView.setId(R.id.rr_interval_sensorview);
                    break;

                case Constants.ACCELEROMETER_SENSOR:
                    sensorView.setId(R.id.accelerometer_sensorview);
                    break;

                case Constants.ALTIMETER_SENSOR:
                    sensorView.setId(R.id.altimeter_sensorview);
                    break;

                case Constants.AMBIENT_LIGHT_SENSOR:
                    sensorView.setId(R.id.ambient_light_sensorview);
                    break;

                case Constants.BAROMETER_SENSOR:
                    sensorView.setId(R.id.barometer_sensorview);
                    break;

                case Constants.GSR_SENSOR:
                    sensorView.setId(R.id.gsr_sensorview);
                    break;

                case Constants.CALORIES_SENSOR:
                    sensorView.setId(R.id.calories_sensorview);
                    break;

                case Constants.DISTANCE_SENSOR:
                    sensorView.setId(R.id.distance_sensorview);
                    break;

                case Constants.BAND_CONTACT_SENSOR:
                    sensorView.setId(R.id.contact_sensorview);
                    break;

                case Constants.GYROSCOPE_SENSOR:
                    sensorView.setId(R.id.gyroscope_sensorview);
                    break;

                case Constants.PEDOMETER_SENSOR:
                    sensorView.setId(R.id.pedometer_sensorview);
                    break;

                case Constants.SKIN_TEMPERATURE_SENSOR:
                    sensorView.setId(R.id.skin_temperature_sensorview);
                    break;

                case Constants.UV_LEVEL_SENSOR:
                    sensorView.setId(R.id.uv_sensorview);
                    break;

            }

        }

    }

    private void stopButtonClicked()  {
        Log.v(LOG_TAG, "btnStop onClick");
        clearSensorTextViews();
        disconnectBand();
    }


    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (getConnectedBandClient()) {

                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                            }
                        });
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n", Constants.BAND_STATUS);
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

    private class BandSensorsSubscriptionTask extends AsyncTask<Void, Void, Void> {

        boolean consent = true;
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if(!consent){
                showConsentDialog();
            }

        }

        @Override
        protected Void doInBackground(Void... params) {

            Log.v(LOG_TAG, "BandSensorsSubscriptionTask doInBackground");

            try {

                String bandStts = "";

                if (getConnectedBandClient()) {

                    bandStts = "Band is connected.";

                    Log.v(LOG_TAG, "getConnectedBandClient(): bandStts: " + bandStts);

                    CheckBox hrSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.HEART_RATE_SENSOR).findViewById(R.id.sensor_checkbox);
                    Log.v(LOG_TAG, "HEART_RATE_SENSOR: " + hrSensorCheckBox.isChecked());

                    if (hrSensorCheckBox.isChecked()) {

                        if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                            try {
                                client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                            } catch (BandException e) {
                                appendToUI("Sensor reading error", Constants.HEART_RATE);
                            }
                            consent = true;
                        }else{
                            Log.v(LOG_TAG, "client.getSensorManager().getCurrentHeartRateConsent() =! UserConsent.GRANTED");
                            consent = false;
                        }

                    }

                    CheckBox rrSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.RR_INTERVAL_SENSOR).findViewById(R.id.sensor_checkbox);
                    Log.v(LOG_TAG, "RR_INTERVAL_SENSOR: " + rrSensorCheckBox.isChecked());

                    if (rrSensorCheckBox.isChecked()) {

                        if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {

                            Log.v(LOG_TAG, "client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED");


                            try {

                                client.getSensorManager().registerRRIntervalEventListener(mRRIntervalEventListener);

                            } catch (BandException e) {
                                e.printStackTrace();
                                appendToUI("Sensor reading error", Constants.RR_INTERVAL);

                            }
                            consent = true;
                        } else {

                            Log.v(LOG_TAG, "client.getSensorManager().getCurrentHeartRateConsent() =! UserConsent.GRANTED");
                            consent = false;
                        }

                    }

                    CheckBox accSensorCheckBox = (CheckBox) mListView.getChildAt(Constants.ACCELEROMETER_SENSOR).findViewById(R.id.sensor_checkbox);
                    Log.v(LOG_TAG, "ACCELEROMETER_SENSOR: " + accSensorCheckBox.isChecked());

                    if (accSensorCheckBox.isChecked()) {
                        try {

                            /*
                            *  MS128 : A value representing a sample rate of every 128 milliseconds
                               MS16 : A value representing a sample rate of every 16 milliseconds
                               MS32 : A value representing a sample rate of every 32 milliseconds
                            * */

                            client.getSensorManager().registerAccelerometerEventListener(mAccelerometerEventListener, SampleRate.MS128);

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

                            /*
                            * MS200: A value representing a sample rate of every 200 milliseconds
                              MS5000 : A value representing a sample rate of every 5000 milliseconds
                            * */

                            client.getSensorManager().registerGsrEventListener(mGsrEventListener, GsrSampleRate.MS5000);
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

                                                        /*
                            *  MS128 : A value representing a sample rate of every 128 milliseconds
                               MS16 : A value representing a sample rate of every 16 milliseconds
                               MS32 : A value representing a sample rate of every 32 milliseconds
                            * */

                            client.getSensorManager().registerGyroscopeEventListener(mGyroscopeEventListener, SampleRate.MS128);
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


                } else {

                    bandStts = "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n";

                }

                Log.v(LOG_TAG, bandStts);
                appendToUI(bandStts, Constants.BAND_STATUS);

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

                Log.e(LOG_TAG, exceptionMessage);
                appendToUI(exceptionMessage, Constants.BAND_STATUS);

            } catch (Exception e) {
                Log.e(LOG_TAG, "BandSensorsSubscriptionTask: " + e.getMessage());
            }

            return null;
        }
    }

    private void showConsentDialog() {

        ConsentDialog dialog =  new ConsentDialog(MainActivity.this);
        dialog.show();
    }

    private BandUVEventListener mUVEventListener = new BandUVEventListener() {
        @Override
        public void onBandUVChanged(BandUVEvent bandUVEvent) {
            if (bandUVEvent != null) {

                UVIndexLevel level = bandUVEvent.getUVIndexLevel();

                String event = new StringBuilder()
                        .append(level.toString()).toString();

                Log.v(LOG_TAG, "mUVEventListener: " + event);

                appendToUI(event, Constants.UV_LEVEL);

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
                        .append(df.format(temp) + " °C").toString();

                appendToUI(event, Constants.SKIN_TEMPERATURE);
            }
        }
    };

    private BandPedometerEventListener mPedometerEventListener = new BandPedometerEventListener() {


        @Override
        public void onBandPedometerChanged(BandPedometerEvent bandPedometerEvent) {
            if (bandPedometerEvent != null) {

                long totalSteps = bandPedometerEvent.getTotalSteps();

                String event = new StringBuilder()
                        .append(String.format("TotalSteps = %d steps\n", totalSteps)).toString();

                appendToUI(event, Constants.PEDOMETER);
            }
        }
    };



    private BandGyroscopeEventListener mGyroscopeEventListener = new BandGyroscopeEventListener() {
        @Override
        public void onBandGyroscopeChanged(BandGyroscopeEvent bandGyroscopeEvent) {
            if (bandGyroscopeEvent != null) {


                String event = new StringBuilder()
                        .append(String.format("AngularVelocityX = %f degrees/s\n", bandGyroscopeEvent.getAngularVelocityX()))
                        .append(String.format("AngularVelocityY = %f degrees/s\n", bandGyroscopeEvent.getAngularVelocityY()))
                        .append(String.format("AngularVelocityZ = %f degrees/s\n", bandGyroscopeEvent.getAngularVelocityZ())).toString();

                appendToUI(event, Constants.GYROSCOPE);

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


                            .append("Band MotionType = " + bandDistanceEvent.getMotionType().toString() + "\n")
                            .append(String.format("Total Distance Today = %d cm\n", bandDistanceEvent.getDistanceToday()))
                            .append(String.format("Band Pace = %f ms/m\n", bandDistanceEvent.getPace()))
                            .append(String.format("Band Speed = %f cm/s\n", bandDistanceEvent.getPace())).toString();

                } catch (InvalidBandVersionException e) {
                    event = e.toString();

                }

                appendToUI(event, Constants.DISTANCE);

            }
        }
    };

    private BandContactEventListener mContactEventListener = new BandContactEventListener() {
        @Override
        public void onBandContactChanged(BandContactEvent bandContactEvent) {
            if (bandContactEvent != null) {
                String event = bandContactEvent.getContactState().toString();
                appendToUI(event, Constants.BAND_CONTACT);

            }
        }
    };

    private BandRRIntervalEventListener mRRIntervalEventListener = new BandRRIntervalEventListener() {
        @Override
        public void onBandRRIntervalChanged(final BandRRIntervalEvent event) {
            if (event != null) {
                appendToUI(String.format("%.3f s\n", event.getInterval()), Constants.RR_INTERVAL);
            }
        }
    };


    private BandCaloriesEventListener mCaloriesEventListener = new BandCaloriesEventListener() {
        @Override
        public void onBandCaloriesChanged(BandCaloriesEvent bandCaloriesEvent) {
            if (bandCaloriesEvent != null) {
                String caloriesEvent = String.format("%d cals\n", bandCaloriesEvent.getCalories());
                appendToUI(caloriesEvent, Constants.CALORIES);
            }

        }
    };

    private BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {
                String gsrEvent = String.format("Resistance = %d kOhms\n", event.getResistance());
                appendToUI(gsrEvent, Constants.GSR);
            }
        }
    };

    private BandAccelerometerEventListener mAccelerometerEventListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {

                String stts = String.format(" X = %.3f g\n Y = %.3f g\n Z = %.3f g", event.getAccelerationX(),
                        event.getAccelerationY(), event.getAccelerationZ());

                appendToUI(stts, Constants.ACCELEROMETER);

            }
        }
    };

    private BandAltimeterEventListener mAltimeterEventListener = new BandAltimeterEventListener() {
        @Override
        public void onBandAltimeterChanged(final BandAltimeterEvent event) {
            if (event != null) {
                appendToUI(new StringBuilder()
                        .append(String.format("Total Gain = %d cm\n", event.getTotalGain()))
                        .append(String.format("Total Loss = %d cm\n", event.getTotalLoss()))
                        .append(String.format("Total Elevation Difference= %d cm\n", (event.getTotalGain() - event.getTotalLoss())))
                        .append(String.format("Stepping Gain = %d cm\n", event.getSteppingGain()))
                        .append(String.format("Stepping Loss = %d cm\n", event.getSteppingLoss()))
                        .append(String.format("Steps Ascended = %d\n", event.getStepsAscended()))
                        .append(String.format("Steps Descended = %d\n", event.getStepsDescended()))
                        .append(String.format("Rate = %f cm/s\n", event.getRate()))
                        .append(String.format("Flights of Stairs Ascended = %d\n", event.getFlightsAscended()))
                        .append(String.format("Flights of Stairs Descended = %d\n", event.getFlightsDescended())).toString(), Constants.ALTIMETER);
            }
        }
    };

    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                appendToUI(String.format("%d beats per minute\n"
                        + "Quality = %s\n", event.getHeartRate(), event.getQuality()), Constants.HEART_RATE);
            }
        }
    };


    private BandAmbientLightEventListener mAmbientLightEventListener = new BandAmbientLightEventListener() {
        @Override
        public void onBandAmbientLightChanged(final BandAmbientLightEvent event) {
            if (event != null) {
                appendToUI(String.format("Brightness = %d lux\n", event.getBrightness()), Constants.AMBIENT_LIGHT);
            }
        }
    };


    private BandBarometerEventListener mBarometerEventListener = new BandBarometerEventListener() {
        @Override
        public void onBandBarometerChanged(final BandBarometerEvent event) {
            if (event != null) {

                String barometerEvent =
                        String.format("Air Pressure = %.3f hPa\n"
                                + "Temperature = %.2f degrees Celsius", event.getAirPressure(), event.getTemperature());

                appendToUI(barometerEvent, Constants.BAROMETER);
            }
        }
    };


    private void appendToUI(final String string, final String requestCode) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                View v;
                TextView sensorValueTextView = null;

                switch (requestCode) {

                    case Constants.UV_LEVEL:

                        v = findViewById(R.id.uv_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;


                    case Constants.SKIN_TEMPERATURE:
                        v = findViewById(R.id.skin_temperature_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;


                    case Constants.PEDOMETER:
                        v = findViewById(R.id.pedometer_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;


                    case Constants.GYROSCOPE:
                        v = findViewById(R.id.gyroscope_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;

                    case Constants.DISTANCE:
                        v = findViewById(R.id.distance_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;

                    case Constants.BAND_CONTACT:
                        v = findViewById(R.id.band_contact_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;

                    case Constants.CALORIES:
                        v = findViewById(R.id.calories_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;

                    case Constants.RR_INTERVAL:
                        v = findViewById(R.id.rr_interval_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;

                    case Constants.GSR:
                        v = findViewById(R.id.gsr_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;

                    case Constants.BAROMETER:
                        v = findViewById(R.id.barometer_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;

                    case Constants.ACCELEROMETER:
                        v = findViewById(R.id.accelerometer_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;

                    case Constants.BAND_STATUS:
                        sensorValueTextView = bandStatusTxt;
                        break;


                    case Constants.ALTIMETER:
                        v = findViewById(R.id.altimeter_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;

                    case Constants.HEART_RATE:
                        v = findViewById(R.id.heart_rate_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;

                    case Constants.AMBIENT_LIGHT:
                        v = findViewById(R.id.ambient_light_sensorview);
                        sensorValueTextView = (TextView) v.findViewById(R.id.sensor_value);
                        break;


                }

                sensorValueTextView.setText(string);


                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        clearSensorTextViews();
    }

    private void clearSensorTextViews() {

        bandStatusTxt.setText(getResources().getString(R.string.select_option));

        for(int i = 0; i < mListView.getChildCount(); i++){
            TextView sensorTextView = (TextView) mListView.getChildAt(i).findViewById(R.id.sensor_value);
            sensorTextView.setText("");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (client != null) {
            try {

                unregisterSensorListeners();

            } catch (BandIOException e) {
                Log.v(LOG_TAG, "onPause: BandIOException: " + e.getMessage());
            }
        }
    }


    private boolean getConnectedBandClient() throws InterruptedException, BandException {

        Log.v(LOG_TAG, "getConnectedBandClient");


        if (client == null) {
            Log.v(LOG_TAG, "client == null");
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();

            if (devices.length == 0) {

                Log.v(LOG_TAG, "devices.length == 0");
                appendToUI("Band isn't paired with your phone.\n", Constants.BAND_STATUS);
                return false;
            } else {
                Log.v(LOG_TAG, "devices.length =! 0");
            }

            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else {
            Log.v(LOG_TAG, "client != null");

            if (ConnectionState.CONNECTED == client.getConnectionState()) {

                Log.v(LOG_TAG, "ConnectionState.CONNECTED");
                return true;
            } else {
                Log.v(LOG_TAG, "ConnectionState.DISCONNECTED");
            }

        }


        appendToUI("Band is connecting...\n", Constants.BAND_STATUS);

        boolean state = ConnectionState.CONNECTED == client.connect().await();

        Log.v(LOG_TAG, "getConnectedBandClient state : " + state);

        return state;
    }

    @Override
    protected void onDestroy() {

        unregisterReceiver(resetSensorReadingReceiver);

        try {
            unregisterSensorListeners();
        } catch (BandIOException e) {
            e.printStackTrace();
        }

        disconnectBand();
        super.onDestroy();
    }

    private void disconnectBand() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
                Log.e(LOG_TAG,"disconnectBand: InterruptedException: " + e.toString());
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
                Log.e(LOG_TAG,"disconnectBand: BandException: " + e.toString());
            }
        }
    }

    private void unregisterSensorListeners() throws BandIOException {
        client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);
        client.getSensorManager().unregisterRRIntervalEventListener(mRRIntervalEventListener);
        client.getSensorManager().unregisterAccelerometerEventListener(mAccelerometerEventListener);
        client.getSensorManager().unregisterAltimeterEventListener(mAltimeterEventListener);
        client.getSensorManager().unregisterAmbientLightEventListener(mAmbientLightEventListener);
        client.getSensorManager().unregisterBarometerEventListener(mBarometerEventListener);
        client.getSensorManager().unregisterGsrEventListener(mGsrEventListener);
        client.getSensorManager().unregisterCaloriesEventListener(mCaloriesEventListener);
        client.getSensorManager().unregisterContactEventListener(mContactEventListener);
        client.getSensorManager().unregisterDistanceEventListener(mDistanceEventListener);
        client.getSensorManager().unregisterGyroscopeEventListener(mGyroscopeEventListener);
        client.getSensorManager().unregisterPedometerEventListener(mPedometerEventListener);
        client.getSensorManager().unregisterSkinTemperatureEventListener(mSkinTemperatureListener);
        client.getSensorManager().unregisterUVEventListener(mUVEventListener);
    }


    private BroadcastReceiver resetSensorReadingReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.v(LOG_TAG, "resetSensorReadingReceiver: onReceive ");
            stopButtonClicked();

        }


    };


    private class ConsentDialog extends AppCompatDialog {

        public ConsentDialog(Context context) {

            super(context);

            setContentView(R.layout.consent_dialog);

            final WeakReference<Activity> reference = new WeakReference<Activity>(MainActivity.this);

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


    }
}
