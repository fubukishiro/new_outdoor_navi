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
            //mFileLogger.writeTxtToFile("距离逐渐增大的提示"+distanceArray.get(currentNum-1)+"#"+distanceArray.get(currentNum-2)+"#"+distanceArray.get(currentNum-3)+"#"+distanceArray.get(currentNum-4)+"#"+distanceArray.get(currentNum-5)+"#"+distanceArray.get(currentNum-6)+"#"+distanceArray.get(currentNum-7),mFileLogger.getFilePath(),mFileLogger.getFileName());
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

        //mFileLogger.writeTxtToFile("当前rssi trend count："+ ascendCount,mFileLogger.getFilePath(),mFileLogger.getFileName());
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

        //mFileLogger.writeTxtToFile("当前Dis NaN count："+ nanCount,mFileLogger.getFilePath(),mFileLogger.getFileName());
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

        //mFileLogger.writeTxtToFile("当前NaN count："+ nanCount,mFileLogger.getFilePath(),mFileLogger.getFileName());
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


    //找到count最小（最可靠）的k个点
    public static ArrayList<GpsPoint> findMinK(ArrayList<GpsPoint> gpsPointArray, int k){
        ArrayList<GpsPoint> reliableGPSPoint = new ArrayList<>();
        //赋初始K个值
        for(int i = 0 ;i<k;i++)
            reliableGPSPoint.add(gpsPointArray.get(i));

        if(gpsPointArray.size()==4){
            return reliableGPSPoint;
        }else {
            //使reliableGPSPoint中的K个点是序列中最可靠的k个
            for (int i = k; i < gpsPointArray.size(); i++) {
                int maxIndex = findMaxIndex(reliableGPSPoint);
                if (reliableGPSPoint.get(maxIndex).getCount() > gpsPointArray.get(i).getCount())
                    reliableGPSPoint.set(maxIndex, gpsPointArray.get(i));
            }
            //将reliableGPSPoint中的K个点按增序（非降序）排列
            for (int i = 0; i < k - 1; i++)
                for (int j = 0; j < k - 1 - i; j++) {
                    if (reliableGPSPoint.get(j).getCount() > reliableGPSPoint.get(j + 1).getCount()) {
                        GpsPoint temp;
                        temp = reliableGPSPoint.get(j);
                        reliableGPSPoint.set(j, reliableGPSPoint.get(j + 1));
                        reliableGPSPoint.set(j + 1, temp);
                    }
                }

            return reliableGPSPoint;
        }
    }
    //找到count最大的点的下标
    public static int findMaxIndex(ArrayList<GpsPoint> gpsPointArray){
        int maxIndex = 0;
        int length = gpsPointArray.size();
        for(int i = 0;i<length;i++){
            if(gpsPointArray.get(i).getCount() > gpsPointArray.get(maxIndex).getCount())
                maxIndex = i;
        }
        return maxIndex;
    }
    //找到算出来的四个点里面误差最小的点的下标
    public static int findMinErrorPoint(ArrayList<PosEstimation> posEstimationArray){
        int minErrorIndex = 0;
        for(int i = 0; i < posEstimationArray.size();i++){
            if(posEstimationArray.get(i).getPosError() < posEstimationArray.get(minErrorIndex).getPosError())
                minErrorIndex = i;
        }
        return minErrorIndex;
    }

}
