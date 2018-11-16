package com.example.fubuki.outdoor_navigation;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;
import java.util.List;

public class MyUtil {
    public static double calculateAngle(double p1X,double p1Y, double p2X, double p2Y){
        //X:latitude; Y:longitude
        //p2:current
        LatLng p1 = new LatLng(p1X, p1Y);
        LatLng p2 = new LatLng(p2X, p2Y);
        LatLng p3 = new LatLng(p1X-p2X,p1Y);

        //此处调用百度地图API获取两个点之间的距离，单位是米
        double r1 = DistanceUtil.getDistance(p1,p2);
        double r2 = DistanceUtil.getDistance(p1,p3);
        double cosa = r1/r2;
        double angle = 0;
        if(((p1X >= p2X) && (p1Y>=p2Y) ))        //1
        {
            angle = Math.acos(cosa);
        }
        else if((p1X >= p2X) && (p1Y < p2Y))   //2
        {
            angle = Math.PI - Math.acos(cosa);
        }
        else if((p1X < p2X) && (p1Y < p2Y))   //3
        {
            angle = Math.PI + Math.acos(cosa);
        }
        else if((p1X < p2X) && (p1Y >= p2Y))   //4
        {
            angle = 2 * Math.PI - Math.acos(cosa);
        }else{
            angle = 0;
        }
        return angle;
    }

    public static boolean judgeTrend(GpsNode gpsNodeSet, double rcvDis){
        int currentNum = gpsNodeSet.getNodeNumber();
        double d1 = gpsNodeSet.getGpsPoint(currentNum - 1).getDistance();
        double d2 = gpsNodeSet.getGpsPoint(currentNum - 2).getDistance();
        double d3 = gpsNodeSet.getGpsPoint(currentNum - 3).getDistance();
        double d4 = gpsNodeSet.getGpsPoint(currentNum - 4).getDistance();

        //Log.e("distance judge","distance is:"+d1+"#"+d2+"#"+d3+"#"+d4);
        int ascendCount = 0;
        if(d1 - d2 > 0)
            ascendCount++;
        if(d2 - d3 > 0)
            ascendCount++;
        if(d3 - d4 > 0)
            ascendCount++;

        if(ascendCount > 1)
            return true;
        else
            return false;
    }

    public static boolean judgeTrend2(List<Double> distanceArray){
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

    public static double calculateAngle(GpsPoint currentPoint, Point node){
        //以point1为相对原点
        LatLng p1 = new LatLng(currentPoint.getLatitude(),currentPoint.getLongitude());
        LatLng p2 = new LatLng(node.getY(),node.getX());
        LatLng p3 = new LatLng(Math.abs(node.getY()-currentPoint.getLatitude()),currentPoint.getLongitude()); //用于计算d1

        double realD1 = DistanceUtil.getDistance(p1,p3);
        double realD3 = DistanceUtil.getDistance(p1,p2); //用百度api计算

        double d1 = currentPoint.getLatitude() - node.getY();
        double d2 = currentPoint.getLongitude() - node.getX();

        double angle;
        double cosa = Math.abs(realD1/realD3);
        if(cosa >= 0.999999999)
        {
            cosa = 0.99999999;
        }
        if(cosa <= -0.999999999)
        {
            cosa = -0.99999999;
        }

        angle = Math.acos(cosa)*180/Math.PI;

        //三象限
        if(d1 < 0 && d2 > 0){
            angle += 90;
        }

        //四象限
        if(d1 < 0 && d2 < 0){
            angle += 180;
        }

        //二象限
        if(d1 > 0 && d2 < 0){
            angle = 360 - angle;
        }

        return angle;
    }


    public static int calculateIntersectionOfRings(GpsPoint p1, GpsPoint p2, double e){
        //e为d的误差
        //外环相切
        LatLng temp1 = new LatLng(p1.getLatitude(), p1.getLongitude());
        LatLng temp2 = new LatLng(p2.getLatitude(), p2.getLongitude());
        double centerDistance = DistanceUtil.getDistance(temp1,temp2);
        double d1 = p1.getDistance(),
               d2 = p2.getDistance();
        //外切不相交
        if(centerDistance > (d1 + d2 + 2*e)){
            return 0;
        }

        //外切
        if(centerDistance == (d1 + d2 + 2*e)){
            return 1;
        }

        //内切不相交
        if(d1 > d2){
            if(centerDistance < d1 - d2 - 2*e)
                return 0;
        }else if(d1 < d2){
            if(centerDistance < d2 - d1 - 2*e)
                return 0;
        }

        //内切
        if(d1 > d2){
            if(centerDistance == d1 - d2 - 2*e)
                return 1;
        }else if(d1 < d2){
            if(centerDistance == d2 - d1 - 2*e)
                return 1;
        }

        if(centerDistance == d1 + d2)
            return 2;

        if((centerDistance > d1 + d2) && (centerDistance < d1 + d2 + 2*e)){
            return 2;
        }


        return 0;
    }
}
