package com.github.pregrafer.RegionInformationReload.Tool;

public class Point {
    double x;
    double y;
    double z;

    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double distance(Point point) {
        double a = getX() - point.getX();
        double b = getY() - point.getY();
        double c = getZ() - point.getZ();
        return Math.sqrt(a * a + b * b + c * c);
    }

    public double distance(double x, double y, double z) {
        double a = getX() - x;
        double b = getY() - y;
        double c = getZ() - z;
        return Math.sqrt(a * a + b * b + c * c);
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
