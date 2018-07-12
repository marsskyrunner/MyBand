package com.mars_skyrunner.myband;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

public class SensorReadingView extends LinearLayout {

    private static final String LOG_TAG = SensorReadingView.class.getSimpleName();
    Context mContext;
    SensorCheckBox checkBox;

    public TextView getUnitsTextView() {

        TextView unitsTextView = new TextView(mContext);
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        textParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        textParams.addRule(RelativeLayout.CENTER_VERTICAL);
        textParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        unitsTextView.setLayoutParams(textParams);

        unitsTextView.setId(R.id.units_textview);

        unitsTextView.setText(" hz");

        return unitsTextView;

    }

    public class SensorCheckBox extends android.support.v7.widget.AppCompatCheckBox{

        public SensorCheckBox(Context context) {
            super(context);

            RelativeLayout.LayoutParams checkBoxParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
            checkBoxParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            checkBoxParams.addRule(RelativeLayout.CENTER_VERTICAL);
            checkBoxParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

            setId(R.id.sensor_checkbox);
            setLayoutParams(checkBoxParams);

            setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    Log.v(LOG_TAG, "SensorCheckBox onCheckedChanged: " + b);
                    Intent stopReadingIntent = new Intent(Constants.RESET_SENSOR_READING);
                    stopReadingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    mContext.sendBroadcast(stopReadingIntent);
                }
            });


        }
    }


    public class PrimaryTextView extends android.support.v7.widget.AppCompatTextView{

        public PrimaryTextView(Context context) {
            super(context);

            RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
            textViewParams.addRule(RelativeLayout.RIGHT_OF,R.id.sensor_checkbox);
            textViewParams.addRule(RelativeLayout.CENTER_VERTICAL);
            textViewParams.leftMargin = convertDipToPixels(16);
            setLayoutParams(textViewParams);

            setTextAppearance(mContext,R.style.PrimaryTextStyle);
            setAllCaps(true);
            setId(R.id.sensor_name);

        }


    }

    public class SecondaryTextView extends android.support.v7.widget.AppCompatTextView{

        public SecondaryTextView(Context context) {
            super(context);

            RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
            textViewParams.addRule(RelativeLayout.CENTER_VERTICAL);
            textViewParams.leftMargin = convertDipToPixels(16);
            setLayoutParams(textViewParams);

            setId(R.id.sensor_value);

        }


    }


    public int convertDipToPixels(double dips)
    {
        return (int) (dips * mContext.getResources().getDisplayMetrics().density + 0.5f);
    }


    public SensorReadingView(Context context , SensorReading sensorReading) {

        super(context,null);

        mContext = context;

        setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);

        int pxTopPadding =  convertDipToPixels(8);
        int pxLRPadding =  convertDipToPixels(16);
        setPadding(pxLRPadding,pxTopPadding,pxLRPadding,0);

        setLayoutParams(layoutParams);

        checkBox = new SensorCheckBox(mContext);

        RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        RelativeLayout relativeLayoutHolder = new RelativeLayout(mContext);
        relativeLayoutHolder.setLayoutParams(rlParams);

        PrimaryTextView sensorNameTextView = new PrimaryTextView(mContext);
        SecondaryTextView sensorValueTextView = new SecondaryTextView(mContext);

        sensorNameTextView.setText(sensorReading.getSensorName());
        sensorValueTextView.setText(sensorReading.getSensorReading());

        relativeLayoutHolder.addView(checkBox);
        relativeLayoutHolder.addView(sensorNameTextView);

        RelativeLayout sampleRateDisplay = getSampleRateDisplay(sensorReading.getSensorName());
        relativeLayoutHolder.addView(sampleRateDisplay);

        addView(relativeLayoutHolder);
        addView(sensorValueTextView);


    }

    private RelativeLayout getSampleRateDisplay(String sensorName) {

        RelativeLayout displayLayout = new RelativeLayout(mContext);
        RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        rlParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rlParams.addRule(RelativeLayout.CENTER_VERTICAL);
        rlParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        displayLayout.setLayoutParams(rlParams);


        //if the sensor is Accelerometer, Gyrometer or GSR, sample rate can be set
        if(sensorName.equals(mContext.getResources().getString(R.string.accelerometer))
                || sensorName.equals(mContext.getResources().getString(R.string.gyroscope))
                || sensorName.equals(mContext.getResources().getString(R.string.gsr))){

            TextView unitsTextView = getUnitsTextView();
            Spinner sampleRateSettingView = getSampleRateSettingSpinner(sensorName);

            displayLayout.addView(unitsTextView);
            displayLayout.addView(sampleRateSettingView);

        }else{

            //if the sensor is Heart Rate, Skin Temp.,UV,Barometer or Altimeter, sample rate is 1hz

            TextView sampleRateTextView = new TextView(mContext);

            RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
            textParams.addRule(RelativeLayout.CENTER_VERTICAL);
            textParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            textParams.addRule(RelativeLayout.LEFT_OF,R.id.units_textview);
            sampleRateTextView.setLayoutParams(textParams);

            TextView unitsTextView = getUnitsTextView();

            if(sensorName.equals(mContext.getResources().getString(R.string.heart_rate))
                    || sensorName.equals(mContext.getResources().getString(R.string.skin_temperature))
                    || sensorName.equals(mContext.getResources().getString(R.string.barometer))
                    || sensorName.equals(mContext.getResources().getString(R.string.altimeter))
                    || sensorName.equals(mContext.getResources().getString(R.string.uv))){


                sampleRateTextView.setText("1");

                displayLayout.addView(sampleRateTextView);
                displayLayout.addView(unitsTextView);

            }else{

                //if the sensor is Ambient Light, sample rate is 2hz

                if(sensorName.equals(mContext.getResources().getString(R.string.ambient_light))){

                    sampleRateTextView.setText("2");

                    displayLayout.addView(sampleRateTextView);
                    displayLayout.addView(unitsTextView);

                }else{

                    //Sample Rate change when event happens

                    sampleRateTextView.setText("Value change");
                    displayLayout.addView(sampleRateTextView);

                }

            }

        }

        return displayLayout;
    }

    private Spinner getSampleRateSettingSpinner (String sensorName) {

        Spinner spinner = new Spinner(mContext);

        RelativeLayout.LayoutParams spinnerParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        spinnerParams.addRule(RelativeLayout.LEFT_OF,R.id.units_textview);
        spinnerParams.addRule(RelativeLayout.CENTER_VERTICAL);
        spinnerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        spinner.setLayoutParams(spinnerParams);
        spinner.setId(R.id.sample_rate_spinner);

        ArrayList<String> options = new ArrayList<>();





        switch (sensorName){

            case "accelerometer":
            case "gyroscope":
                options.add("8");
                options.add("31");
                options.add("62");
                break;

            case "GSR":
                options.add("5");
                options.add("0.2");
                break;


        }


        final Spinner mSpinner = spinner;

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(mContext,R.layout.sample_rate_option_textview,options);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {

                Log.v(LOG_TAG, "spinner: onItemSelected: arg2:   " + arg2);

                String  optionSelected = mSpinner.getSelectedItem().toString();

                Log.v(LOG_TAG, "spinner: optionSelected:   " + optionSelected);


            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                Log.v(LOG_TAG, "spinner: onNothingSelected: arg0:   " + arg0);
            }
        });

        spinner.setAdapter(dataAdapter);

        return spinner;

    }

    public CheckBox getSensorCheckBox(){
        return checkBox;
    }

}
