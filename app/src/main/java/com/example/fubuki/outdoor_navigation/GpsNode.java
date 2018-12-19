package com.example.fubuki.outdoor_navigation;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;

import static java.lang.Double.NaN;
import static org.apache.commons.math3.linear.MatrixUtils.inverse;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;


public class GpsNode {
    //根据经纬度求两点距离时的系数
    public static final double t1 = 9222364766L;
    public static final double t2 = 12364993101L;
    private ArrayList<GpsPoint> gpsPointArray = new ArrayList<>();
    private ArrayList<GpsPoint> reliablePoint = new ArrayList<>();
    private Node loraNode = new Node();

    private static Node returnNode = new Node();

    private Node result = new Node();
    /*增加新的手机采样点*/
    public void addGpsPoint(GpsPoint newPoint)
    {
        gpsPointArray.add(newPoint);
    }
    public GpsPoint getGpsPoint(int index)
    {
        return gpsPointArray.get(index);
    }
    public static Node getReturnNode()
    {
        return returnNode;
    }
    private int getSize(){return  gpsPointArray.size();}

    public int getNodeNumber(){
        return gpsPointArray.size();
    }

    private static FileLogger mFileLogger;
    public GpsNode(boolean isLog){
        if(isLog) {
            mFileLogger = new FileLogger();
            mFileLogger.initData("iLocIteration");
        }
    }
    //基于三点，采用最小二乘法求解迭代初始点
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
            Log.e("leastSquare","奇异矩阵");
            return new Point((tmpGPSPointArr.get(0).getLongitude()+tmpGPSPointArr.get(1).getLongitude()+tmpGPSPointArr.get(2).getLongitude())/3,(tmpGPSPointArr.get(0).getLatitude()+tmpGPSPointArr.get(1).getLatitude()+tmpGPSPointArr.get(2).getLatitude())/3);
        }

    }

    //基于三点，采用牛顿迭代法求解待定位点
    public static Point newtonIteration(ArrayList<GpsPoint> tmpGPSPointArr){
        //迭代初始点
        double x = lsGetNodePoint(tmpGPSPointArr).getX();
        double y = lsGetNodePoint(tmpGPSPointArr).getY();
        //限制最多迭代50次
        int t;
        int max_iter;
        if(tmpGPSPointArr.get(0).getDistance()>45&&tmpGPSPointArr.get(1).getDistance()>45&&tmpGPSPointArr.get(2).getDistance()>45){
            max_iter=100;
        }
        else{
            max_iter=50;
        }
        for(t=0;t<max_iter;t++){
            /*x=x-obj(x,y,tmpGPSPointArr)/dfx1(x,y,tmpGPSPointArr);
            y=y-obj(x,y,tmpGPSPointArr)/dfy1(x,y,tmpGPSPointArr);*/
            x=x-dfx1(x,y,tmpGPSPointArr)/dfx2(x,y,tmpGPSPointArr);
            y=y-dfy1(x,y,tmpGPSPointArr)/dfy2(x,y,tmpGPSPointArr);
            //满足误差精度时结束循环
            if(obj(x,y,tmpGPSPointArr)<0.6){
                Log.e("leastSquare","当前组合的迭代次数:"+(t+1));
                Log.e("leastSquare","迭代结果的obj值:"+obj(x,y,tmpGPSPointArr));
                mFileLogger.writeTxtToFile("迭代次数"+(t+1)+"#"+"obj"+obj(x,y,tmpGPSPointArr),mFileLogger.getFilePath(),mFileLogger.getFileName());
                break;
            }
        }
        //一般迭代满50次时结果都不佳，舍去这个计算点
        if(t==max_iter){
            Log.e("leastSquare","迭代次数达到上限"+max_iter);
            Log.e("leastSquare","迭代到上限时的obj值:"+obj(x,y,tmpGPSPointArr));
            mFileLogger.writeTxtToFile("迭代次数"+max_iter+"#"+"obj"+obj(x,y,tmpGPSPointArr),mFileLogger.getFilePath(),mFileLogger.getFileName());
            if(obj(x,y,tmpGPSPointArr)<5){
                return new Point(x,y);
            }
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


    //基于最可靠的三个点计算位置
    public Point getNodePosition(){
        int number = gpsPointArray.size();  //手机采样点的数量
        loraNode.clear();                   //计算生成的节点队列
        if(number <=2)
        {
            return new Point(NaN,NaN);
        }
        IterationMethod mIterationMethod = new IterationMethod();
        //对gps序列点进行坏点标记
        mIterationMethod.markGPSPoint(gpsPointArray,60);
        if(number==3){
            //只有三个GPS点
            GpsPoint reliableGPSPoint1,reliableGPSPoint2,reliableGPSPoint3;
            //按count升序排列
            if(gpsPointArray.get(1).getCount()<gpsPointArray.get(0).getCount()){
                reliableGPSPoint1 = gpsPointArray.get(1);
                reliableGPSPoint2 = gpsPointArray.get(0);
            }else{
                reliableGPSPoint1 = gpsPointArray.get(0);
                reliableGPSPoint2 = gpsPointArray.get(1);
            }
            if(gpsPointArray.get(2).getCount()<reliableGPSPoint1.getCount()){
                reliableGPSPoint3 = reliableGPSPoint2;
                reliableGPSPoint2 = reliableGPSPoint1;
                reliableGPSPoint1 = gpsPointArray.get(2);
            }else if(gpsPointArray.get(2).getCount()<reliableGPSPoint2.getCount()){
                reliableGPSPoint3 = reliableGPSPoint2;
                reliableGPSPoint2 = gpsPointArray.get(2);
            }else{
                reliableGPSPoint3 = gpsPointArray.get(2);
            }
            reliablePoint.add(reliableGPSPoint1);
            reliablePoint.add(reliableGPSPoint2);
            reliablePoint.add(reliableGPSPoint3);
            Log.e("leastSquare","初始三个点"+reliableGPSPoint1.getLatitude()+"-"+reliableGPSPoint1.getLongitude());
            Log.e("leastSquare","初始三个点"+reliableGPSPoint2.getLatitude()+"-"+reliableGPSPoint2.getLongitude());
            Log.e("leastSquare","初始三个点"+reliableGPSPoint3.getLatitude()+"-"+reliableGPSPoint3.getLongitude());
            Point loraNode = newtonIteration(reliablePoint);//牛顿迭代法
            if(loraNode.getX()==NaN&&loraNode.getY()==NaN){
                return new Point(NaN,NaN);
            }
            returnNode.addPoint(loraNode);//显示在屏幕上
            return loraNode;
            //return new Point(NaN,NaN);
        }else{
            //gps点增多后，重新找到最可靠的三个点
            reliablePoint = mIterationMethod.newReliablePoint(reliablePoint,gpsPointArray);
            Log.e("leastSquare","最可靠三点的距离"+reliablePoint.get(0).getDistance()+"-"+reliablePoint.get(1).getDistance()+"-"+reliablePoint.get(2).getDistance());
            Log.e("leastSquare","最可靠三点的count值"+reliablePoint.get(0).getCount()+"-"+reliablePoint.get(1).getCount()+"-"+reliablePoint.get(2).getCount());
            mFileLogger.writeTxtToFile("可靠count"+reliablePoint.get(0).getCount()+"-"+reliablePoint.get(1).getCount()+"-"+reliablePoint.get(2).getCount()+"#"+"可靠距离"+reliablePoint.get(0).getDistance()+"-"+reliablePoint.get(1).getDistance()+"-"+reliablePoint.get(2).getDistance(),mFileLogger.getFilePath(),mFileLogger.getFileName());

            Point loraNode = newtonIteration(reliablePoint);
            if(loraNode.getX()==NaN&&loraNode.getY()==NaN){
                return new Point(NaN,NaN);
            }
            returnNode.addPoint(loraNode);//显示在屏幕上
            return loraNode;
        }
    }

}
