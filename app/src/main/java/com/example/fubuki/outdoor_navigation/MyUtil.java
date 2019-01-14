package com.example.fubuki.outdoor_navigation;

import android.text.TextUtils;
import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;
import java.util.List;

public class MyUtil {
    //判断用户是否在走直线
    public static boolean isWalkingStraight(GpsNode blindPointSet){
        int currentNum = blindPointSet.getNodeNumber();
        double threshhold = 1.0;
        GpsPoint gpsPoint1 = blindPointSet.getGpsPoint(currentNum - 1);
        GpsPoint gpsPoint2 = blindPointSet.getGpsPoint(currentNum - 2);
        GpsPoint gpsPoint3 = blindPointSet.getGpsPoint(currentNum - 3);

        //暂时无法判断斜率不存在的情况
        if((gpsPoint1.getLongitude() - gpsPoint2.getLongitude()== 0) && (gpsPoint2.getLongitude() - gpsPoint3.getLongitude() == 0))
            return false;

        double k1 = (gpsPoint1.getLatitude() - gpsPoint2.getLatitude()) / (gpsPoint1.getLongitude() - gpsPoint2.getLatitude());
        double k2 = (gpsPoint2.getLatitude() - gpsPoint3.getLatitude()) / (gpsPoint2.getLongitude() - gpsPoint3.getLatitude());

        if(Math.abs(k1 - k2) < threshhold)
            return true;
        else
            return false;

    }
    //判断蓝牙接收距离趋势
    public static boolean judgeTrend(List<Double> distanceArray,FileLogger mFileLogger){
        /*int currentNum = distanceArray.size();
        double d1 = distanceArray.get(currentNum - 1);
        double d2 = distanceArray.get(currentNum - 2);
        double d3 = distanceArray.get(currentNum - 3);
        double d4 = distanceArray.get(currentNum - 4);
        double d5 = distanceArray.get(currentNum - 5);

        Log.e("distance judge","distance is:"+d1+"#"+d2+"#"+d3+"#"+d4);
        int ascendCount = 0;

        if(d1 - d2 >=0 && d1!=0 && d2!=0)
            ascendCount++;
        if(d2 - d3 >= 0 &&d1!=0 && d2!=0)
            ascendCount++;
        if(d3 - d4 >= 0 && d1!=0 && d2!=0)
            ascendCount++;
        if(d4 - d5 >= 0 && d1!=0 && d2!=0)
            ascendCount++;

        if(ascendCount > 2)
            return true;
        else
            return false;*/

        int currentNum = distanceArray.size();
        int distanceJudgeSize = 7;
        int ascendCount = 0;
        for(int i = currentNum - 1 ;i > currentNum - distanceJudgeSize;i--){
            double temp1 = distanceArray.get(i);
            double temp2 = distanceArray.get(i-1);

            if((temp1 - temp2 >= 0)&&(temp2 != 0))
                ascendCount++;

        }

        if(ascendCount > Math.floor(distanceJudgeSize/2)){
            mFileLogger.writeTxtToFile("距离逐渐增大的提示"+distanceArray.get(currentNum-1)+"#"+distanceArray.get(currentNum-2)+"#"+distanceArray.get(currentNum-3)+"#"+distanceArray.get(currentNum-4)+"#"+distanceArray.get(currentNum-5)+"#"+distanceArray.get(currentNum-6)+"#"+distanceArray.get(currentNum-7),mFileLogger.getFilePath(),mFileLogger.getFileName());
            return true;}
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
        for(int i = currentNum - 1 ;i > currentNum - 4;i--){
           double temp = rcvDisArray.get(i);
            if(Double.isNaN(temp))
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

    public static LatLng getSymmetricPoint(GpsPoint linePoint1,GpsPoint linePoint2,Point p){
        double a,b,c;
        double symX,symY;
        //calculate the slope
        if(linePoint1.getLongitude() - linePoint2.getLongitude() != 0) {
            a = (linePoint1.getLatitude() - linePoint2.getLatitude()) / (linePoint1.getLongitude() - linePoint2.getLongitude());
            b = -1;
            c = linePoint1.getLatitude()- a * linePoint1.getLongitude();

            double symXNumerator = 2*(p.getX()+a*p.getY()-a*c);
            double symYNumerator = 2*(a*a*p.getY()+a*p.getX()+c);
            double denominator = a*a + 1;

            symX = symXNumerator/denominator - p.getX();
            symY = symYNumerator/denominator - p.getY();

        }else {
            a = 1;
            b = 0;
            c = linePoint1.getLatitude();

            symY = linePoint1.getLongitude();
            symX = linePoint1.getLongitude() + (linePoint1.getLongitude() - p.getX());
        }
        System.out.print("the parameter:"+"a:"+a+" b:"+b+" c:"+c+"\n");

        return new LatLng(symY,symX);
    }

    public static double calculatePositionError(ArrayList<GpsPoint> reliablePoint,Point estimatePos){
        double posError;
        double dis1 =  DistanceUtil.getDistance(new LatLng(reliablePoint.get(0).getLatitude(),reliablePoint.get(0).getLongitude()),new LatLng(estimatePos.getY(),estimatePos.getX()));
        double dis2 =  DistanceUtil.getDistance(new LatLng(reliablePoint.get(1).getLatitude(),reliablePoint.get(1).getLongitude()),new LatLng(estimatePos.getY(),estimatePos.getX()));
        double dis3 =  DistanceUtil.getDistance(new LatLng(reliablePoint.get(2).getLatitude(),reliablePoint.get(2).getLongitude()),new LatLng(estimatePos.getY(),estimatePos.getX()));
        posError = Math.abs(reliablePoint.get(0).getDistance()-dis1) + Math.abs(reliablePoint.get(1).getDistance()-dis2) + Math.abs(reliablePoint.get(2).getDistance()-dis3);

        return posError;
    }
}
