package com.mars_skyrunner.myband;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SensorReadingView extends LinearLayout {

    private static final String LOG_TAG = SensorReadingView.class.getSimpleName();
    Context mContext;
    SensorCheckBox checkBox;

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

        addView(relativeLayoutHolder);
        addView(sensorValueTextView);


    }

    public CheckBox getSensorCheckBox(){
        return checkBox;
    }

}
