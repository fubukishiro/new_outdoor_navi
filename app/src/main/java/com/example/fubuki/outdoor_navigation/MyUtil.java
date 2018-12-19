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
        double ts1 = rssiArray.get(currentNum - 1).getDate().getTime();
        double ts2 = rssiArray.get(currentNum - 2).getDate().getTime();
        double ts3 = rssiArray.get(currentNum - 3).getDate().getTime();
        double ts4 = rssiArray.get(currentNum - 4).getDate().getTime();
        double ts5 = rssiArray.get(currentNum - 5).getDate().getTime();
        double ts6 = rssiArray.get(currentNum - 6).getDate().getTime();
        double ts7 = rssiArray.get(currentNum - 7).getDate().getTime();
        //丢包率
        int lostCount = 0;
        if(ts1 - ts2 > 1500 && ts1 - ts2 < 2500)
            lostCount++;
        if(ts2 - ts3 > 1500 && ts2 - ts3 < 2500)
            lostCount++;
        if(ts3 - ts4 > 1500 && ts3 - ts4 < 2500)
            lostCount++;
        if(ts4 - ts5 > 1500 && ts4 - ts5 < 2500)
            lostCount++;
        if(ts5 - ts6 > 1500 && ts5 - ts6 < 2500)
            lostCount++;
        if(ts6 - ts7 > 1500 && ts6 - ts7 < 2500)
            lostCount++;

        if(lostCount > 3)
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
