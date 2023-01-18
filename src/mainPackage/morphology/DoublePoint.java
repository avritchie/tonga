package mainPackage.morphology;

import java.awt.Point;

public class DoublePoint extends Point {

    double px;
    double py;

    public DoublePoint(double x, double y) {
        this.px = x;
        this.py = y;
        this.x = (int) x;
        this.y = (int) y;
    }

    @Override
    public double getX() {
        return this.px;
    }

    @Override
    public double getY() {
        return this.py;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (int) (java.lang.Double.doubleToLongBits(this.px) ^ (java.lang.Double.doubleToLongBits(this.px) >>> 32));
        hash = 19 * hash + (int) (java.lang.Double.doubleToLongBits(this.py) ^ (java.lang.Double.doubleToLongBits(this.py) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DoublePoint other = (DoublePoint) obj;
        if (java.lang.Double.doubleToLongBits(this.px) != java.lang.Double.doubleToLongBits(other.px)) {
            return false;
        }
        return java.lang.Double.doubleToLongBits(this.py) == java.lang.Double.doubleToLongBits(other.py);
    }

}
