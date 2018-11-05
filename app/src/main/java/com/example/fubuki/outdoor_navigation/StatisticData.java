package com.example.fubuki.outdoor_navigation;

import java.util.ArrayList;

public class StatisticData {
    private double xMean = 0;
    private double yMean = 0;
    private double xVariance = 0;
    private double yVariance = 0;

    private void calculateMean(ArrayList<Point> point){
        double sumX = 0, sumY = 0;
        for(int i = 0; i < point.size();i++){
            sumX += point.get(i).getX();
            sumY += point.get(i).getY();
        }
        xMean = sumX / point.size();
        yMean = sumY / point.size();
    }

    private void calculateVariance(ArrayList<Point> point){
        double sumX = 0, sumY = 0;
        for(int i = 0; i < point.size();i++){
            sumX += Math.pow(point.get(i).getX() - xMean,2);
            sumY += Math.pow(point.get(i).getY() - yMean,2);
        }
        xVariance = sumX / point.size();
        yVariance = sumY / point.size();
    }


    public double[] getStatisticData(ArrayList<Point> point){
        this.calculateMean(point);
        this.calculateVariance(point);
        double statisticData[] = {xMean, yMean, xVariance, yVariance, xVariance+yVariance};

        return statisticData;
    }
}
