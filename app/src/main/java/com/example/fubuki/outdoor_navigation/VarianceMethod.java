package com.example.fubuki.outdoor_navigation;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;

public class VarianceMethod {
    private ArrayList<GpsPoint> tmpGPSPointArr = new ArrayList<>();
    private ArrayList<Point> tmpNodePointArr = new ArrayList<>();
    public ArrayList<double[]> gpsDataArr = new ArrayList<>();
    public ArrayList<double[]> nodeDataArr = new ArrayList<>();


    public void combineNodePoint(int index,int k, ArrayList<Point> pointArray){
        if(k == 1){
            for (int i = index; i < pointArray.size(); i++) {
                this.tmpNodePointArr.add(pointArray.get(i));
                StatisticData tempData = new StatisticData();
                double tempStatisData[] = tempData.getStatisticData(this.tmpNodePointArr);
                nodeDataArr.add(tempStatisData);
                this.tmpNodePointArr.remove((Object)pointArray.get(i));
            }
        }else if(k > 1){
            for (int i = index; i <= pointArray.size() - k; i++) {
                this.tmpNodePointArr.add(pointArray.get(i)); //tmpArr都是临时性存储一下
                combineNodePoint(i + 1,k - 1, pointArray); //索引右移，内部循环，自然排除已经选择的元素
                this.tmpNodePointArr.remove((Object)pointArray.get(i)); //tmpArr因为是临时存储的，上一个组合找出后就该释放空间，存储下一个元素继续拼接组合了
            }
        }else{
            return ;
        }
    }

    public void combineGPSPoint(int index,int k,ArrayList<GpsPoint> gpsPointArray) {
        if(k == 1){
            for (int i = index; i < gpsPointArray.size(); i++) {
                this.tmpGPSPointArr.add(gpsPointArray.get(i));
                this.gpsDataArr.add(GpsNode.newGetNodePoint(this.tmpGPSPointArr)); //这个方法是找方差最小化算的
                //this.gpsDataArr.add(GpsNode.minimizeGetNodePoint(this.tmpGPSPointArr,1));  //这个是通过修正距离找方差最小化
                //this.gpsDataArr.add(GpsNode.minimizeGetNodePoint2(this.tmpGPSPointArr,1)); //这个2方法可以保存本次算出来的distance
                this.tmpGPSPointArr.remove((Object)gpsPointArray.get(i));
            }
        }else if(k > 1){
            for (int i = index; i <= gpsPointArray.size() - k; i++) {
                this.tmpGPSPointArr.add(gpsPointArray.get(i)); //tmpArr都是临时性存储一下
                combineGPSPoint(i + 1,k - 1, gpsPointArray); //索引右移，内部循环，自然排除已经选择的元素
                this.tmpGPSPointArr.remove((Object)gpsPointArray.get(i)); //tmpArr因为是临时存储的，上一个组合找出后就该释放空间，存储下一个元素继续拼接组合了
            }
        }else{
            return ;
        }
    }

    //线性组合
    public void linearCombineGPSPoint(ArrayList<GpsPoint> gpsPointArray){
        int step = (int)(Math.floor(gpsPointArray.size()/3));
        for(int i = 0; i < gpsPointArray.size()/3; i++){
            this.tmpGPSPointArr.clear();
            tmpGPSPointArr.add(gpsPointArray.get(i));
            tmpGPSPointArr.add(gpsPointArray.get(i+step));
            tmpGPSPointArr.add(gpsPointArray.get(i+step*2));
            //this.gpsDataArr.add(GpsNode.minimizeGetNodePoint(this.tmpGPSPointArr,3));
            this.gpsDataArr.add(GpsNode.newGetNodePoint(this.tmpGPSPointArr)); //这个方法是找方差最小化算的
            //this.gpsDataArr.add(GpsNode.minimizeGetNodePoint2(this.tmpGPSPointArr,3)); //这个2方法可以保存本次算出来的distance
        }
    }

    public Point countNode(GpsPoint currentGpsPoint, int stepAngle, double radius){
        //radius是搜索范围
        int rangeCount = 360 / stepAngle; //区间数
        int nodeNumCount[] = new int[rangeCount];

        double sumX[] = new double[rangeCount];
        double sumY[] = new double[rangeCount]; //统计每个区间的坐标的和

        //遍历所有计算出来的节点
        for(int i = 0; i < this.gpsDataArr.size();i++){
            Point tmpPoint = new Point(this.gpsDataArr.get(i)[0],this.gpsDataArr.get(i)[1]);
            double angle = MyUtil.calculateAngle(currentGpsPoint, tmpPoint);
            LatLng p1 = new LatLng(tmpPoint.getY(),tmpPoint.getX());
            LatLng p2 = new LatLng(currentGpsPoint.getLatitude(),currentGpsPoint.getLongitude());
            double distance = DistanceUtil.getDistance(p1,p2);//百度API
            for(int j = 0; j<rangeCount; j++){
                if((angle >= stepAngle*j) && angle < (stepAngle * (j+1)) && distance < radius){
                    nodeNumCount[j]++;
                    sumX[j] += tmpPoint.getX();
                    sumY[j] += tmpPoint.getY();
                }
            }
        }

        //找到区间中点最多的地方
        int max = Integer.MIN_VALUE;
        int maxIndex = 0;
        for(int j = 0; j<rangeCount; j++){
            if(nodeNumCount[j] > max){
                max = nodeNumCount[j];
                maxIndex = j;
            }
        }
        return new Point(sumX[maxIndex]/nodeNumCount[maxIndex], sumY[maxIndex]/nodeNumCount[maxIndex]);
    }

    public void newMarkGPSPoint(ArrayList<GpsPoint> gpsPointArray,double thre){
        //应该将新采的这个点 与 前面的序列点中选两个点做组合，不再是C n 3，只做增量的组合，减小复杂度
        this.tmpGPSPointArr.add(gpsPointArray.get(gpsPointArray.size()-1));
        for(int i=0;i<gpsPointArray.size()-2;i++){
            this.tmpGPSPointArr.add(gpsPointArray.get(i));
            for(int j=i+1;j<gpsPointArray.size()-1;j++){
                this.tmpGPSPointArr.add(gpsPointArray.get(j));
                //Point tempBest = GpsNode.lsGetNodePoint(this.tmpGPSPointArr);//这个方法是最小二乘法算的
                Point tempBest = GpsNode.newtonIteration(this.tmpGPSPointArr);//牛顿迭代法
                LatLng p1 = new LatLng(tempBest.getY(),tempBest.getX());
                double totalDiff = 0;
                for(int t=0;t<3;t++){
                    LatLng p2 = new LatLng(this.tmpGPSPointArr.get(t).getLatitude(),this.tmpGPSPointArr.get(t).getLongitude());
                    totalDiff = totalDiff+Math.abs(DistanceUtil.getDistance(p1,p2)-this.tmpGPSPointArr.get(t).getDistance());
                }
                Log.e("leastSquare","当前组合的距离偏差和:"+totalDiff);
                if(totalDiff>thre){
                    //超过阈值打标记
                    for(int t=0;t<3;t++){
                        this.tmpGPSPointArr.get(t).addCount();
                        //Log.e("leastSquare","当前点"+this.tmpGPSPointArr.get(t).getLongitude()+"-"+this.tmpGPSPointArr.get(t).getLatitude()+"count:"+this.tmpGPSPointArr.get(t).getCount());
                    }
                }
                this.tmpGPSPointArr.remove((Object)gpsPointArray.get(j));
            }
            this.tmpGPSPointArr.remove((Object)gpsPointArray.get(i));
        }
        this.tmpGPSPointArr.remove((Object)gpsPointArray.get(gpsPointArray.size()-1));
    }

    //gps序列点增多后，找出新的最可靠的三个点
    public ArrayList<GpsPoint> reliableNode(ArrayList<GpsPoint> reliablePointArray,ArrayList<GpsPoint> gpsPointArray){
        double count = gpsPointArray.get(gpsPointArray.size()-1).getCount();
        GpsPoint reliableGPSPoint1,reliableGPSPoint2,reliableGPSPoint3;
        //reliablePointArray里面的顺序可能会乱掉 比如1<2>3,重新排一下
        if(reliablePointArray.get(1).getCount()<reliablePointArray.get(0).getCount()){
            reliableGPSPoint1 = reliablePointArray.get(1);
            reliableGPSPoint2 = reliablePointArray.get(0);
        }else{
            reliableGPSPoint1 = reliablePointArray.get(0);
            reliableGPSPoint2 = reliablePointArray.get(1);
        }
        if(reliablePointArray.get(2).getCount()<reliableGPSPoint1.getCount()){
            reliableGPSPoint3 = reliableGPSPoint2;
            reliableGPSPoint2 = reliableGPSPoint1;
            reliableGPSPoint1 = reliablePointArray.get(2);
        }else if(reliablePointArray.get(2).getCount()<reliableGPSPoint2.getCount()){
            reliableGPSPoint3 = reliableGPSPoint2;
            reliableGPSPoint2 = reliablePointArray.get(2);
        }else{
            reliableGPSPoint3 = reliablePointArray.get(2);
        }
        //加入最新点
        if(count<reliableGPSPoint1.getCount()){
            reliableGPSPoint3 = reliableGPSPoint2;
            reliableGPSPoint2 = reliableGPSPoint1;
            reliableGPSPoint1 = gpsPointArray.get(gpsPointArray.size()-1);
        }else if(count<reliableGPSPoint2.getCount()){
            reliableGPSPoint3 = reliableGPSPoint2;
            reliableGPSPoint2 = gpsPointArray.get(gpsPointArray.size()-1);
        }else if(count<reliableGPSPoint3.getCount()){
            reliableGPSPoint3 = gpsPointArray.get(gpsPointArray.size()-1);
        }else if(count == reliableGPSPoint3.getCount()&&gpsPointArray.get(gpsPointArray.size()-1).getDistance()<reliableGPSPoint3.getDistance()){
            reliableGPSPoint3 = gpsPointArray.get(gpsPointArray.size()-1);
        }
        ArrayList<GpsPoint> reliableGPSPoint = new ArrayList<>();
        reliableGPSPoint.add(reliableGPSPoint1);
        reliableGPSPoint.add(reliableGPSPoint2);
        reliableGPSPoint.add(reliableGPSPoint3);
        return reliableGPSPoint;
    }
}
