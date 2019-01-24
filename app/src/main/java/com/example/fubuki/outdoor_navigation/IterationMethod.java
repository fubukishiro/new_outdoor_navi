package com.example.fubuki.outdoor_navigation;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.ArrayList;

import static java.lang.Double.NaN;
import static org.apache.commons.math3.linear.MatrixUtils.inverse;

public class IterationMethod {
    //根据经纬度求两点距离时的系数
    public static final double t1 = 9222364766L;
    public static final double t2 = 12364993101L;


    //基于三个GPS点，采用最小二乘法求解迭代初始点
    public static Point lsGetNodePoint(ArrayList<GpsPoint> tmpGPSPointArr){
        int rowNumber=tmpGPSPointArr.size()-1;
        double matrixA[][] = new double[rowNumber][2];
        double matrixB[][] = new double[rowNumber][1];
        for(int t=0;t<rowNumber;t++){
            matrixA[t][0] = 2*t1*(tmpGPSPointArr.get(rowNumber).getLongitude()-tmpGPSPointArr.get(t).getLongitude());
            matrixA[t][1] = 2*t2*(tmpGPSPointArr.get(rowNumber).getLatitude()-tmpGPSPointArr.get(t).getLatitude());
            matrixB[t][0] = t1*(Math.pow(tmpGPSPointArr.get(rowNumber).getLongitude(),2)-Math.pow(tmpGPSPointArr.get(t).getLongitude(),2))+t2*(Math.pow(tmpGPSPointArr.get(rowNumber).getLatitude(),2)-Math.pow(tmpGPSPointArr.get(t).getLatitude(),2));
            matrixB[t][0] = matrixB[t][0]+Math.pow(tmpGPSPointArr.get(t).getDistance(),2)-Math.pow(tmpGPSPointArr.get(rowNumber).getDistance(),2);
        }
        RealMatrix matrixA1 = new Array2DRowRealMatrix(matrixA);
        RealMatrix matrixB1 = new Array2DRowRealMatrix(matrixB);
        RealMatrix temp = matrixA1.transpose();
        temp = temp.multiply(matrixA1);
        try{
            temp = inverse(temp).multiply(matrixA1.transpose());
            temp = temp.multiply(matrixB1);
            double point[][] = temp.getData();
            return new Point(point[0][0],point[1][0]);
        }catch(Exception e){
            //出现奇异矩阵时，选择当前三点的均值作为迭代初始点
            return new Point((tmpGPSPointArr.get(0).getLongitude()+tmpGPSPointArr.get(1).getLongitude()+tmpGPSPointArr.get(2).getLongitude())/3,(tmpGPSPointArr.get(0).getLatitude()+tmpGPSPointArr.get(1).getLatitude()+tmpGPSPointArr.get(2).getLatitude())/3);
        }

    }

    //基于三个GPS点，采用牛顿迭代法求解待定位点
    public static Point newtonIteration(ArrayList<GpsPoint> tmpGPSPointArr){
        //迭代初始点
        double x = lsGetNodePoint(tmpGPSPointArr).getX();
        double y = lsGetNodePoint(tmpGPSPointArr).getY();
        //动态的迭代次数上限
        int max_iter;
        if(tmpGPSPointArr.get(0).getDistance()>45&&tmpGPSPointArr.get(1).getDistance()>45&&tmpGPSPointArr.get(2).getDistance()>45){
            max_iter=100;
        }
        else{
            max_iter=50;
        }
        int t;
        for(t=0;t<max_iter;t++){
            //牛顿迭代公式
            x=x-dfx1(x,y,tmpGPSPointArr)/dfx2(x,y,tmpGPSPointArr);
            y=y-dfy1(x,y,tmpGPSPointArr)/dfy2(x,y,tmpGPSPointArr);
            //满足误差精度时结束循环
            if(obj(x,y,tmpGPSPointArr)<0.6){
                break;
            }
        }
        //迭代满次数时
        if(t==max_iter){
            //结果还凑活，保留这个计算点
            if(obj(x,y,tmpGPSPointArr)<5){
                return new Point(x,y);
            }
            //结果不佳，舍去这个计算点，返回NaN
            else{
                return new Point(NaN,NaN);
            }

        }
        return new Point(x,y);

    }
    //牛顿迭代法的目标函数
    public static double obj(double x, double y, ArrayList<GpsPoint> GPSPointArr){
        double x1 = GPSPointArr.get(0).getLongitude();
        double y1 = GPSPointArr.get(0).getLatitude();
        double d1 = GPSPointArr.get(0).getDistance();
        double x2 = GPSPointArr.get(1).getLongitude();
        double y2 = GPSPointArr.get(1).getLatitude();
        double d2 = GPSPointArr.get(1).getDistance();
        double x3 = GPSPointArr.get(2).getLongitude();
        double y3 = GPSPointArr.get(2).getLatitude();
        double d3 = GPSPointArr.get(2).getDistance();
        double obj = 0.5*(Math.pow((Math.sqrt(t1*Math.pow((x-x1),2)+t2*Math.pow((y-y1),2))-d1),2)+Math.pow((Math.sqrt(t1*Math.pow((x-x2),2)+t2*Math.pow((y-y2),2))-d2),2)+Math.pow((Math.sqrt(t1*Math.pow((x-x3),2)+t2*Math.pow((y-y3),2))-d3),2));
        return obj;
    }
    //目标函数对x的一阶偏导
    public static double dfx1(double x, double y, ArrayList<GpsPoint> GPSPointArr){
        double x1 = GPSPointArr.get(0).getLongitude();
        double y1 = GPSPointArr.get(0).getLatitude();
        double d1 = GPSPointArr.get(0).getDistance();
        double x2 = GPSPointArr.get(1).getLongitude();
        double y2 = GPSPointArr.get(1).getLatitude();
        double d2 = GPSPointArr.get(1).getDistance();
        double x3 = GPSPointArr.get(2).getLongitude();
        double y3 = GPSPointArr.get(2).getLatitude();
        double d3 = GPSPointArr.get(2).getDistance();
        double dfx1 = - (t1*(2*x - 2*x1)*(d1 - Math.sqrt(t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2))))/(2*Math.sqrt(t1*Math.pow((x - x1),2) +t2*Math.pow((y - y1),2))) - (t1*(2*x - 2*x2)*(d2 - Math.sqrt(t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2))))/(2*Math.sqrt(t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2))) - (t1*(2*x - 2*x3)*(d3 - Math.sqrt(t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2))))/(2*Math.sqrt(t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2)));
        return dfx1;
    }
    //目标函数对y的一阶偏导
    public static double dfy1(double x, double y, ArrayList<GpsPoint> GPSPointArr){
        double x1 = GPSPointArr.get(0).getLongitude();
        double y1 = GPSPointArr.get(0).getLatitude();
        double d1 = GPSPointArr.get(0).getDistance();
        double x2 = GPSPointArr.get(1).getLongitude();
        double y2 = GPSPointArr.get(1).getLatitude();
        double d2 = GPSPointArr.get(1).getDistance();
        double x3 = GPSPointArr.get(2).getLongitude();
        double y3 = GPSPointArr.get(2).getLatitude();
        double d3 = GPSPointArr.get(2).getDistance();
        double dfy1 = - (t2*(2*y - 2*y1)*(d1 - Math.sqrt(t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2))))/(2*Math.sqrt(t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2))) - (t2*(2*y - 2*y2)*(d2 - Math.sqrt(t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2))))/(2*Math.sqrt(t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2))) - (t2*(2*y - 2*y3)*(d3 - Math.sqrt(t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2))))/(2*Math.sqrt(t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2)));
        return dfy1;
    }
    //目标函数对x的二阶偏导
    public static double dfx2(double x, double y, ArrayList<GpsPoint> GPSPointArr){
        double x1 = GPSPointArr.get(0).getLongitude();
        double y1 = GPSPointArr.get(0).getLatitude();
        double d1 = GPSPointArr.get(0).getDistance();
        double x2 = GPSPointArr.get(1).getLongitude();
        double y2 = GPSPointArr.get(1).getLatitude();
        double d2 = GPSPointArr.get(1).getDistance();
        double x3 = GPSPointArr.get(2).getLongitude();
        double y3 = GPSPointArr.get(2).getLatitude();
        double d3 = GPSPointArr.get(2).getDistance();
        double dfx2 = (Math.pow(t1,2)*Math.pow((2*x - 2*x1),2))/(4*(t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2)))
                +(Math.pow(t1,2)*Math.pow((2*x - 2*x2),2))/(4*(t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2)))
                +(Math.pow(t1,2)*Math.pow((2*x - 2*x3),2))/(4*(t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2)))
                -(t1*(d1 - Math.sqrt(t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2))))/Math.sqrt(t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2))
                -(t1*(d2 - Math.sqrt(t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2))))/Math.sqrt(t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2))
                -(t1*(d3 - Math.sqrt(t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2))))/Math.sqrt(t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2))
                +(Math.pow(t1,2)*Math.pow((2*x - 2*x1),2)*(d1 - Math.sqrt(t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2))))/(4*Math.sqrt(Math.pow((t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2)),3)))
                +(Math.pow(t1,2)*Math.pow((2*x - 2*x2),2)*(d2 - Math.sqrt(t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2))))/(4*Math.sqrt(Math.pow((t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2)),3)))
                +(Math.pow(t1,2)*Math.pow((2*x - 2*x3),2)*(d3 - Math.sqrt(t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2))))/(4*Math.sqrt(Math.pow((t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2)),3)));
        return dfx2;
    }
    //目标函数对y的二阶偏导
    public static double dfy2(double x, double y, ArrayList<GpsPoint> GPSPointArr){
        double x1 = GPSPointArr.get(0).getLongitude();
        double y1 = GPSPointArr.get(0).getLatitude();
        double d1 = GPSPointArr.get(0).getDistance();
        double x2 = GPSPointArr.get(1).getLongitude();
        double y2 = GPSPointArr.get(1).getLatitude();
        double d2 = GPSPointArr.get(1).getDistance();
        double x3 = GPSPointArr.get(2).getLongitude();
        double y3 = GPSPointArr.get(2).getLatitude();
        double d3 = GPSPointArr.get(2).getDistance();
        double dfy2 = (Math.pow(t2,2)*Math.pow((2*y - 2*y1),2))/(4*(t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2)))
                +(Math.pow(t2,2)*Math.pow((2*y - 2*y2),2))/(4*(t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2)))
                +(Math.pow(t2,2)*Math.pow((2*y - 2*y3),2))/(4*(t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2)))
                -(t2*(d1 - Math.sqrt(t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2))))/Math.sqrt(t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2))
                -(t2*(d2 - Math.sqrt(t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2))))/Math.sqrt(t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2))
                -(t2*(d3 - Math.sqrt(t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2))))/Math.sqrt(t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2))
                +(Math.pow(t2,2)*Math.pow((2*y - 2*y1),2)*(d1 - Math.sqrt(t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2))))/(4*Math.sqrt(Math.pow((t1*Math.pow((x - x1),2) + t2*Math.pow((y - y1),2)),3)))
                +(Math.pow(t2,2)*Math.pow((2*y - 2*y2),2)*(d2 - Math.sqrt(t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2))))/(4*Math.sqrt(Math.pow((t1*Math.pow((x - x2),2) + t2*Math.pow((y - y2),2)),3)))
                +(Math.pow(t2,2)*Math.pow((2*y - 2*y3),2)*(d3 - Math.sqrt(t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2))))/(4*Math.sqrt(Math.pow((t1*Math.pow((x - x3),2) + t2*Math.pow((y - y3),2)),3)));
        return dfy2;
    }


    public static void markGPSPoint(ArrayList<GpsPoint> gpsPointArray,double thre){
        ArrayList<GpsPoint> tmpGPSPointArr = new ArrayList<>();
        //将新采的这个点 与 前面序列点中的任意两点组合
        tmpGPSPointArr.add(gpsPointArray.get(gpsPointArray.size()-1));
        for(int i=0;i<gpsPointArray.size()-2;i++){
            tmpGPSPointArr.add(gpsPointArray.get(i));
            for(int j=i+1;j<gpsPointArray.size()-1;j++){
                tmpGPSPointArr.add(gpsPointArray.get(j));
                Point tempBest = newtonIteration(tmpGPSPointArr);//牛顿迭代法
                LatLng p1 = new LatLng(tempBest.getY(),tempBest.getX());
                double totalDiff = 0;
                for(int t=0;t<3;t++){
                    LatLng p2 = new LatLng(tmpGPSPointArr.get(t).getLatitude(),tmpGPSPointArr.get(t).getLongitude());
                    totalDiff = totalDiff+Math.abs(DistanceUtil.getDistance(p1,p2)-tmpGPSPointArr.get(t).getDistance());
                }
                //误差（三个delta d 的绝对值的和)超过阈值时打坏点标记
                if(totalDiff>thre){
                    double totalDis = tmpGPSPointArr.get(0).getDistance()+tmpGPSPointArr.get(1).getDistance()+tmpGPSPointArr.get(2).getDistance();
                    //考虑距离越大时本身误差就会越大
                    for(int t=0;t<3;t++){
                        double count = tmpGPSPointArr.get(t).getDistance()/totalDis;
                        tmpGPSPointArr.get(t).addCount(count);
                        }
                }
            }
        }
    }


}
