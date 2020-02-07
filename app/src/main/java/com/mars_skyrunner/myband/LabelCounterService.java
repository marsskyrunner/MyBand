package com.mars_skyrunner.myband;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.logging.Logger;


public class LabelCounterService extends IntentService {

    private String LOG_TAG = LabelCounterService.class.getSimpleName();

    // filter to identify images based on their extensions

    Context mContext = LabelCounterService.this;


    public LabelCounterService() {
        super("LabelCounterService");

        Log.v(LOG_TAG, "LabelCounterService() constructor");
    }

    static final FilenameFilter IMAGE_FILTER = new FilenameFilter() {

        @Override
        public boolean accept(final File dir, final String name) {

            if (dir.isDirectory() && !(name.endsWith(".db")) && !(name.endsWith(".ini"))) {
                return true;
            }

            for (final String ext : Constants.EXTENSIONS) {

                if (name.endsWith("." + ext)) {
                    return (true);
                }
            }

            return (false);
        }
    };

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.v(LOG_TAG, "onHandleIntent");


        File directory;

        int[] labelCount = new int[4];

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
            } else {
                Log.v(LOG_TAG, "directory exist");

                labelCount = getLabelCount(directory);

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
            } else {
                Log.v(LOG_TAG, "directory exist");

                labelCount = getLabelCount(directory);

            }


            Log.v(LOG_TAG, "DONE");

        }// end of SD card checking

        appendToUI( labelCount );

    }

    private int[] getLabelCount(File directory) {

        Log.v(LOG_TAG, "getLabelCount: ");
        int[] labelCount = new int[4];
        labelCount[0] = 0;
        labelCount[1] = 0;
        labelCount[2] = 0;
        labelCount[3] = 0;


        for (File sampleFile : directory.listFiles(IMAGE_FILTER)) {/*Iteracion entre muestras*/

            Log.v(LOG_TAG, "sampleFile.getName(): " + sampleFile.getName());

            String sampleFileName = sampleFile.getName();
            String labelPrefix = sampleFileName.substring(0, 2);

            Log.v(LOG_TAG, "labelPrefix: " + labelPrefix);

            switch (labelPrefix) {

                case "up":
                    labelCount[0]++;
                    break;
                case "dw":
                    labelCount[1]++;
                    break;
                case "si":
                    labelCount[2]++;
                    break;
                case "st":
                    labelCount[3]++;
                    break;

            }

        }

        return  labelCount;

    }

    private void appendToUI(int[] value) {

        Intent appendToUiIntent = new Intent(Constants.DISPLAY_LABEL_COUNTER_VALUE);
        appendToUiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        appendToUiIntent.putExtra(Constants.VALUE, value);
        mContext.sendBroadcast(appendToUiIntent);

    }


}
