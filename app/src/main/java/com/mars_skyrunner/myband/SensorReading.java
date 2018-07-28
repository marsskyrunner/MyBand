package com.mars_skyrunner.myband;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

public class SensorReading implements Parcelable {

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

    public SensorReading(Context context, String sensorName, String sensorReading){
        mSensorName = sensorName;
        mSensorReading = sensorReading;
    }



    public SensorReading(Context context, String sensorName, String sensorReading, String rate, String  date, String time){
        mSensorName = sensorName;
        mSensorReading = sensorReading;
        mSensorReadingRate = rate;
        mSensorReadingDate = date;
        mSensorReadingTime = time;
    }


    protected SensorReading(Parcel in) {
        mSensorReadingRate = in.readString();
        mSensorName = in.readString();
        mSensorReading = in.readString();
        mSensorReadingDate = in.readString();
        mSensorReadingTime = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSensorReadingRate);
        dest.writeString(mSensorName);
        dest.writeString(mSensorReading);
        dest.writeString(mSensorReadingDate);
        dest.writeString(mSensorReadingTime);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<SensorReading> CREATOR = new Parcelable.Creator<SensorReading>() {
        @Override
        public SensorReading createFromParcel(Parcel in) {
            return new SensorReading(in);
        }

        @Override
        public SensorReading[] newArray(int size) {
            return new SensorReading[size];
        }
    };
}