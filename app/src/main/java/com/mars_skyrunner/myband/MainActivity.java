package com.mars_skyrunner.myband;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.mars_skyrunner.myband.data.SensorReadingContract.ReadingEntry;
import com.microsoft.band.ConnectionState;


public class MainActivity extends AppCompatActivity {

    public static BandClient client = null;
    final String LOG_TAG = "MainActivity";
    private TextView bandStatusTxt;
    private Button btnStart, btnStop;
    Toolbar toolbar;
    public static LinearLayout mListView;
    LinearLayout mLoadingView, mMainLayout;
    ArrayList<SensorReading> sensorReadings;
    File saveFile;
    Date date;
    boolean bandSubscriptionTaskRunning = false;
    ImageButton saveDataButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mListView = (LinearLayout) findViewById(R.id.sensor_list);
        initSensorListView();

        mMainLayout = (LinearLayout) findViewById(R.id.main_layout);
        mLoadingView = (LinearLayout) findViewById(R.id.loading_layout);
        saveDataButton = (ImageButton) toolbar.findViewById(R.id.save_data_imagebutton);

        showLoadingView(false);

        saveDataButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Log.w(LOG_TAG, "saveDataButton");

                if (bandSubscriptionTaskRunning) {

                    showLoadingView(true);

                    String sensorReadingsStr = "";

                    date = new Date();

                    for (SensorReading sr : sensorReadings) {

                        String sensorValue = getSensorReadingViewValue(sr);
                        String sensorSampleRate = getSensorSampleRate(sr);

                        if (!sensorValue.equals("")) {

                            sensorReadingsStr += sr.getSensorName() + " : " + sensorValue + "\n";

                            // Create a ContentValues object where column names are the keys,
                            // and sensorReadings values are the values.

                            ContentValues values = new ContentValues();
                            values.put(ReadingEntry.COLUMN_READING_DATE, new SimpleDateFormat("d MMM yyyy").format(date));
                            values.put(ReadingEntry.COLUMN_READING_TIME, new SimpleDateFormat("HH:mm:ss").format(date));
                            values.put(ReadingEntry.COLUMN_SENSOR_NAME, sr.getSensorName());
                            values.put(ReadingEntry.COLUMN_SAMPLE_RATE, sensorSampleRate);
                            values.put(ReadingEntry.COLUMN_SENSOR_VALUE, sensorValue);

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
                                Log.w(LOG_TAG, "sensorReadingsStr" + sensorReadingsStr);

                                // Kick off the record loader
                                getLoaderManager().restartLoader(Constants.SAVE_DATAPOINT_LOADER, null, saveDataCursorLoader);


                            }
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

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);


        bandStatusTxt = (TextView) toolbar.findViewById(R.id.band_status);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.v(LOG_TAG, "btnStart onClick");
                clearSensorTextViews();

                // Kick off the  loader
                getLoaderManager().restartLoader(Constants.BAND_SUSCRIPTION_LOADER, null, bandSensorSubscriptionLoader);


            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopButtonClicked();

            }
        });

    }

    private void showLoadingView(boolean loadingState) {

        Log.v(LOG_TAG, "showLoadingView loadingState: " + loadingState);

        if (loadingState) {
            mMainLayout.setVisibility(View.GONE);
            saveDataButton.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.VISIBLE);


        } else {
            mMainLayout.setVisibility(View.VISIBLE);
            saveDataButton.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.GONE);
        }



    }

    private File getCsvOutputFile(File dir, Date date) {


        String timeStamp = new SimpleDateFormat("ddMMyyHHmmss").format(date);

        // the name of the file to export with
        String filename = "dp_" + timeStamp + ".csv";
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

        Log.v(LOG_TAG, "getSensorSampleRate: sr.getSensorName(): " + sr.getSensorName());

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


        Log.v(LOG_TAG, "getSensorSampleRate: " + value);

        return value;
    }


    private String getSensorReadingViewValue(SensorReading sr) {

        String value;
        int resourceID = 0;

        //Log.v(LOG_TAG,"getSensorReadingViewValue: sr.getSensorName(): " + sr.getSensorName());

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

        if (sensorValueTextView != null) {
            value = sensorValueTextView.getText().toString();
        } else {
            value = "";
        }

        Log.v(LOG_TAG, "getSensorReadingViewValue: " + value);

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

    private void stopButtonClicked() {

        Log.v(LOG_TAG, "btnStop onClick");

        bandSubscriptionTaskRunning = false;

        clearSensorTextViews();
        disconnectBand();
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
    protected void onResume() {
        super.onResume();
        clearSensorTextViews();
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
        if (client != null) {
            try {

                unregisterSensorListeners();

            } catch (BandIOException e) {
                Log.v(LOG_TAG, "onPause: BandIOException: " + e.getMessage());
            }
        }
    }


    @Override
    protected void onDestroy() {

        unregisterReceiver(resetSensorReadingReceiver);
        unregisterReceiver(displayVaueReceiver);

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
                Log.e(LOG_TAG, "disconnectBand: InterruptedException: " + e.toString());
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
                Log.e(LOG_TAG, "disconnectBand: BandException: " + e.toString());
            }
        }

        Log.v(LOG_TAG,"disconnectBand(): " + client.getConnectionState().toString());

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


    private BroadcastReceiver displayVaueReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

//            Log.v(LOG_TAG, "displayVaueReceiver: onReceive ");

            String sensor = intent.getStringExtra(Constants.SENSOR);
            String value = intent.getStringExtra(Constants.VALUE);

//            Log.v(LOG_TAG, "displayVaueReceiver: sensor: " + sensor);
//            Log.v(LOG_TAG, "displayVaueReceiver: value: " + value);

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

            bandSubscriptionTaskRunning = true;
            return new BandSensorsSubscriptionLoader(MainActivity.this);
        }

        @Override
        public void onLoadFinished(Loader<ConnectionState> loader, ConnectionState cs) {

            Log.v(LOG_TAG, "bandSensorSubscriptionLoader: onLoadFinished ");

            showLoadingView(false);

            Log.v(LOG_TAG,cs.toString());

            String userMsg = "";

            switch (cs){

                case CONNECTED:

                    userMsg = "Band is bound to Microsoft Health's band communication service and connected to its corresponding Microsoft Band";

                    break;

                case BOUND:
                    userMsg = " Band is bound to Microsoft Health's band communication service";
                    break;

                case BINDING:
                    userMsg = "Band is binding to Microsoft Health's band communication service";
                    break;

                case UNBOUND:
                    userMsg = "Band is not bound to Microsoft Health's band communication service";
                    break;

                case DISPOSED:
                    userMsg = "Band has been disposed of by Microsoft Health's band communication service";
                    break;

                case UNBINDING:
                    userMsg = "Band is unbinding from Microsoft Health's band communication service";
                    break;

                case INVALID_SDK_VERSION:
                    userMsg = "Band will not be able to bind to the currently in use Microsoft Health band communication service due to a version mismatch";
                    break;

            }

            Log.v(LOG_TAG,userMsg);

            if(cs.equals(ConnectionState.CONNECTED)){
                appendToUI("Band Connected.", Constants.BAND_STATUS);
            }else{
                appendToUI(userMsg, Constants.BAND_STATUS);
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
                    ReadingEntry.COLUMN_READING_DATE,
                    ReadingEntry.COLUMN_READING_TIME,
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
                case Constants.SAVE_DATAPOINT_LOADER:

                    File dir = getOutputDirectory();
                    saveFile = getCsvOutputFile(dir, date);

                    FileWriter fw = null;

                    try {

                        fw = new FileWriter(saveFile);

                        BufferedWriter bw = new BufferedWriter(fw);

                        int rowcount = c.getCount();
                        int colcount = c.getColumnCount();

                        Log.w(LOG_TAG, "rowcount: " + rowcount);
                        Log.w(LOG_TAG, "colcount: " + colcount);

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

                                    Log.w(LOG_TAG, "fileValue: " + fileValue);
                                    bw.write(fileValue);

                                }

                                bw.newLine();
                            }

                            bw.flush();

                            Log.w(LOG_TAG, "Datapoint Exported Successfully.");

                            showLoadingView(false);

                            //Show success message
                            Toast.makeText(MainActivity.this, getString(R.string.sensor_data_saving_success), Toast.LENGTH_SHORT).show();

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


}
