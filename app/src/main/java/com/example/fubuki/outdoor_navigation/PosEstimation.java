package com.example.fubuki.outdoor_navigation;

public class PosEstimation {
    private Point estimationPos;//估计位置
    private double obj;         //估计误差

    PosEstimation(Point pEstimationPos,double pObj){
        estimationPos=pEstimationPos;
        obj=pObj;
    }

    public Point getEstimationPos() {
        return estimationPos;
    }
    public double getPosError(){
        return obj;
    }
}
