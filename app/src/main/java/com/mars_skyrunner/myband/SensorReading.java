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

    String mSensorName;
    String mSensorReading;
    Context mContext;

    public SensorReading(Context context, String sensorName, String sensorReading){
        mContext = context;
        mSensorName = sensorName;
        mSensorReading = sensorReading;
    }


}
