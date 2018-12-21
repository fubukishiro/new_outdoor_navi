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
        if(d1 - d2 >=0)
            ascendCount++;
        if(d2 - d3 >= 0)
            ascendCount++;
        if(d3 - d4 >= 0)
            ascendCount++;
        if(d4 - d5 >= 0)
            ascendCount++;

        if(ascendCount > 2)
            return true;
        else
            return false;
    }

    public static boolean judgeCountTrend(List<Integer> rssiCountArray, FileLogger mFileLogger){
        int currentNum = rssiCountArray.size();

        int ascendCount = 0;
        for(int i = currentNum - 1 ;i > currentNum - 7;i--){
            int temp1 = rssiCountArray.get(i);
            int temp2 = rssiCountArray.get(i-1);

            if((temp1 - temp2 >=0)&&(temp2!=0))
                ascendCount++;

        }

        mFileLogger.writeTxtToFile("当前rssi trend count："+ ascendCount,mFileLogger.getFilePath(),mFileLogger.getFileName());
        Log.e("judge","当前rssi trend count："+ ascendCount);

        if(ascendCount > 3)
            return true;
        else
            return false;
    }

    public static boolean judgeTimeStamp(List<Rssi> rssiArray){
        int currentNum = rssiArray.size();

        int lostCount = 0, descendCount = 0;
        for(int i = currentNum - 1 ;i > currentNum - 7;i--){
            Rssi temp1 = rssiArray.get(i);
            Rssi temp2 = rssiArray.get(i-1);

            if(temp1.getDate().getTime()  - temp2.getDate().getTime() > 2500)
                lostCount++;

            /*if(temp1.getRssi() < 0 && temp2.getRssi() < 0 && temp1.getRssi() - temp2.getRssi() < 0)
                descendCount++;*/
        }

        Log.e("judge","当前count："+lostCount);

        if(lostCount > 3)
            return true;
        else
            return false;
    }

    public static int countDisNanNumber(List<Double> rcvDisArray, FileLogger mFileLogger){
        int currentNum = rcvDisArray.size();

        int nanCount = 0;
        for(int i = currentNum - 1 ;i > currentNum - 8;i--){
           double temp = rcvDisArray.get(i);
            if(temp == 0)
                nanCount++;

        }

        mFileLogger.writeTxtToFile("当前Dis NaN count："+ nanCount,mFileLogger.getFilePath(),mFileLogger.getFileName());
        Log.e("judge","当前Dis NaN count："+ nanCount);

        return nanCount;
    }
    public static int countRssiNanNumber(List<Rssi> rssiArray,FileLogger mFileLogger){
        int currentNum = rssiArray.size();

        int nanCount = 0;
        for(int i = currentNum - 1 ;i > currentNum - 8;i--){
            Rssi temp = rssiArray.get(i);

            if(temp.getRssi() == 0)
                nanCount++;

        }

        mFileLogger.writeTxtToFile("当前NaN count："+ nanCount,mFileLogger.getFilePath(),mFileLogger.getFileName());
        Log.e("judge","当前NaN count："+ nanCount);

        return nanCount;
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
