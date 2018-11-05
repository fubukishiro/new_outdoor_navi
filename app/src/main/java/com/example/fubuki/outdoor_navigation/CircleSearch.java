package com.example.fubuki.outdoor_navigation;

import java.util.ArrayList;

public class CircleSearch {
    private static final double angleIncrement = 3.0;
    public static Point search(Node nodeList,GpsPoint center)
    {
        ArrayList<Point> areaPointGroup = new ArrayList<>();
        int pointCount = nodeList.getSize();
        double radius = center.getDistance()/3;
        //遍历所有角度
        int[] numberInCircle = new int[(int)(360.0/angleIncrement)+1] ;
        System.out.println("angleNumber"+numberInCircle.length);
        for(int angle = 0;angle< 360; angle+=CircleSearch.angleIncrement)
        {
            double circleX = center.getLongitude()+center.getDistance()*Math.cos(angle/180.0*Math.PI)/GpsPoint.k1;
            double circleY = center.getLatitude()+center.getDistance()*Math.sin(angle/180.0*Math.PI)/GpsPoint.k2;
            for (int i = 0; i < pointCount; i++)
            {
                Point temp = nodeList.getPoint(i);
                double sum = (temp.getX()-circleX)*(temp.getX()-circleX)*GpsPoint.k1*GpsPoint.k1+(temp.getY()-circleY)*(temp.getY()-circleY)*GpsPoint.k2*GpsPoint.k2;
                if(radius<=5.0)
                {
                    radius = 5.0;
                }
                if(sum <= radius*radius)
                {
                    //    System.out.println("Get Point");
                    int position = angle/(int)CircleSearch.angleIncrement;
                    numberInCircle[position]++;
                    //  System.out.println("Position "+position);
                }
            }
        }
        //找到最大圆区间
        int maxNumberInCircle = numberInCircle[0];
        int anglePosition = 0;
        for(int angle = 0;angle< 360; angle+=CircleSearch.angleIncrement)
        {
            if(numberInCircle[angle/(int)CircleSearch.angleIncrement]>=maxNumberInCircle)
            {
                anglePosition = angle/(int)CircleSearch.angleIncrement;
                maxNumberInCircle = numberInCircle[anglePosition];
                System.out.println("maxNumberInCircle in Circle"+maxNumberInCircle);
            }
        }
        //找到最大圆区间的点
        double circleX = center.getLongitude()+center.getDistance()*Math.cos(anglePosition*CircleSearch.angleIncrement/180.0*Math.PI)/GpsPoint.k1;
        double circleY = center.getLatitude()+center.getDistance()*Math.sin(anglePosition*CircleSearch.angleIncrement/180.0*Math.PI)/GpsPoint.k2;
        System.out.println("pointCount"+pointCount);
        for (int i = 0; i < pointCount; i++)
        {
            Point temp = nodeList.getPoint(i);
            double sum = (temp.getX()-circleX)*(temp.getX()-circleX)*GpsPoint.k1*GpsPoint.k1+(temp.getY()-circleY)*(temp.getY()-circleY)*GpsPoint.k2*GpsPoint.k2;
            if(sum <= radius*radius)
            {
                System.out.println("sum"+sum);
                System.out.println("pRadius"+radius*radius);
                areaPointGroup.add(new Point(temp.getX(),temp.getY()));
                System.out.println("Add result Lora Point");
            }
        }
        int resultLength = areaPointGroup.size();
        System.out.println("resultLength "+resultLength);
        double xSum = 0;
        double ySum = 0;
        for(int i=0;i<resultLength;i++)
        {
            Point temp = areaPointGroup.get(i);
            xSum += temp.getX();
            ySum += temp.getY();
        }
        xSum = xSum/resultLength;
        ySum = ySum/resultLength;
        Point result = new Point(xSum,ySum);
        return  result;
    }
}