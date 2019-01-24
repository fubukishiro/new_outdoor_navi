package com.example.fubuki.outdoor_navigation;

import java.util.ArrayList;

public class PosEstimation {
    private Point estimationPos;//估计位置
    private double obj;         //估计误差
    private ArrayList<GpsPoint> reliablePoint = new ArrayList<>();
    PosEstimation(Point pEstimationPos,double pObj,ArrayList<GpsPoint> pReliablePoint){
        estimationPos=pEstimationPos;
        obj=pObj;
        reliablePoint = pReliablePoint;
    }

    public Point getEstimationPos() {
        return estimationPos;
    }
    public double getPosError(){
        return obj;
    }
    public ArrayList<GpsPoint> getReliablePoint() {
        return reliablePoint;
    }
}
