/*该类表示平面上点的集合，包含由集合计算节点位置的方法*/
package com.example.fubuki.outdoor_navigation;

import com.example.fubuki.outdoor_navigation.Point;

import java.util.ArrayList;
public class Node {
    public ArrayList<Point> nodePointArray;
    public Node()
    {
        nodePointArray = new ArrayList<>();
    }
    public int getSize()
    {
        return  nodePointArray.size();
    }
    public Point getPoint(int i)
    {
        return nodePointArray.get(i);
    }
    public void clear()
    {
        nodePointArray.clear();
    }
    public void addPoint(Point pPoint)
    {
        nodePointArray.add(pPoint);
    }
    public Node copy()
    {
        Node result = new Node();
        for(int i=0;i<this.getSize();i++)
        {
            result.addPoint(this.getPoint(i));
        }
        return result;
    }
    public void removePoint(Point point)
    {
        System.out.println("Remove Point:"+"X"+point.getX()+" Y"+point.getY());
        nodePointArray.remove(point);
    }
    public boolean contains(Point point)
    {
        return (nodePointArray.contains(point));
    }
    public Point getNodeLocation()
    {
        double longitude = 0;
        double latitude = 0;
        for(int i=0;i<this.getSize();i++)
        {
            longitude += this.getPoint(i).getX();
            latitude += this.getPoint(i).getY();
        }
        return new Point(longitude/this.getSize(),latitude/this.getSize());
    }
}
