/*手机采样点的位置，包含坐标和采集到的距离*/
package com.example.fubuki.outdoor_navigation;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.example.fubuki.outdoor_navigation.Point;

public class GpsPoint {
    private double angle;       //采样点与北方向夹角，弧度值
    private double longitude;   //采样点经度
    private double latitude;    //采样点纬度
    private double distance;    //采样点采集的距离
    public static final double k1 = 96029;
    public static final double k2 = 112000;

    GpsPoint(double pLongitude,double pLatitude,double pAngle,double pDistance)
    {
        angle = pAngle;
        latitude = pLatitude;
        longitude = pLongitude;
        distance = pDistance;
    }

    GpsPoint(double pLongitude,double pLatitude, double pDistance)
    {
        latitude = pLatitude;
        longitude = pLongitude;
        distance = pDistance;
    }

    public double getLongitude()
    {
        return longitude;
    }
    public double getLatitude()
    {
        return latitude;
    }
    public double getDistance(){return distance;}
    public void setDistance(double setDis){ this.distance = setDis; }
    /*返回两个手机采样点点之间的距离*/
    public double getDistanceFromNextPoint(GpsPoint nextGpsPoint)
    {
        LatLng p1 = new LatLng(latitude,longitude);
        LatLng p2 = new LatLng(nextGpsPoint.getLatitude(),nextGpsPoint.getLongitude());

        //此处调用百度地图API获取两个点之间的距离，单位是米
        double distance = DistanceUtil.getDistance(p1,p2);
        //Log.e("Distance","Distance:"+distance);
        return DistanceUtil.getDistance(p1,p2);
    }
    /*根据两个GPS点计算出两个节点Point*/
    Point[] calculateNode(GpsPoint otherGpsPoint)
    {
        double pointAngle = 0;
        double nextPointDistance = otherGpsPoint.getDistance();
        double distanceToNextPoint = this.getDistanceFromNextPoint(otherGpsPoint);
        double T = ((this.distance)*(this.distance)+(distanceToNextPoint)*(distanceToNextPoint)-(nextPointDistance)*(nextPointDistance))/(2*distance*distanceToNextPoint);
        if(T >= 0.999999)
        {
            T = 0.999999;
        }
        else if(T <= -0.999999)
        {
            T = -0.9999999;
        }
        pointAngle = Math.acos(T);
        //用gps点的数据计算夹角

        GpsPoint P0 = new GpsPoint(this.getLongitude(),otherGpsPoint.getLatitude(),0,0);
        double L01 = P0.getDistanceFromNextPoint(this);
        double L12 = distanceToNextPoint;
        double cosa = L01/L12;
        if(cosa>=0.999999999)
        {
            cosa = 0.99999999;
        }
        if(cosa<=-0.999999999)
        {
            cosa = -0.99999999;
        }
        if((otherGpsPoint.getLatitude()>=this.getLatitude()&&(otherGpsPoint.getLongitude()>=this.getLongitude())))        //1
        {
            angle = Math.acos(cosa);
        }
        else if((otherGpsPoint.getLatitude()>=this.getLatitude()&&(otherGpsPoint.getLongitude()<this.getLongitude())))   //2
        {
            angle = Math.PI - Math.acos(cosa);
        }
        else if((otherGpsPoint.getLatitude()<this.getLatitude()&&(otherGpsPoint.getLongitude()<this.getLongitude())))   //3
        {
            angle = Math.PI + Math.acos(cosa);
        }
        else if((otherGpsPoint.getLatitude()<this.getLatitude()&&(otherGpsPoint.getLongitude()>=this.getLongitude())))   //4
        {
            angle = 2 * Math.PI - Math.acos(cosa);
        }
        Point[] result = new Point[2];
        double x1,x2,y1,y2;

        x1 = this.longitude + this.distance*Math.cos(pointAngle+Math.PI/2-angle)/k1;
        y1 = this.latitude + this.distance*Math.sin(pointAngle+Math.PI/2-angle)/k2;
        x2 = this.longitude + this.distance*Math.cos(Math.PI/2-angle-pointAngle)/k1;
        y2 = this.latitude + this.distance*Math.sin(Math.PI/2-angle-pointAngle)/k2;

//        double res1,res2;
//
//       res1 = (this.getLatitude()-otherGpsPoint.getLatitude())/(this.getLongitude()-otherGpsPoint.getLongitude())*(x1-this.getLongitude())+this.getLatitude();
//        res2 = (this.getLatitude()-otherGpsPoint.getLatitude())/(this.getLongitude()-otherGpsPoint.getLongitude())*(x2-this.getLongitude())+this.getLatitude();

      //  if(res1>res2)
//        {
            result[0] = new Point(x1,y1,true);
            result[1] = new Point(x2,y2,false);
//        }
//        else
//        {
//            result[0] = new Point(x1,y1,false);
//            result[1] = new Point(x2,y2,true);
//        }
        return result;
    }
    /*判断点在直线的哪一边*/
    public static boolean GetLineSide(GpsPoint p1,GpsPoint p2,Point ps)
    {
        double res = (p2.getLatitude()-p1.getLatitude())/(p1.getLongitude()-p1.getLongitude())*(ps.getX()-p1.getLongitude())+p1.getLatitude();
        if(ps.getY()>res)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
