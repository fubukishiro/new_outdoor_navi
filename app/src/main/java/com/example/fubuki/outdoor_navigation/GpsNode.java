package com.example.fubuki.outdoor_navigation;

import android.location.GpsStatus;
import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;

import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static org.apache.commons.math3.linear.MatrixUtils.inverse;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;


public class GpsNode {

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


    //基于最可靠的四个点（初始三个点）计算可能的节点位置（还返回了误差和相应的三个计算点）
    public ArrayList<PosEstimation> getNodePosition(GpsPoint currentGpsPoint){
        ArrayList<PosEstimation> posEstimationArray = new ArrayList<PosEstimation>();
        int number = gpsPointArray.size();  //手机采样点的数量
        double posError = NaN;
        loraNode.clear();                   //计算生成的节点队列
        if(number <=2)
        {
            posEstimationArray.add(new PosEstimation(new Point(NaN,NaN),NaN,new ArrayList<GpsPoint>()));
            return posEstimationArray;
        }
        //对gps序列点进行坏点标记
        IterationMethod.markGPSPoint(gpsPointArray,60);
        if(number==3){
            //只有三个GPS点时，按count升序排列
            GpsPoint reliableGPSPoint1,reliableGPSPoint2,reliableGPSPoint3;
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
            //估计位置
            Point loraNode = IterationMethod.newtonIteration(reliablePoint);
            if(isNaN(loraNode.getX())&&isNaN(loraNode.getY())){
                posEstimationArray.add(new PosEstimation(new Point(NaN,NaN),NaN,new ArrayList<GpsPoint>()));
                return posEstimationArray;
            }
            //估计误差
            double dis1 =  DistanceUtil.getDistance(new LatLng(reliableGPSPoint1.getLatitude(),reliableGPSPoint1.getLongitude()),new LatLng(loraNode.getY(),loraNode.getX()));
            double dis2 =  DistanceUtil.getDistance(new LatLng(reliableGPSPoint2.getLatitude(),reliableGPSPoint2.getLongitude()),new LatLng(loraNode.getY(),loraNode.getX()));
            double dis3 =  DistanceUtil.getDistance(new LatLng(reliableGPSPoint3.getLatitude(),reliableGPSPoint3.getLongitude()),new LatLng(loraNode.getY(),loraNode.getX()));
            posError = Math.abs(reliableGPSPoint1.getDistance()-dis1) + Math.abs(reliableGPSPoint2.getDistance()-dis2) + Math.abs(reliableGPSPoint3.getDistance()-dis3);
            returnNode.addPoint(loraNode);//显示在屏幕上
            posEstimationArray.add(new PosEstimation(loraNode,posError,reliablePoint));
            return posEstimationArray;
        }else{
            //gps点增多后，找到最可靠的四个点
            reliablePoint = MyUtil.findMinK(gpsPointArray,4);
            //四个点组合计算出四个可能的目标位置
            posEstimationArray.add(getPosEstimation(reliablePoint.get(0),reliablePoint.get(1),reliablePoint.get(2)));
            posEstimationArray.add(getPosEstimation(reliablePoint.get(0),reliablePoint.get(1),reliablePoint.get(3)));
            posEstimationArray.add(getPosEstimation(reliablePoint.get(0),reliablePoint.get(2),reliablePoint.get(3)));
            posEstimationArray.add(getPosEstimation(reliablePoint.get(1),reliablePoint.get(2),reliablePoint.get(3)));
            return posEstimationArray;
        }
    }

    //基于可靠的三个点计算位置与误差
    private static PosEstimation getPosEstimation(GpsPoint point1,GpsPoint point2,GpsPoint point3){
        ArrayList<GpsPoint> calculatedPoint = new ArrayList<>();
        double posError = NaN;
        calculatedPoint.add(point1);
        calculatedPoint.add(point2);
        calculatedPoint.add(point3);
        Point loraNode = IterationMethod.newtonIteration(calculatedPoint);
        if(isNaN(loraNode.getX())&&isNaN(loraNode.getY())){
            return new PosEstimation(new Point(NaN,NaN),NaN,new ArrayList<GpsPoint>());
        }
        double dis1 =  DistanceUtil.getDistance(new LatLng(calculatedPoint.get(0).getLatitude(),calculatedPoint.get(0).getLongitude()),new LatLng(loraNode.getY(),loraNode.getX()));
        double dis2 =  DistanceUtil.getDistance(new LatLng(calculatedPoint.get(1).getLatitude(),calculatedPoint.get(1).getLongitude()),new LatLng(loraNode.getY(),loraNode.getX()));
        double dis3 =  DistanceUtil.getDistance(new LatLng(calculatedPoint.get(2).getLatitude(),calculatedPoint.get(2).getLongitude()),new LatLng(loraNode.getY(),loraNode.getX()));
        posError = Math.abs(calculatedPoint.get(0).getDistance()-dis1) + Math.abs(calculatedPoint.get(1).getDistance()-dis2) + Math.abs(calculatedPoint.get(2).getDistance()-dis3);
        returnNode.addPoint(loraNode);//显示在屏幕上
        return new PosEstimation(loraNode,posError,calculatedPoint);
    }
}
