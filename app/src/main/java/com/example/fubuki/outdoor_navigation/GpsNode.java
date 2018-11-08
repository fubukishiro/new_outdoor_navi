package com.example.fubuki.outdoor_navigation;

import android.util.Log;

import java.util.ArrayList;

import static java.lang.Double.NaN;

public class GpsNode {
    private ArrayList<GpsPoint> gpsPointArray = new ArrayList<>();
    private Node loraNode = new Node();

    private Node returnNode = new Node();

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
    private int getSize(){return  gpsPointArray.size();}
    /*从手机采样点计算获取LoRa节点*/
    public static Point[] getNodePoint(GpsPoint firstPoint,GpsPoint secondPoint)
    {
        Point[] result = firstPoint.calculateNode(secondPoint);
        return result;
    }
    /*找出真实的节点*/
    public Point getNodePosition()
    {
        int number = gpsPointArray.size();  //手机采样点的数量
        //System.out.println("Gps Point number "+number);
        loraNode.clear();                   //计算生成的节点队列
        if(number <=2)
        {
            return new Point(NaN,NaN);
        }
        /*for(int i=1;i<number;i++)
        {
            Point temp[] = getNodePoint(gpsPointArray.get(i-1), gpsPointArray.get(i));
            loraNode.addPoint(temp[0]);
            loraNode.addPoint(temp[1]);
            Log.e("gpsnode:","第"+i+"个对称点："+temp[0].getY()+"/"+temp[0].getX()+"#"+temp[1].getY()+"/"+temp[1].getX());
        }*/
        for(int i=1;i<number;i++)
        {
            for(int j=i;j<number-1;j++)
            {
                Point temp[] = getNodePoint(gpsPointArray.get(i - 1), gpsPointArray.get(j));
                loraNode.addPoint(temp[0]);
                loraNode.addPoint(temp[1]);
                Log.e("gpsnode:", "第" + i + "个对称点：" + temp[0].getY() + "/" + temp[0].getX() + "#" + temp[1].getY() + "/" + temp[1].getX());
            }
        }
        //System.out.println("loraNode number "+loraNode.getSize());
        int balance = 0;
        for(int i=1;i<number;i++)
        {
            System.out.println("Lora Node Size "+loraNode.getSize());
            if(this.getLineSide(this.getGpsPoint(0),this.getGpsPoint(i),loraNode)==false)
            {
                balance++;
            }
        }
        if (balance == number-1)
        {
            return new Point(NaN,NaN);
        }
        for(int i=0;i<loraNode.getSize();i++)
        {
            System.out.println("Point:"+"X"+loraNode.getPoint(i).getX()+" Y"+loraNode.getPoint(i).getY());
        }
        returnNode = loraNode;
        return CircleSearch.search(loraNode,this.getGpsPoint(this.getSize()-1));
        //return loraNode.getNodeLocation();
    }
   /* public Point getNodePosition()
    {
        Node temp = getPoints();
        return CircleSearch.search(temp,this.getGpsPoint(this.getSize()-1));
    }*/
    boolean getLineSide(GpsPoint p1,GpsPoint p2,Node nodeList)
    {
        int number = nodeList.getSize();
        int upCount = 0;
        int downCount = 0;
        for(int i=0;i<number;i++)
        {
            Point temp = nodeList.getPoint(i);
            if (GpsPoint.GetLineSide(p1,p2,temp))
            {
                System.out.println("Add up");
                upCount++;
            }
            else
            {
                System.out.println("Add down");
                downCount++;
            }
        }
        if(upCount>downCount)
        {
            for(int k=0;k<nodeList.getSize();k++)
            {
                Point temp = nodeList.getPoint(k);
                if (GpsPoint.GetLineSide(p1,p2,temp)==false)
                {
                    nodeList.removePoint(temp);
                }
            }
            return  true;
        }
        else if(upCount<downCount)
        {
            for(int k=0;k<nodeList.getSize();k++)
            {
                Point temp = nodeList.getPoint(k);
                if (GpsPoint.GetLineSide(p1,p2,temp))
                {
                    nodeList.removePoint(temp);
                }
            }
            return true;
        }
        return false;
    }
    public int getNodeNumber(){
        return gpsPointArray.size();
    }

    public Node getNodeTest(){
        return returnNode;
    }

    public Node getPoints()
    {
        Node tempNode = result.copy();
        int gpsNumber = gpsPointArray.size();
        if(gpsNumber == 2)
        {
            Point temp[] = getNodePoint(gpsPointArray.get(0), gpsPointArray.get(1));
            result.addPoint(temp[0]);
            result.addPoint(temp[1]);
            return result;
        }
        Point temp[] = getNodePoint(gpsPointArray.get(gpsNumber-2), gpsPointArray.get(gpsNumber-1));
        tempNode.addPoint(temp[0]);
        tempNode.addPoint(temp[1]);
        int up = 0;
        int down = 0;
        for(int i=0;i<tempNode.getSize();i++)
        {
            if(GpsPoint.GetLineSide(gpsPointArray.get(gpsNumber-2), gpsPointArray.get(gpsNumber-1),tempNode.getPoint(i)))
            {
                up++;
            }
            else
            {
                down++;
            }
        }
        if(up>down)
        {
            if(GpsPoint.GetLineSide(gpsPointArray.get(gpsNumber-2), gpsPointArray.get(gpsNumber-1),temp[0]))
            {
                result.addPoint(temp[0]);
            }
            else
            {
                result.addPoint(temp[1]);
            }
        }
        if(up<down)
        {
            if(GpsPoint.GetLineSide(gpsPointArray.get(gpsNumber-2), gpsPointArray.get(gpsNumber-1),temp[0]))
            {
                result.addPoint(temp[1]);
            }
            else
            {
                result.addPoint(temp[0]);
            }
        }
        return result;
    }

    public static double[] newGetNodePoint(ArrayList<GpsPoint> tmpGPSPointArr){
        ArrayList<Point> pointArray = new ArrayList<>();
        ArrayList<double[]> dataArr = new ArrayList<>();
        //TODO:目前只有三个点的两两组合
        pointArray.clear();
        for(int i = 0; i < 2; i++){
            //pointArray.clear();
            Point temp[] = getNodePoint(tmpGPSPointArr.get(i), tmpGPSPointArr.get(i+1));
            pointArray.add(temp[0]);
            pointArray.add(temp[1]);
            //StatisticData tempData = new StatisticData();
            //double tempStatisData[] = tempData.getStatisticData(pointArray);
            //dataArr.add(tempStatisData);
        }
        VarianceMethod tempVarMethod = new VarianceMethod();
        tempVarMethod.combineNodePoint(0,2,pointArray);
        dataArr = tempVarMethod.nodeDataArr;
        int minIndex = 0;
        double min = Double.MAX_VALUE;
        for(int i = 0;i < dataArr.size();i++){
            if(dataArr.get(i)[4] < min){
                minIndex = i;
                min = dataArr.get(i)[4];
            }
        }
        return dataArr.get(minIndex);
    }

    //基于统计方差序列点方位求节点的方法
    public Point countGetNodePosition(GpsPoint currentGpsPoint, double rcvDis){
        int number = gpsPointArray.size();  //手机采样点的数量
        loraNode.clear();                   //计算生成的节点队列
        if(number <=2)
        {
            return new Point(NaN,NaN);
        }
        VarianceMethod mVarianceMethod = new VarianceMethod();
        mVarianceMethod.combineGPSPoint(0,3, gpsPointArray);
       // mVarianceMethod.linearCombineGPSPoint(gpsPointArray); //此处可以修改组合方法
        return mVarianceMethod.countNode(currentGpsPoint, 15, rcvDis*1.5);
    }

    public static double[] minimizeGetNodePoint(ArrayList<GpsPoint> tmpGPSPointArr, int step){
        //step是for循环步长
        ArrayList<Point> pointArray = new ArrayList<>();
        ArrayList<double[]> dataArr = new ArrayList<>();
        ArrayList<double[]> totalDataArray = new ArrayList<>();
        double d1 = tmpGPSPointArr.get(0).getDistance(),
                d2 = tmpGPSPointArr.get(1).getDistance(),
                d3 = tmpGPSPointArr.get(2).getDistance();
        for(int a = 0; a < 6; a++) {
            dataArr.clear();
            for (double i = d1 - step; i <= d1 + step; i++)
                for (double j = d2 - step; j <= d2 + step; j++)
                    for (double k = d3 - step; k <= d3 + step; k++) {
                        pointArray.clear();
                        GpsPoint tempGPoint1 = new GpsPoint(tmpGPSPointArr.get(0).getLongitude(), tmpGPSPointArr.get(0).getLatitude(), i),
                                tempGPoint2 = new GpsPoint(tmpGPSPointArr.get(1).getLongitude(), tmpGPSPointArr.get(1).getLatitude(), j),
                                tempGPoint3 = new GpsPoint(tmpGPSPointArr.get(2).getLongitude(), tmpGPSPointArr.get(2).getLatitude(), k);
                        Point temp1[] = getNodePoint(tempGPoint1, tempGPoint2);
                        Point temp2[] = getNodePoint(tempGPoint2, tempGPoint3);
                        switch (a) {
                            case 0:
                                pointArray.add(temp1[0]);
                                pointArray.add(temp1[1]);
                                break;
                            case 1:
                                pointArray.add(temp1[0]);
                                pointArray.add(temp2[0]);
                                break;
                            case 2:
                                pointArray.add(temp1[0]);
                                pointArray.add(temp2[1]);
                                break;
                            case 3:
                                pointArray.add(temp1[1]);
                                pointArray.add(temp2[0]);
                                break;
                            case 4:
                                pointArray.add(temp1[1]);
                                pointArray.add(temp2[1]);
                                break;
                            case 5:
                                pointArray.add(temp2[0]);
                                pointArray.add(temp2[1]);
                                break;
                            default:
                                System.out.println("发生未知错误");
                                break;
                        }
                        StatisticData tempData = new StatisticData();
                        double tempStatisData[] = tempData.getStatisticData(pointArray);
                        dataArr.add(tempStatisData);
                    }
            //找出最小方差
            int minIndex = 0;
            double min = Double.MAX_VALUE;
            for(int i = 0;i < dataArr.size();i++){
                if(dataArr.get(i)[4] < min){
                    minIndex = i;
                    min = dataArr.get(i)[4];
                }
            }
            totalDataArray.add(dataArr.get(minIndex));
        }
        //找出六个组合中的最小的方差作为最终点
        int totalMinIndex = 0;
        double totalMin = Double.MAX_VALUE;
        for(int i = 0;i < totalDataArray.size();i++){
            if(totalDataArray.get(i)[4] < totalMin){
                totalMinIndex = i;
                totalMin = totalDataArray.get(i)[4];
            }
        }
        return totalDataArray.get(totalMinIndex);
    }


    public static double[] minimizeGetNodePoint2(ArrayList<GpsPoint> tmpGPSPointArr, int step){
        //step是for循环步长
        //每次更新步长的
        ArrayList<Point> pointArray = new ArrayList<>();
        ArrayList<double[]> dataArr = new ArrayList<>();
        ArrayList<double[]> totalDataArray = new ArrayList<>();
        ArrayList<double[]> distanceArray = new ArrayList<>();
        ArrayList<double[]> totalDistanceArray = new ArrayList<>();
        double d1 = tmpGPSPointArr.get(0).getDistance(),
                d2 = tmpGPSPointArr.get(1).getDistance(),
                d3 = tmpGPSPointArr.get(2).getDistance();
        for(int a = 0; a < 6; a++) {
            dataArr.clear();
            distanceArray.clear();
            for (double i = d1 - step; i <= d1 + step; i++)
                for (double j = d2 - step; j <= d2 + step; j++)
                    for (double k = d3 - step; k <= d3 + step; k++) {
                        pointArray.clear();
                        GpsPoint tempGPoint1 = new GpsPoint(tmpGPSPointArr.get(0).getLongitude(), tmpGPSPointArr.get(0).getLatitude(), i),
                                tempGPoint2 = new GpsPoint(tmpGPSPointArr.get(1).getLongitude(), tmpGPSPointArr.get(1).getLatitude(), j),
                                tempGPoint3 = new GpsPoint(tmpGPSPointArr.get(2).getLongitude(), tmpGPSPointArr.get(2).getLatitude(), k);
                        Point temp1[] = getNodePoint(tempGPoint1, tempGPoint2);
                        Point temp2[] = getNodePoint(tempGPoint2, tempGPoint3);
                        switch (a) {
                            case 0:
                                pointArray.add(temp1[0]);
                                pointArray.add(temp1[1]);
                                break;
                            case 1:
                                pointArray.add(temp1[0]);
                                pointArray.add(temp2[0]);
                                break;
                            case 2:
                                pointArray.add(temp1[0]);
                                pointArray.add(temp2[1]);
                                break;
                            case 3:
                                pointArray.add(temp1[1]);
                                pointArray.add(temp2[0]);
                                break;
                            case 4:
                                pointArray.add(temp1[1]);
                                pointArray.add(temp2[1]);
                                break;
                            case 5:
                                pointArray.add(temp2[0]);
                                pointArray.add(temp2[1]);
                                break;
                            default:
                                System.out.println("发生未知错误");
                                break;
                        }
                        StatisticData tempData = new StatisticData();
                        double tempStatisData[] = tempData.getStatisticData(pointArray);
                        double tempDistance[] = {i,j,k};
                        distanceArray.add(tempDistance);
                        dataArr.add(tempStatisData);
                    }
            //找出最小方差
            int minIndex = 0;
            double min = Double.MAX_VALUE;
            for(int i = 0;i < dataArr.size();i++){
                if(dataArr.get(i)[4] < min){
                    minIndex = i;
                    min = dataArr.get(i)[4];
                }
            }
            totalDistanceArray.add(distanceArray.get(minIndex));
            totalDataArray.add(dataArr.get(minIndex));
        }
        //找出六个组合中的最小的方差作为最终点
        int totalMinIndex = 0;
        double totalMin = Double.MAX_VALUE;

        for(int i = 0;i < totalDataArray.size();i++){
            if(totalDataArray.get(i)[4] < totalMin){
                totalMinIndex = i;
                totalMin = totalDataArray.get(i)[4];
            }
        }
        tmpGPSPointArr.get(0).setDistance(totalDistanceArray.get(totalMinIndex)[0]);
        tmpGPSPointArr.get(1).setDistance(totalDistanceArray.get(totalMinIndex)[1]);
        tmpGPSPointArr.get(2).setDistance(totalDistanceArray.get(totalMinIndex)[2]);

        return totalDataArray.get(totalMinIndex);
    }

    public Point varianceGetNodePosition(){
        int number = gpsPointArray.size();  //手机采样点的数量
        loraNode.clear();                   //计算生成的节点队列
        if(number <=2)
        {
            return new Point(NaN,NaN);
        }
        VarianceMethod mVarianceMethod = new VarianceMethod();
        mVarianceMethod.combineGPSPoint(0,3, gpsPointArray);
        int minIndex = 0;
        double min = Double.MAX_VALUE;
        for(int i = 0;i < mVarianceMethod.gpsDataArr.size();i++){
            if(mVarianceMethod.gpsDataArr.get(i)[4] < min){
                minIndex = i;
                min = mVarianceMethod.gpsDataArr.get(i)[4];
            }
        }
        Log.e("variance related","当前方差是:"+mVarianceMethod.gpsDataArr.get(minIndex)[4]);
        double threshold = 5.00E-14;
        if(mVarianceMethod.gpsDataArr.get(minIndex)[4] > threshold){
            return new Point(NaN,NaN);
        }else
            return new Point(mVarianceMethod.gpsDataArr.get(minIndex)[0],mVarianceMethod.gpsDataArr.get(minIndex)[1]);
    }
}
