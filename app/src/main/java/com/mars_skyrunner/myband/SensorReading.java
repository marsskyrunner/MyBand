package com.mars_skyrunner.myband;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;

public class SensorReading implements Parcelable {

    public String getSensorName() {

        switch(mSensorID){



            case Constants.HEART_RATE_SENSOR_ID:

                return Constants.HEART_RATE_SENSOR_LABEL;

            case Constants.RR_INTERVAL_SENSOR_ID:

                return Constants.RR_INTERVAL_SENSOR_LABEL;

            case Constants.ACCELEROMETER_SENSOR_ID:

                return Constants.ACCELEROMETER_SENSOR_LABEL;

            case Constants.ALTIMETER_SENSOR_ID:

                return Constants.ALTIMETER_SENSOR_LABEL;

            case Constants.AMBIENT_LIGHT_SENSOR_ID:

                return Constants.AMBIENT_LIGHT_SENSOR_LABEL;

            case Constants.BAROMETER_SENSOR_ID:

                return Constants.BAROMETER_SENSOR_LABEL;

            case Constants.GSR_SENSOR_ID:

                return Constants.GSR_SENSOR_LABEL;

            case Constants.CALORIES_SENSOR_ID:

                return Constants.CALORIES_SENSOR_LABEL;

            case Constants.DISTANCE_SENSOR_ID:

                return Constants.DISTANCE_SENSOR_LABEL;

            case Constants.GYROSCOPE_SENSOR_ID:

                return Constants.GYROSCOPE_SENSOR_LABEL;

            case Constants.PEDOMETER_SENSOR_ID:

                return Constants.PEDOMETER_SENSOR_LABEL;

            case Constants.SKIN_TEMPERATURE_SENSOR_ID:

                return Constants.SKIN_TEMPERATURE_SENSOR_LABEL;

            case Constants.UV_LEVEL_SENSOR_ID:

                return Constants.UV_LEVEL_SENSOR_LABEL;

            case Constants.BAND_STATUS_SENSOR_ID:

                return Constants.BAND_STATUS_SENSOR_LABEL;

            case Constants.BAND_CONTACT_SENSOR_ID:

                return Constants.BAND_CONTACT_SENSOR_LABEL;





        }

        return null;
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
    int mSensorID;
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


    public SensorReading(Context context, int sensorID, String sensorReading){
        mContext = context;
        mSensorID = sensorID;
        mSensorReading = sensorReading;
    }



    public SensorReading(Context context, int sensorID, String sensorReading, String rate, long time){

        mContext = context;
        mSensorID = sensorID;
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
        mSensorID = in.readInt();
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
        dest.writeInt(mSensorID);
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

    public int  getSensorID() {
        return mSensorID;
    }
}