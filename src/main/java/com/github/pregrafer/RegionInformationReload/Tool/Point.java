package com.github.pregrafer.RegionInformationReload.Tool;

/**
 * 自定义点类
 * 储存三维位置
 * 放弃Java Point3D (不适用于不同的JDK)
 */
public class Point {
    private double x;
    private double y;
    private double z;

    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double distance(double x, double y, double z) {
        double a = getX() - x;
        double b = getY() - y;
        double c = getZ() - z;
        return Math.sqrt(a * a + b * b + c * c);
    }

    public double distance(Point point) {
        return distance(point.getX(), point.getY(), point.getZ());
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    public void reset(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
