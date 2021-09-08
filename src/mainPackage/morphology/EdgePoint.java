/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mainPackage.morphology;

import java.awt.Point;
import static java.lang.Double.NaN;
import mainPackage.utils.GEO;

/**
 *
 * @author aritchie
 */
public class EdgePoint extends Point {

    public double angle;
    public double direction;
    public double distance;
    public Line line; // viiva sijainnin ja directionin mukaan
    public Pairings pairings; // kaikki mahdolliset pisteet joiden kanssa voi parittaa +datat niistä
    public boolean hasBeenPairedYet;
    public boolean isUnsure; // onko unsure pixel? vai normaali

    public EdgePoint(int x, int y) {
        super(x, y);
        direction = NaN;
        angle = NaN;
        distance = NaN;
        line = null;
        // intersection stuff
        hasBeenPairedYet = false;
        pairings = new Pairings();
        isUnsure = false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != Point.class
                && obj.getClass() != EdgePoint.class) {
            return false;
        }
        return (((Point) obj).x == this.x) && (((Point) obj).y == this.y);
        //return hashCode() == obj.hashCode();
    }

    @Override
    public String toString() {
        return "EdgePoint{x:" + x + ",y:" + y + ", angle=" + angle + ", direction=" + direction + ", distance=" + distance + ", line=" + line + '}';
    }

    protected void setAsFriend(EdgePoint edgePoint, int dist) {
        pairings.closestFriend = edgePoint;
        pairings.closestFriendDistance = dist;
        pairings.closestFriendDistanceRaw = GEO.getDist(this, edgePoint);
        //System.out.println("Friend found at " + edgePoint + " for " + this);
        //System.out.println("Distance to her is " + dist);
    }

    protected void setAsBuddy(EdgePoint edgePoint, int dist) {
        pairings.closestBuddy = edgePoint;
        pairings.closestBuddyDistance = dist;
        pairings.closestBuddyDistanceRaw = GEO.getDist(this, edgePoint);
        //System.out.println("Buddy found at " + edgePoint + " for " + this);
        //System.out.println("Distance to her is " + dist);
    }

}
