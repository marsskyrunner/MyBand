package com.mars_skyrunner.myband;

import android.content.ContentValues;
import android.net.Uri;
import android.content.Context;
import android.util.Log;

import com.mars_skyrunner.myband.data.SensorReadingContract;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Make an insertion for every sensor datapoint to readings.db .
 * returns an ArrayList<Boolean> with insertion results:
 *
 * true : if insertion was a success
 * false: if something went wrong with insertion
 *
 */

public class SaveDataPointLoader extends android.content.AsyncTaskLoader< ArrayList<Boolean>> {

    /**
     * Tag for log messages
     */
    private static final String LOG_TAG = SaveDataPointLoader.class.getName();

    Context mContext;

    ArrayList<SensorReading> mValues;

    Date mDate;

    /**
     * Constructs a new {@link SaveDataPointLoader}.
     *
     * @param context of the activity
     */

    public SaveDataPointLoader(Context context, ArrayList<SensorReading> values , Date date) {

        super(context);
        mContext = context;
        mValues = values;
        mDate = date;

    }

    @Override
    protected void onStartLoading() {

        Log.v(LOG_TAG, "onStartLoading()");
        forceLoad();
    }


    @Override
    public void deliverResult(ArrayList<Boolean> data) {
        super.deliverResult(data);


    }

    /**
     * This is on a background thread.
     */


    @Override
    public ArrayList<Boolean> loadInBackground() {


        Log.v(LOG_TAG, "SaveDataPointLoader doInBackground");

        ArrayList<Boolean> answer = new ArrayList<>();

        for (SensorReading sr : mValues) {

            String sensorValue = sr.getSensorReading();
            String sensorSampleRate = sr.getSensorReadingRate();

            // Create a ContentValues object where column names are the keys,
            // and sensorReadings values are the values.

            ContentValues values = new ContentValues();
            values.put(SensorReadingContract.ReadingEntry.COLUMN_READING_DATE, new SimpleDateFormat("d MMM yyyy").format(mDate));
            values.put(SensorReadingContract.ReadingEntry.COLUMN_READING_TIME, new SimpleDateFormat("HH:mm:ss").format(mDate));
            values.put(SensorReadingContract.ReadingEntry.COLUMN_SENSOR_NAME, sr.getSensorName());
            values.put(SensorReadingContract.ReadingEntry.COLUMN_SAMPLE_RATE, sensorSampleRate);
            values.put(SensorReadingContract.ReadingEntry.COLUMN_SENSOR_VALUE, sensorValue);

            Uri newUri;

            // This is a NEW record, so insert a new record into the provider,
            // returning the content URI for the new record.
            newUri = mContext.getContentResolver().insert(SensorReadingContract.ReadingEntry.CONTENT_URI, values);


            String result = "";
            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {

                result = mContext.getResources().getString(R.string.sensor_data_saving_failed);
                answer.add(false);

            } else {

                result = mContext.getResources().getString(R.string.sensor_data_saving_success);
                answer.add(true);

            }


            // If the new content URI is null, then there was an error with insertion.
            Log.w(LOG_TAG, "sr.getSensorName(): " + result);


        }

        return answer;

    }

}