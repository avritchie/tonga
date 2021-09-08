package mainPackage.morphology;

import java.awt.Point;
import mainPackage.utils.GEO;

public class Pairing {

    public EdgePoint who;
    public EdgePoint with;
    public double edgeDistance;
    public double rawDistance;
    public double lineDistance;
    public double parallelity;
    public Point intersect;

    public Pairing(EdgePoint who, EdgePoint with) {
        this.who = who;
        this.with = with;
    }

    public void fill(ROI roi) {
        edgeDistance = GEO.getListDifference(roi.outEdge.list.indexOf(who.line.end), roi.outEdge.list.indexOf(with), roi.outEdge.list.size());
        rawDistance = GEO.getDist(who, with);
        lineDistance = roi.getLineDistance(who,with);
        parallelity = GEO.getParallelFactor(with, who);
        intersect = Line.intersection(who.line, with.line);
    }
}
