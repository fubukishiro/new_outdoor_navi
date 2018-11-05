package com.example.fubuki.outdoor_navigation;

/*计算点类，该仅表示坐标系中的一个点*/
public class Point {
    private double x;
    private double y;
    private boolean side;
    Point(double px,double py,boolean pSide)
    {
        x = px;
        y = py;
        side = pSide;
    }
    Point(double px,double py)
    {
        x = px;
        y = py;
    }
    //get the sum of x + y
    public double getSum()
    {
        return x + y;
    }
    public double getSub()
    {
        return  x - y;
    }
    public double getX()
    {
        return x;
    }
    public double getY()
    {
        return y;
    }
    public boolean getSide()
    {
        return side;
    }
    public void setSide(boolean pSide)
    {
        side = pSide;
    }
}

