package com.example.fubuki.outdoor_navigation;

import java.util.Date;

public class Rssi {
    private double rssi;
    private Date currentDate;

    public double getRssi(){
        return rssi;
    }

    public Date getDate(){
        return currentDate;
    }

    public Rssi(double mRssi, Date mDate){
        rssi =  mRssi;
        currentDate = mDate;
    }
}
