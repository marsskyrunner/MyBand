package com.mars_skyrunner.myband;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;

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

    Context mContext;

    String mSensorReadingYear;
    String mSensorReadingMonth;
    String mSensorReadingDay;


    public String getmSensorReadingYear() {
        return mSensorReadingYear;
    }

    public void setmSensorReadingYear(String mSensorReadingYear) {
        this.mSensorReadingYear = mSensorReadingYear;
    }

    public String getmSensorReadingMonth() {
        return mSensorReadingMonth;
    }

    public void setmSensorReadingMonth(String mSensorReadingMonth) {
        this.mSensorReadingMonth = mSensorReadingMonth;
    }

    public String getmSensorReadingDay() {
        return mSensorReadingDay;
    }

    public void setmSensorReadingDay(String mSensorReadingDay) {
        this.mSensorReadingDay = mSensorReadingDay;
    }

    public String getmSensorReadingHH() {
        return mSensorReadingHH;
    }

    public void setmSensorReadingHH(String mSensorReadingHH) {
        this.mSensorReadingHH = mSensorReadingHH;
    }

    public String getmSensorReadingMM() {
        return mSensorReadingMM;
    }

    public void setmSensorReadingMM(String mSensorReadingMM) {
        this.mSensorReadingMM = mSensorReadingMM;
    }

    public String getmSensorReadingSS() {
        return mSensorReadingSS;
    }

    public void setmSensorReadingSS(String mSensorReadingSS) {
        this.mSensorReadingSS = mSensorReadingSS;
    }

    String mSensorReadingHH;
    String mSensorReadingMM;
    String mSensorReadingSS;


    public SensorReading(Context context, String sensorName, String sensorReading){
        mContext = context;
        mSensorName = sensorName;
        mSensorReading = sensorReading;
    }



    public SensorReading(Context context, String sensorName, String sensorReading, String rate, long time){

        mContext = context;
        mSensorName = sensorName;
        mSensorReading = sensorReading;
        mSensorReadingRate = rate;

        mSensorReadingYear = new SimpleDateFormat("yyyy").format(time);
        mSensorReadingMonth = new SimpleDateFormat("MM").format(time);
        mSensorReadingDay = new SimpleDateFormat("dd").format(time);

        mSensorReadingHH = new SimpleDateFormat("HH").format(time);
        mSensorReadingMM = new SimpleDateFormat("mm").format(time);
        mSensorReadingSS = new SimpleDateFormat("ss").format(time);

    }


    protected SensorReading(Parcel in) {
        mSensorReadingRate = in.readString();
        mSensorName = in.readString();
        mSensorReading = in.readString();


        mSensorReadingYear = in.readString();
        mSensorReadingMonth = in.readString();
        mSensorReadingDay = in.readString();

        mSensorReadingHH = in.readString();
        mSensorReadingMM = in.readString();
        mSensorReadingSS = in.readString();

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


        dest.writeString(mSensorReadingYear);
        dest.writeString(mSensorReadingMonth);
        dest.writeString(mSensorReadingDay);

        dest.writeString(mSensorReadingHH);
        dest.writeString(mSensorReadingMM);
        dest.writeString(mSensorReadingSS);

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