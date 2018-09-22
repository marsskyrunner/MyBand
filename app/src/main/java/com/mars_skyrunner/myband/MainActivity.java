package com.mars_skyrunner.myband;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mars_skyrunner.myband.data.SensorReadingContract;
import com.mars_skyrunner.myband.data.SensorReadingContract.ReadingEntry;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class MainActivity extends AppCompatActivity{

    public static BandClient client = null;
    final String LOG_TAG = "MainActivity";
    private TextView bandStatusTxt;
    Toolbar toolbar;
    public static LinearLayout mListView;
    LinearLayout mLoadingView, mMainLayout;
    ArrayList<SensorReading> sensorReadings;
    File saveFile;
    Date date;
    public static boolean bandSubscriptionTaskRunning = false;

    public static SaveButton saveDataButton;

    boolean saveClicked = false;
    FrameLayout holder, saveButtonHolder;
    ToggleButton toggle;

    ImageButton settingsButton;
    ArrayList<SensorReading> values = new ArrayList<SensorReading>();

    String timeStampPattern = "ddMMyyyy";
    String fileNameExtension = ".csv";
    String displayDate ;
    String filename  ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.w(LOG_TAG,"onCreate()");

        setTheme(R.style.AppTheme_NoActionBar);

        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mListView = (LinearLayout) findViewById(R.id.sensor_list);
        initSensorListView();

        mMainLayout = (LinearLayout) findViewById(R.id.main_layout);
        mLoadingView = (LinearLayout) findViewById(R.id.loading_layout);

        date = new Date();
        displayDate = new SimpleDateFormat(timeStampPattern).format(date);;


        settingsButton = (ImageButton) findViewById(R.id.settigs_imagebutton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               new EditLabelDialog(MainActivity.this).show();;

            }
        });

        saveButtonHolder =  (FrameLayout) findViewById(R.id.save_button_holder);

        saveDataButton = new SaveButton(this);
        saveDataButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_system_update_alt_white_24dp));
        saveButtonHolder.addView(saveDataButton);

        toggle = (ToggleButton) findViewById(R.id.togglebutton);
        holder =  (FrameLayout) findViewById(R.id.toggle_button_holder);

        toggle.setTextOff(getResources().getString(R.string.start));
        toggle.setTextOn(getResources().getString(R.string.stop));

        toggle.setChecked(false);

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    // The toggle is enabled
                    Log.v(LOG_TAG,"ToggleButton startButtonClicked()");

                    holder.setBackground(getResources().getDrawable(R.drawable.toggle_button_off_background));
                    clearSensorTextViews();

                    if(!bandSubscriptionTaskRunning){

                        // Kick off the  loader
                        getLoaderManager().restartLoader(Constants.BAND_SUSCRIPTION_LOADER, null, bandSensorSubscriptionLoader);

                    }


                } else {
                    // The toggle is disabled
                    Log.v(LOG_TAG,"ToggleButton stopButtonClicked()");

                    bandSubscriptionTaskRunning = false;
                    holder.setBackground(getResources().getDrawable(R.drawable.toggle_button_on_background));
                    resetSaveDataButton();
                    clearSensorTextViews();
                    disconnectBand();

                }
            }
        });

        showLoadingView(false);

        saveDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.w(LOG_TAG, "saveDataButton : " +bandSubscriptionTaskRunning);

                Log.w(LOG_TAG, "saveDataButton isChecked(): " +saveDataButton.isChecked() );

                if (bandSubscriptionTaskRunning) {

                    boolean sensorSelected = false;

                    saveClicked = true;

                    for (SensorReading sr : sensorReadings) {

                        if(!sr.getSensorName().equals("band contact")){
                            SensorReadingView sensorReadingView = getSensorReadingView(sr);

                            Log.v(LOG_TAG,sr.getSensorName() + " : " + sensorReadingView.getSensorCheckBox().isChecked());

                            if (sensorReadingView.getSensorCheckBox().isChecked()) {

                                sensorSelected = true;

                            }
                        }

                    }

                    if(!sensorSelected){
                        stopButtonClicked();
                        //Band is connected, but no sensor is selected to take any data point
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.no_data_point), Toast.LENGTH_SHORT).show();

                    }else{
                        if(saveDataButton.isChecked()){
                            resetSaveDataButton();
                        }else{
                            saveDataButton.setChecked(true);
                            saveButtonHolder.setBackground(getResources().getDrawable(R.drawable.save_button_on));
                        }
                    }

                } else {

                    Log.e(LOG_TAG, "bandSubscriptionTaskRunning: " + bandSubscriptionTaskRunning);

                    Toast.makeText(MainActivity.this, getResources().getString(R.string.no_data_point), Toast.LENGTH_SHORT).show();
                }

            }
        });

        //Register broadcast receiver to reset activity if any checkbox is selected
        registerReceiver(resetSensorReadingReceiver, new IntentFilter(Constants.RESET_SENSOR_READING));

        //Register broadcast receiver to print values on screen from BandSensorsSubscriptionLoader
        registerReceiver(displayVaueReceiver, new IntentFilter(Constants.DISPLAY_VALUE));

        //Register broadcast receiver to save SensorReading objects from BandSensorsSubscriptionLoader
        registerReceiver(sensorReadingObjectReceiver, new IntentFilter(Constants.SENSOR_READING_OBJECT_RECEIVER));


        //Register broadcast receiver to save SensorReading objects from BandSensorsSubscriptionLoader
        registerReceiver(createCSVReceiver, new IntentFilter(Constants.CREATE_CSV_RECEIVER));



        bandStatusTxt = (TextView) toolbar.findViewById(R.id.band_status);

    }

    private void startButtonClicked() {
        Log.v(LOG_TAG, "btnStart onClick");

        holder.setBackground(getResources().getDrawable(R.drawable.toggle_button_off_background));
        clearSensorTextViews();

        if(!bandSubscriptionTaskRunning){

            // Kick off the  loader
            getLoaderManager().restartLoader(Constants.BAND_SUSCRIPTION_LOADER, null, bandSensorSubscriptionLoader);

        }

    }

    private void showLoadingView(boolean loadingState) {

        Log.v(LOG_TAG, "showLoadingView loadingState: " + loadingState);

        if (loadingState) {
            findViewById(R.id.main_layout).setVisibility(View.GONE);
            saveDataButton.setVisibility(View.GONE);
            settingsButton.setVisibility(View.GONE);
            findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
            saveDataButton.setVisibility(View.VISIBLE);
            settingsButton.setVisibility(View.VISIBLE);
            findViewById(R.id.loading_layout).setVisibility(View.GONE);
        }

    }

    private File getCsvOutputFile(File dir, Date date) {

        // the name of the file to export with

        try{

            displayDate = new SimpleDateFormat(timeStampPattern).format(date);

        }catch(Exception ex){

            displayDate = timeStampPattern;
            Log.e(LOG_TAG,"getCsvOutputFile: " + ex.toString());
        }

        filename =  displayDate + fileNameExtension;

        Log.v(LOG_TAG, "getCsvOutputFile: filename: " + filename);

        return new File(dir, filename);
    }


    /**
     * if there is no SD card, create new directory objects to make directory on device
     */
    private File getOutputDirectory() {

        Log.v(LOG_TAG, "getOutputDirectory");

        File directory = null;

        if (Environment.getExternalStorageState() == null) {
            //create new file directory object

            Log.v(LOG_TAG, "getExternalStorageState() == null");

            directory = new File(Environment.getDataDirectory()
                    + "/MyBand/");

            Log.v(LOG_TAG, "directory path: " + Environment.getDataDirectory()
                    + "/MyBand/");

            // if no directory exists, create new directory
            if (!directory.exists()) {
                Log.v(LOG_TAG, "directory dont exist");
                directory.mkdir();
            } else {
                Log.v(LOG_TAG, "directory exist");
            }


            // if phone DOES have sd card
        } else if (Environment.getExternalStorageState() != null) {

            Log.v(LOG_TAG, "getExternalStorageState() != null");


            // search for directory on SD card
            directory = new File(Environment.getExternalStorageDirectory()
                    + "/Myband/");
            // if no directory exists, create new directory
            if (!directory.exists()) {
                Log.v(LOG_TAG, "directory dont exist");
                directory.mkdir();
            } else {
                Log.v(LOG_TAG, "directory exist");
            }
        }// end of SD card checking

        return directory;

    }


    private String getSensorSampleRate(SensorReading sr) {

        String value = "";
        int resourceID = 0;

        //Log.v(LOG_TAG, "getSensorSampleRate: sr.getSensorName(): " + sr.getSensorName());

        switch (sr.getSensorName()) {
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

        if (resourceID != 0) {
            sensorReadingView = findViewById(resourceID);
            value = sensorReadingView.getSampleRate();
        }


        //Log.v(LOG_TAG, "getSensorSampleRate: " + value);

        return value;
    }


    private SensorReadingView getSensorReadingView(SensorReading sr) {

        String value;
        int resourceID = 0;

        //Log.v(LOG_TAG,"getSensorReadingView: sr.getSensorName(): " + sr.getSensorName());

        switch (sr.getSensorName()) {
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

        if (resourceID != 0) {
            sensorReadingView = (SensorReadingView) findViewById(resourceID);
            sensorValueTextView = (TextView) sensorReadingView.findViewById(R.id.sensor_value);
        }


        return sensorReadingView;
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

    private void stopButtonClicked() {

        Log.v(LOG_TAG, "btnStop onClick");

        bandSubscriptionTaskRunning = false;

        resetSaveDataButton();
        resetToggleButton();
        clearSensorTextViews();
        disconnectBand();
    }

    private void resetSaveDataButton() {
        saveDataButton.setChecked(false);
        saveButtonHolder.setBackground(getResources().getDrawable(R.drawable.save_button_off));
    }

    private void resetToggleButton() {

        Log.v(LOG_TAG,"resetToggleButton");

        toggle.setChecked(false);

    }


    private void appendToUI(final String string, final String requestCode) {

        View v;
        TextView sensorValueTextView = null;

        switch (requestCode) {

            case Constants.BAND_STATUS:
                sensorValueTextView = bandStatusTxt;
                break;


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

    @Override
    protected void onStop() {
        super.onStop();

        Log.w(LOG_TAG,"onStop()");
        Log.w(LOG_TAG,"bandSubscriptionTaskRunning: " + bandSubscriptionTaskRunning);

    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.w(LOG_TAG,"onStart()");
        Log.w(LOG_TAG,"bandSubscriptionTaskRunning: " + bandSubscriptionTaskRunning);

        //This means MyApp is restarting
        if(bandSubscriptionTaskRunning){

            toggle.setChecked(true);

        }

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        Log.w(LOG_TAG,"onPostResume()");
        Log.w(LOG_TAG,"bandSubscriptionTaskRunning: " + bandSubscriptionTaskRunning);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w(LOG_TAG,"onResume()");
        Log.w(LOG_TAG,"bandSubscriptionTaskRunning: " + bandSubscriptionTaskRunning);


    }

    private void clearSensorTextViews() {

        bandStatusTxt.setText(getResources().getString(R.string.select_option));

        for (int i = 0; i < mListView.getChildCount(); i++) {
            TextView sensorTextView = (TextView) mListView.getChildAt(i).findViewById(R.id.sensor_value);
            sensorTextView.setText("");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w(LOG_TAG, "onPause()");
        Log.w(LOG_TAG,"bandSubscriptionTaskRunning: " + bandSubscriptionTaskRunning);
//        if (client != null) {
//            try {
//
//                unregisterSensorListeners();
//
//            } catch (BandIOException e) {
//                Log.v(LOG_TAG, "onPause: BandIOException: " + e.getMessage());
//            }
//        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(LOG_TAG,"onDestroy()");
        Log.w(LOG_TAG,"bandSubscriptionTaskRunning: " + bandSubscriptionTaskRunning);

        unregisterReceiver(resetSensorReadingReceiver);
        unregisterReceiver(displayVaueReceiver);
        unregisterReceiver(sensorReadingObjectReceiver);
        unregisterReceiver(createCSVReceiver);

        try {
            unregisterSensorListeners();
        } catch (BandIOException e) {
            e.printStackTrace();
        }

        disconnectBand();

    }

    private void disconnectBand() {

        Log.v(LOG_TAG,"disconnectBand()");

        if (client != null) {
            Log.v(LOG_TAG,"client != null");

            try {
                client.disconnect().await(3 , TimeUnit.SECONDS);

            } catch (TimeoutException e) {
                Log.e(LOG_TAG,"TimeoutException: " + e.toString());
            } catch (InterruptedException e) {
                Log.e(LOG_TAG,"InterruptedException: " + e.toString());
            } catch (BandException e) {
                Log.e(LOG_TAG,"BandException: " + e.toString());
            }

        }else{

            Log.v(LOG_TAG,"client == null");
        }
    }

    private void unregisterSensorListeners() throws BandIOException {
        client.getSensorManager().unregisterAllListeners();
    }


    private BroadcastReceiver resetSensorReadingReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.v(LOG_TAG, "resetSensorReadingReceiver: onReceive ");
            stopButtonClicked();

        }


    };





    private BroadcastReceiver  createCSVReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.v(LOG_TAG,"createCSVReceiver onReceive");

            // Kick off saveDataCursorLoader
            getLoaderManager().restartLoader(Constants.CREATE_CSV_LOADER, null, saveDataCursorLoader);

        }

    };


    private BroadcastReceiver  sensorReadingObjectReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            SensorReading receivedSensor = intent.getParcelableExtra(Constants.SERVICE_EXTRA);

            Log.w(LOG_TAG, "sensorReadingObjectReceiver : onReceive:  "  + receivedSensor.getSensorName());

            //values.add(receivedSensor);


        }


    };


    private BroadcastReceiver displayVaueReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

//            Log.v(LOG_TAG, "displayVaueReceiver: onReceive ");

            String sensor = intent.getStringExtra(Constants.SENSOR);
            String value = intent.getStringExtra(Constants.VALUE);

            Log.v(LOG_TAG, "displayVaueReceiver: sensor: " + sensor);
            Log.v(LOG_TAG, "displayVaueReceiver: value: " + value);

            appendToUI(value,sensor);

        }


    };


    public class OpenCSVFileListener implements View.OnClickListener {


        @Override
        public void onClick(View v) {

            Log.v(LOG_TAG, "OpenCSVFileListener onClick");

            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("csv");
            Log.v(LOG_TAG, "mimeType: " + mimeType);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(saveFile), "application/vnd.ms-excel");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Verify that the intent will resolve to an activity
            if (intent.resolveActivity(getPackageManager()) != null) {
                Log.v(LOG_TAG, "resolveActivity YES");
                startActivity(intent);
            } else {
                Log.v(LOG_TAG, "resolveActivity NO");
            }

        }
    }



    private LoaderManager.LoaderCallbacks<ConnectionState> bandSensorSubscriptionLoader

            = new LoaderManager.LoaderCallbacks<ConnectionState>() {

        @Override
        public Loader<ConnectionState> onCreateLoader(int i, Bundle bundle) {

            Log.v(LOG_TAG, "bandSensorSubscriptionLoader: onCreateLoader");

            showLoadingView(true);

            return new BandSensorsSubscriptionLoader(MainActivity.this);
        }

        @Override
        public void onLoadFinished(Loader<ConnectionState> loader, ConnectionState cs) {

            Log.v(LOG_TAG, "bandSensorSubscriptionLoader: onLoadFinished ");

            showLoadingView(false);


            getLoaderManager().destroyLoader(loader.getId());

            String userMsg = "";

            if(cs != null){
                Log.v(LOG_TAG, "cs != null");
                Log.v(LOG_TAG,cs.toString());

                switch (cs){

                    case CONNECTED:

                        userMsg = "Band is bound to MS Health's band communication service and connected to its corresponding MS Band";

                        break;

                    case BOUND:
                        userMsg = "Band is bound to MS Health's band comm. service";
                        break;

                    case BINDING:
                        userMsg = "Band is binding to MS Health's band comm. service";
                        break;

                    case UNBOUND:
                        userMsg = "Band is not bound to MS Health's band comm. service";
                        break;

                    case DISPOSED:
                        userMsg = "Band has been disposed of by MS Health's band comm. service";
                        break;

                    case UNBINDING:
                        userMsg = "Band is unbinding from MS Health's band comm. service";
                        break;

                    case INVALID_SDK_VERSION:
                        userMsg = "MS Health band comm. service version mismatch";
                        break;

                    default:
                        userMsg = "Band Suscription failed.";
                        break;

                }

                Log.v(LOG_TAG,userMsg);

                if(!cs.equals(ConnectionState.CONNECTED)){
                    Log.v(LOG_TAG, "ConnectionState != CONNECTED");
                    resetToggleButton();
                    appendToUI(userMsg, Constants.BAND_STATUS);

                }else{


                    Log.v(LOG_TAG, "ConnectionState == CONNECTED");
                    bandSubscriptionTaskRunning = true;

                }

            }else{

                //Band isnt paired with phone
                resetToggleButton();
                appendToUI("Band isn't paired with your phone.", Constants.BAND_STATUS);
                Log.v(LOG_TAG, "cs == null");
                Log.v(LOG_TAG, "Band isn't paired with your phone.");

            }

        }

        @Override
        public void onLoaderReset(Loader<ConnectionState> loader) {

            Log.v(LOG_TAG, "bandSensorSubscriptionLoader: onLoaderReset");

        }
    };


    private LoaderManager.LoaderCallbacks<Cursor> saveDataCursorLoader
            = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

            Log.v(LOG_TAG, "saveDataCursorLoader: onCreateLoader");

            showLoadingView(true);

            // Define a projection that specifies the columns from the table we care about.
            String[] projection = {
                    ReadingEntry._ID,







                    ReadingEntry.COLUMN_READING_YEAR,
                    ReadingEntry.COLUMN_READING_MONTH,
                    ReadingEntry.COLUMN_READING_DAY,
                    ReadingEntry.COLUMN_READING_HH,
                    ReadingEntry.COLUMN_READING_MM,
                    ReadingEntry.COLUMN_READING_SS,
                    ReadingEntry.COLUMN_SAMPLE_RATE,
                    ReadingEntry.COLUMN_SENSOR_NAME,
                    ReadingEntry.COLUMN_SENSOR_VALUE};

            String sortOrder = ReadingEntry._ID;

            // This loader will execute the ContentProvider's query method on a background thread

            return new CursorLoader(MainActivity.this,   // Parent activity context
                    ReadingEntry.CONTENT_URI,   // Provider content URI to query
                    projection,             // Columns to include in the resulting Cursor
                    null,                   //  selection clause
                    null,                   //  selection arguments
                    sortOrder);                  //  sort order

        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {

            Log.v(LOG_TAG, "saveDataCursorLoader: onLoadFinished");

            showLoadingView(false);

            switch (loader.getId()) {
                case Constants.CREATE_CSV_LOADER:

                    File dir = getOutputDirectory();
                    saveFile = getCsvOutputFile(dir, date);

                    FileWriter fw = null;

                    try {

                        fw = new FileWriter(saveFile);

                        BufferedWriter bw = new BufferedWriter(fw);

                        int rowcount = c.getCount();
                        int colcount = c.getColumnCount();

                        //Log.w(LOG_TAG, "rowcount: " + rowcount);
                        //Log.w(LOG_TAG, "colcount: " + colcount);

                        if (rowcount > 0) {

                            c.moveToFirst();

                            for (int i = 0; i < colcount; i++) {

                                if (i != (colcount - 1)) {

                                    bw.write(c.getColumnName(i) + ",");

                                } else {

                                    bw.write(c.getColumnName(i));

                                }
                            }

                            bw.newLine();

                            for (int i = 0; i < rowcount; i++) {

                                c.moveToPosition(i);

                                for (int j = 0; j < colcount; j++) {

                                    String cellValue = c.getString(j);
                                    Log.w(LOG_TAG, j + " : " + i + " = " + cellValue);

                                    String fileValue = "";

                                    if (j != (colcount - 1)) {

                                        Log.w(LOG_TAG, j + " != " + (colcount - 1));
                                        fileValue = cellValue + ",";

                                    } else {
                                        Log.w(LOG_TAG, j + " == " + (colcount - 1));
                                        fileValue = cellValue;
                                    }

                                    //Log.w(LOG_TAG, "fileValue: " + fileValue);
                                    bw.write(fileValue);

                                }

                                bw.newLine();
                            }

                            bw.flush();

                            Log.w(LOG_TAG, "Datapoint Exported Successfully.");

                            showLoadingView(false);

                            //Show success message
                            Toast.makeText(MainActivity.this, getString(R.string.sensor_data_saving_success), Toast.LENGTH_SHORT).show();


                            //If SaveDatapoint button is clicked while MSBand still connected, it saves the
                            //newest values
                            values.clear();


                            //Save datapoint loader destroyed, so that if user comes back from
                            //CSV file viewer, it does not create a new one
                            getLoaderManager().destroyLoader(Constants.CREATE_CSV_LOADER);

                            //shows "OPEN CSV" action on a snackbar
                            Snackbar mySnackbar = Snackbar.make(mMainLayout,
                                    R.string.open_csv_file, Snackbar.LENGTH_LONG);
                            mySnackbar.setAction(R.string.open, new OpenCSVFileListener());
                            mySnackbar.show();


                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG, "FileWriter IOException: " + e.toString());
                    }
                    break;
            }

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

            Log.v(LOG_TAG, "saveDataCursorLoader: onLoaderReset");

        }

    };






    public class SaveButton extends android.support.v7.widget.AppCompatImageButton implements Checkable {

        boolean isChecked = false;

        public SaveButton(Context context) {
            super(context);
        }

        @Override
        public void setChecked(boolean b) {
            isChecked = b;
            Log.v(LOG_TAG,"SaveButton: setChecked: " + isChecked);

            if(b){
                // reset temporary table
                getContentResolver().delete(SensorReadingContract.ReadingEntry.CONTENT_URI,null,null);

            }else{

                Log.v(LOG_TAG,"SaveButton " + b + " , bandSubscriptionTaskRunning: " + bandSubscriptionTaskRunning);

                if(bandSubscriptionTaskRunning){

                    // Kick off saveDataCursorLoader
                    getLoaderManager().restartLoader(Constants.CREATE_CSV_LOADER, null, saveDataCursorLoader);
                }

            }
        }

        @Override
        public boolean isChecked() {

            Log.v(LOG_TAG,"SaveButton: isChecked: " + isChecked);

            return isChecked;
        }

        @Override
        public void toggle() {

            Log.v(LOG_TAG,"SaveButton: toggle");

        }


        @Override
        public void setBackground(Drawable background) {

            // Create an array of the attributes we want to resolve
            // using values from a theme
            // android.R.attr.selectableItemBackground requires API LEVEL 11
            int[] attrs = new int[] { android.R.attr.selectableItemBackground /* index 0 */};

            // Obtain the styled attributes. 'themedContext' is a context with a
            // theme, typically the current Activity (i.e. 'this')
            TypedArray ta = obtainStyledAttributes(attrs);

            // Now get the value of the 'listItemBackground' attribute that was
            // set in the theme used in 'themedContext'. The parameter is the index
            // of the attribute in the 'attrs' array. The returned Drawable
            // is what you are after
            Drawable drawableFromTheme = ta.getDrawable(0 /* index */);

            // Finally free resources used by TypedArray
            ta.recycle();

            // imageButton.setBackgroundDrawable(drawableFromTheme);
            super.setBackground(drawableFromTheme);
        }




    }

    private class EditLabelDialog extends AppCompatDialog {

        public EditLabelDialog(Context context) {

            super(context);

            setContentView(R.layout.edit_label_dialog);

            final EditText datePatternEditText = (EditText) findViewById(R.id.date_pattern);
            datePatternEditText.setHint(displayDate);

            Button okButton = (Button) findViewById(R.id.btnSave);
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String newPattern = datePatternEditText.getText().toString();

                    if (!TextUtils.isEmpty(newPattern)) {

                        try{

                            displayDate = new SimpleDateFormat(newPattern).format(date);

                        }catch(Exception ex){

                            displayDate = newPattern ;

                            Snackbar exSnackBar = Snackbar.make(mMainLayout,
                                    R.string.stamp_pattern_error, Snackbar.LENGTH_SHORT);
                            exSnackBar.show();

                            Log.e(LOG_TAG,"btnSave: " + ex.toString());

                        }

                        timeStampPattern = newPattern;
                    }

                    if (!TextUtils.isEmpty(newPattern) ) {
                        Toast.makeText(MainActivity.this,"Label changed.",Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(MainActivity.this,"No changes made.",Toast.LENGTH_SHORT).show();
                    }

                    cancel();


                }
            });


            Button cancelButton = (Button) findViewById(R.id.btnCancel);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cancel();
                }
            });



        }



    }

}
