package com.example.fubuki.outdoor_navigation;

import android.text.TextUtils;
import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;
import java.util.List;

public class MyUtil {
    //判断蓝牙接收距离趋势
    public static boolean judgeTrend(List<Double> distanceArray){
        int currentNum = distanceArray.size();
        double d1 = distanceArray.get(currentNum - 1);
        double d2 = distanceArray.get(currentNum - 2);
        double d3 = distanceArray.get(currentNum - 3);
        double d4 = distanceArray.get(currentNum - 4);
        double d5 = distanceArray.get(currentNum - 5);

        Log.e("distance judge","distance is:"+d1+"#"+d2+"#"+d3+"#"+d4);
        int ascendCount = 0;
        if(d1 - d2 > 0)
            ascendCount++;
        if(d2 - d3 > 0)
            ascendCount++;
        if(d3 - d4 > 0)
            ascendCount++;
        if(d4 - d5 > 0)
            ascendCount++;

        if(ascendCount > 2)
            return true;
        else
            return false;
    }

    public static boolean judgeTimeStamp(List<Rssi> rssiArray){
        int currentNum = rssiArray.size();
        Rssi r1 = rssiArray.get(currentNum - 1);
        Rssi r2 = rssiArray.get(currentNum - 2);
        Rssi r3 = rssiArray.get(currentNum - 3);
        Rssi r4 = rssiArray.get(currentNum - 4);
        Rssi r5 = rssiArray.get(currentNum - 5);
        Rssi r6 = rssiArray.get(currentNum - 6);
        Rssi r7 = rssiArray.get(currentNum - 7);

        int lostCount = 0, descendCount = 0;
        for(int i = currentNum - 1 ;i > currentNum - 7;i--){
            Rssi temp1 = rssiArray.get(i);
            Rssi temp2 = rssiArray.get(i-1);

            if(temp1.getDate().getTime()  - temp2.getDate().getTime() > 1500 && temp1.getDate().getTime() - temp2.getDate().getTime() < 2500)
                lostCount++;

            if(temp1.getRssi() < 0 && temp2.getRssi() < 0 && temp1.getRssi() - temp2.getRssi() < 0)
                descendCount++;
        }

        if(lostCount > 3 && descendCount > 3)
            return true;
        else
            return false;
    }
    //寻找盲走序列中距离最小的点
    public static GpsPoint searchMinPoint(GpsNode blindPointSet){
        int minIndex = 0;
        double minDis = Double.MAX_VALUE;
        for(int i = 0; i < blindPointSet.getNodeNumber(); i++){
            if(blindPointSet.getGpsPoint(i).getDistance() < minDis){
                minIndex = i;
                minDis = blindPointSet.getGpsPoint(i).getDistance();
            }
        }

        return blindPointSet.getGpsPoint(minIndex);
    }

    //string转float
    public static double convertToDouble(String number, float defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(number);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
