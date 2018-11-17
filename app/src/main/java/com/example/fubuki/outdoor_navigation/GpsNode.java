package com.example.fubuki.outdoor_navigation;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;

import static java.lang.Double.NaN;
import static org.apache.commons.math3.linear.MatrixUtils.inverse;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;


public class GpsNode {
    public static final double t1 = 9222364766L;//不加L会报错，说number too large
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

    public double[] newGetNodePoint(ArrayList<GpsPoint> tmpGPSPointArr){
        ArrayList<Point> pointArray = new ArrayList<>();
        ArrayList<double[]> dataArr = new ArrayList<>();
        //TODO:目前只有三个点的两两组合
        pointArray.clear();
        Log.e("newGetNodePoint","当前采样组合："+tmpGPSPointArr.get(0).getIndex()+" "+tmpGPSPointArr.get(0).getLatitude()+" "+tmpGPSPointArr.get(0).getLongitude()+"#"+tmpGPSPointArr.get(1).getIndex()+" "+tmpGPSPointArr.get(1).getLatitude()+" "+tmpGPSPointArr.get(1).getLongitude()+"#"+tmpGPSPointArr.get(2).getIndex()+" "+tmpGPSPointArr.get(2).getLatitude()+" "+tmpGPSPointArr.get(2        ).getLongitude());
        for(int i = 0; i < 2; i++){
            //pointArray.clear();
            Point temp[] = getNodePoint(tmpGPSPointArr.get(i), tmpGPSPointArr.get(i+1));
            pointArray.add(temp[0]);
            pointArray.add(temp[1]);
            //returnNode.addPoint(temp[0]);
            //returnNode.addPoint(temp[1]);
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
        //returnNode.addPoint(new Point(dataArr.get(minIndex)[0],dataArr.get(minIndex)[1]));
        Log.e("GpsNode","本次算出的最小的方差及经纬度："+dataArr.get(minIndex)[4]+"#"+dataArr.get(minIndex)[0]+"#"+dataArr.get(minIndex)[1]);
        return dataArr.get(minIndex);
    }

    //最小二乘法求解迭代的初始点
    public static Point lsGetNodePoint(ArrayList<GpsPoint> tmpGPSPointArr){
        int rowNumber=tmpGPSPointArr.size()-1;//组合内的点数
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
            Log.e("leastSquare","奇异矩阵");
            return new Point((tmpGPSPointArr.get(0).getLongitude()+tmpGPSPointArr.get(1).getLongitude()+tmpGPSPointArr.get(2).getLongitude())/3,(tmpGPSPointArr.get(0).getLatitude()+tmpGPSPointArr.get(1).getLatitude()+tmpGPSPointArr.get(2).getLatitude())/3);
        }

    }

    //牛顿迭代法求解这个组合得到的待定位点
    public static Point newtonIteration(ArrayList<GpsPoint> tmpGPSPointArr){
        //迭代初始点
        double x = lsGetNodePoint(tmpGPSPointArr).getX();
        double y = lsGetNodePoint(tmpGPSPointArr).getY();
        /*double x = (tmpGPSPointArr.get(0).getLongitude()+tmpGPSPointArr.get(1).getLongitude()+tmpGPSPointArr.get(2).getLongitude())/3;
        double y = (tmpGPSPointArr.get(0).getLatitude()+tmpGPSPointArr.get(1).getLatitude()+tmpGPSPointArr.get(2).getLatitude())/3;*/
        //最大迭代次数50次
        int t;
        for(t=0;t<50;t++){
            x=x-obj(x,y,tmpGPSPointArr)/dfx1(x,y,tmpGPSPointArr);
            y=y-obj(x,y,tmpGPSPointArr)/dfy1(x,y,tmpGPSPointArr);
            //满足误差精度时结束循环
            if(obj(x,y,tmpGPSPointArr)<0.3){
                Log.e("leastSquare","当前组合的迭代次数:"+(t+1));
                break;
            }
        }
        if(t==50){
            Log.e("leastSquare","迭代满50次,无效");
            return new Point(NaN,NaN);
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

    /*//基于统计方差序列点方位求节点的方法
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
    }*/

    //基于最可靠的三个点计算lora节点位置，入参可以删掉
    public Point countGetNodePosition(GpsPoint currentGpsPoint, double rcvDis){
        int number = gpsPointArray.size();  //手机采样点的数量
        loraNode.clear();                   //计算生成的节点队列
        if(number <=2)
        {
            return new Point(NaN,NaN);
        }
        VarianceMethod mVarianceMethod = new VarianceMethod();
        mVarianceMethod.newMarkGPSPoint(gpsPointArray,60);//对gps序列点打标记，60是阈值，三个距离的偏差的和，还需要调试
        if(number==3){
            //只有三个点的情况下，他们最可靠，没有选择，count1<count2<count3
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
            Point loraPoint = newtonIteration(reliablePoint);
            if(loraPoint.getX()==NaN&&loraPoint.getY()==NaN){
                return new Point(NaN,NaN);
            }
            //下面都是方便调试的，正式用的时候可以去掉
            //Log.e("leastSquare","仅三个点:"+reliablePoint.get(0).getLongitude()+","+reliablePoint.get(0).getLatitude()+","+reliablePoint.get(0).getDistance()+"-"+reliablePoint.get(1).getLongitude()+","+reliablePoint.get(1).getLatitude()+","+reliablePoint.get(1).getDistance()+"-"+reliablePoint.get(2).getLongitude()+","+reliablePoint.get(2).getLatitude()+","+reliablePoint.get(2).getDistance());
            //Log.e("leastSquare","仅三点的count值:"+reliablePoint.get(0).getCount()+"-"+reliablePoint.get(1).getCount()+"-"+reliablePoint.get(2).getCount());
            Log.e("leastSquare","仅三点算出的节点位置:"+loraPoint.getX()+"-"+loraPoint.getY());
            LatLng p1 = new LatLng(loraPoint.getY(),loraPoint.getX());
            double totalDiff = 0;
            for(int t=0;t<3;t++){
                LatLng p2 = new LatLng(reliablePoint.get(t).getLatitude(),reliablePoint.get(t).getLongitude());
                totalDiff = totalDiff+Math.abs(DistanceUtil.getDistance(p1,p2)-reliablePoint.get(t).getDistance());
            }
            Log.e("leastSquare","仅三个点组合的距离偏差和:"+totalDiff);
            returnNode.addPoint(loraPoint);//显示在屏幕上
            return loraPoint;
        }else{
            //gps点序列变化之后，找到新的最可靠的三个点
            reliablePoint = mVarianceMethod.reliableNode(reliablePoint,gpsPointArray);
            Point loraPoint = newtonIteration(reliablePoint);
            if(loraPoint.getX()==NaN&&loraPoint.getY()==NaN){
                return new Point(NaN,NaN);
            }
            //调试日志
            //Log.e("leastSquare","可靠的三个点:"+reliablePoint.get(0).getLongitude()+","+reliablePoint.get(0).getLatitude()+","+reliablePoint.get(0).getDistance()+"-"+reliablePoint.get(1).getLongitude()+","+reliablePoint.get(1).getLatitude()+","+reliablePoint.get(1).getDistance()+"-"+reliablePoint.get(2).getLongitude()+","+reliablePoint.get(2).getLatitude()+","+reliablePoint.get(2).getDistance());
            //Log.e("leastSquare","可靠三点的count值:"+reliablePoint.get(0).getCount()+"-"+reliablePoint.get(1).getCount()+"-"+reliablePoint.get(2).getCount());
            Log.e("leastSquare","可靠三点算出的节点位置:"+loraPoint.getX()+"-"+loraPoint.getY());
            LatLng p1 = new LatLng(loraPoint.getY(),loraPoint.getX());
            double totalDiff = 0;
            for(int t=0;t<3;t++){
                LatLng p2 = new LatLng(reliablePoint.get(t).getLatitude(),reliablePoint.get(t).getLongitude());
                totalDiff = totalDiff+Math.abs(DistanceUtil.getDistance(p1,p2)-reliablePoint.get(t).getDistance());
            }
            Log.e("leastSquare","当前最可靠的组合的距离偏差和:"+totalDiff);
            returnNode.addPoint(loraPoint);//显示在屏幕上
            return loraPoint;
        }
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
