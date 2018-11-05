package com.example.fubuki.outdoor_navigation;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;

public class VarianceMethod {
    private ArrayList<GpsPoint> tmpGPSPointArr = new ArrayList<>();
    public ArrayList<double[]> gpsDataArr = new ArrayList<>();

    public void combineGPSPoint(int index,int k,ArrayList<GpsPoint> gpsPointArray) {
        if(k == 1){
            for (int i = index; i < gpsPointArray.size(); i++) {
                this.tmpGPSPointArr.add(gpsPointArray.get(i));
                //this.gpsDataArr.add(GpsNode.newGetNodePoint(this.tmpGPSPointArr));
                this.gpsDataArr.add(GpsNode.minimizeGetNodePoint(this.tmpGPSPointArr,1));
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
            this.gpsDataArr.add(GpsNode.minimizeGetNodePoint(this.tmpGPSPointArr,1));
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
}
