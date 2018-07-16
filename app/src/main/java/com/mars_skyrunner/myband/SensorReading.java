package com.mars_skyrunner.myband;

import android.content.Context;

public class SensorReading {



    public String getSensorName() {
        return mSensorName;
    }

    public void setSensorName(String mSensorName) {
        this.mSensorName = mSensorName;
    }

    public String getSensorReading() {
        return mSensorReading;
    }

    public void setSensorReading(String mSensorReading) {
        this.mSensorReading = mSensorReading;
    }


    public String getSensorReadingRate() {
        return mSensorReadingRate;
    }

    String mSensorReadingRate;
    String mSensorName;
    String mSensorReading;
    String mSensorReadingDate;

    public String getSensorReadingDate() {
        return mSensorReadingDate;
    }

    public String getSensorReadingTime() {
        return mSensorReadingTime;
    }

    String mSensorReadingTime;
    Context mContext;


    public SensorReading(Context context, String sensorName, String sensorReading){
        mContext = context;
        mSensorName = sensorName;
        mSensorReading = sensorReading;
    }



    public SensorReading(Context context, String sensorName, String sensorReading, String rate, String  date, String time){
        mContext = context;
        mSensorName = sensorName;
        mSensorReading = sensorReading;
        mSensorReadingRate = rate;
        mSensorReadingDate = date;
        mSensorReadingTime = time;
    }

}
