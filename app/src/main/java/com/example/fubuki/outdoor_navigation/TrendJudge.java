package com.example.fubuki.outdoor_navigation;

import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TrendJudge {
    private static int rssiJudgeSize = 7; //rssi判断中所采用的序列中的rssi值的个数
    private static int distanceJudgeSize = 5;
    private static List<Integer> rssiLostCount = new ArrayList<Integer>(); //rssi序列中前若干个是NaN的个数的序列

    /**
     * 判断当前趋势状态
     * @param rssiArray 接收到的rssi序列
     * @param distanceArray 接收到的距离序列
     * @return 0:状态未知;1:从有距离的地方走到没有距离的地方;2:收不到距离，但能收到rssi时，逐渐远离目标;3:收得到距离，逐渐远离目标
     */
    public static int trendStatusJudge(List<Integer> rssiArray, List<Double> distanceArray){
        int distanceSize = distanceArray.size();
        int rssiSize = rssiArray.size();
        //从有距离走到没有距离的判断
        if(countDisNanNumber(distanceArray) >2)
            return 1;

        if(Double.isNaN(distanceArray.get(distanceSize-1)) && rssiSize > rssiJudgeSize && rssiArray.get(rssiSize-1) < 0){
            //收不到距离，但能收到rssi时的距离判断
            rssiLostCount.add(countRssiNanNumber(rssiArray));
            if(judgeRssiCountTrend(rssiLostCount)) {
                return 2;
            }
        }else{
            //能收到距离时的趋势判断
            if(distanceSize > 5 && judgeDistanceTrend(distanceArray))
                return 3;
        }
        return 0;
    }

    /**
     * 判断rssi序列中NaN个数的变化趋势
     * @param rssiCountArray 每一次rssi序列中前若干个数中为NaN的个数的序列
     * @return true:有增加的趋势;false:无明显变化趋势
     */
    private static boolean judgeRssiCountTrend(List<Integer> rssiCountArray){
        int currentNum = rssiCountArray.size();
        int ascendCount = 0;
        for(int i = currentNum - 1 ;i > currentNum - rssiJudgeSize;i--){
            int temp1 = rssiCountArray.get(i);
            int temp2 = rssiCountArray.get(i-1);

            if((temp1 - temp2 >= 0)&&(temp2 != 0))
                ascendCount++;

        }
        if(ascendCount > Math.floor(rssiJudgeSize/2))
            return true;
        else
            return false;
    }

    /**
     * 判断距离变化趋势
     * @param distanceArray 接收到的距离序列
     * @return true:有增加的趋势;false:无明显变化趋势
     */
    private static boolean judgeDistanceTrend(List<Double> distanceArray){
        int currentNum = distanceArray.size();

        int ascendCount = 0;
        for(int i = currentNum - 1 ;i > currentNum - distanceJudgeSize;i--){
            double temp1 = distanceArray.get(i);
            double temp2 = distanceArray.get(i-1);

            if((temp1 - temp2 >= 0)&&(temp2 != 0))
                ascendCount++;

        }
        if(ascendCount > Math.floor(distanceJudgeSize/2))
            return true;
        else
            return false;
    }

    /**
     * 计算rssi序列中前若干个数中NaN的个数
     * @param rssiArray 接收到的rssi序列
     * @return NaN个数
     */
    private static int countRssiNanNumber(List<Integer> rssiArray){
        int currentNum = rssiArray.size();

        int nanCount = 0;
        for(int i = currentNum - 1 ;i > currentNum - 8;i--){
            int temp = rssiArray.get(i);
            if(temp == 0)
                nanCount++;
        }

        return nanCount;
    }

    /**
     * 计算距离序列中前若干个数中NaN的个数
     * @param rcvDisArray 接收到的距离序列
     * @return NaN个数
     */
    private static int countDisNanNumber(List<Double> rcvDisArray){
        int currentNum = rcvDisArray.size();

        int nanCount = 0;
        for(int i = currentNum - 1 ;i > currentNum - 4;i--){
            double temp = rcvDisArray.get(i);
            if(temp == 0)
                nanCount++;

        }

        return nanCount;
    }
}
