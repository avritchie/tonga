package mainPackage.morphology;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import mainPackage.utils.GEO;
import mainPackage.utils.DRAW.lineDrawer;

public class Segmentor {

    private ROI ROI;
    private ROISet SET;
    private int avgObjSize;
    private int lineThickness;
    public static boolean segmentedSomething;

    Segmentor(ROI myself, ROISet set) {
        ROI = myself;
        SET = set;
        avgObjSize = (int) SET.avgCornerlessSize();
        lineThickness = SET.targetsize > 35 ? 2 : 1;
        //lineThickness = (int) Math.max(1, Math.pow(SET.targetsize, 0.5) / 3); // 2
    }

    abstract class formPairs {

        Pairings p;

        abstract void logic(EdgePoint p);

        public void pair(ROI roi) {
            roi.edgeData.cornerPoints.forEach(pp -> {
                if (!pp.hasBeenPairedYet) {
                    p = pp.pairings;
                    logic(pp);
                }
            });
        }

        public void pairBuddies(ROI roi) {
            roi.edgeData.cornerCandidates.forEach(pp -> {
                if (!pp.hasBeenPairedYet) {
                    p = pp.pairings;
                    logic(pp);
                }
            });
        }
    }

    protected void segmentu() {
        new formPairs() {
            @Override
            void logic(EdgePoint point) {
                if (point.pairings.closestOpposite != null) {
                    drawSegmentLine(point, point.pairings.closestOpposite);
                }
            }
        }.pair(ROI);
    }

    private formPairs ultraCloseParallel() {
        return new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // pair those that are very closest and very parallel
                if (!p.pairingMap.isEmpty()) {
                    EdgePoint cc = p.closest(0);
                    Pairing pc = p.pairingMap.get(cc);
                    if ((pc.parallelity < 0.2 && pc.rawDistance < partOfAvg(20))
                            || (pc.parallelity < 0.1 && pc.rawDistance < partOfAvg(15))) {
                        justPair(point, cc, "ultraclose");
                    }
                }
            }
        };
    }

    private formPairs primaryCloseParallel() {
        return new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // pair those that are very closest and very parallel
                if (!p.pairingMap.isEmpty()) {
                    if (p.closest.size() > 1) {
                        Pairing cc1, cc2;
                        EdgePoint ep1;
                        ep1 = p.closest(0);
                        cc1 = p.pairingMap.get(ep1);
                        cc2 = p.pairingMap.get(p.closest(1));
                        if (cc1.parallelity < 0.1
                                && cc1.rawDistance * 3 < cc2.rawDistance
                                && cc1.rawDistance < partOfAvg(10)) {
                            justPair(point, ep1, "primary raw closest item");
                        }
                        // if only one other point and that is very close/parallel
                    } else {
                        EdgePoint cc = p.closest(0);
                        Pairing pc = p.pairingMap.get(cc);
                        if ((pc.parallelity < 0.2 && pc.rawDistance < partOfAvg(15))
                                || (pc.parallelity < 0.1 && pc.rawDistance < partOfAvg(10))) {
                            justPair(point, cc, "primary raw only item");
                        }
                    }
                }
            }
        };
    }

    private formPairs commonMidpoint() {
        return new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // multiple intersections with other lines (=create common midpoint)
                if (!p.intersectors.isEmpty()) {
                    // only common if all points counted?
                    List<EdgePoint> pool = intersectorPool(point, false);
                    if (pool.size() > 2 && onlyCommonIntersections(pool)) {
                        pairPool(pool, "allpoints");
                    } else {
                        // only common if only sures counted?
                        pool = intersectorPool(point, true);
                        if (pool.size() > 2 && onlyCommonIntersections(pool)) {
                            pairPool(pool, "surepoints");
                        }
                    }
                    if (!point.hasBeenPairedYet) {
                        EdgePoint po = bestIntersector(point);
                        if (po != null) {
                            //  pairTwo(point, po, "best intersector");
                        }
                    }
                }
            }
        };
    }

    private formPairs closePairs() {
        return new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // both are each others closest and also parallel
                // raw distance does not matter
                if (!p.pairingMap.isEmpty()) {
                    EdgePoint ep;
                    if (p.closest.size() > 1) {
                        Pairing cc1, cc2;
                        ep = p.closestEdge(0);
                        cc1 = p.pairingMap.get(ep);
                        cc2 = p.pairingMap.get(p.closestEdge(1));
                        if (cc1.parallelity < 0.1
                                && p.closestLine(0) == ep
                                && cc1.edgeDistance * 3 < cc2.edgeDistance
                                && cc1.lineDistance * 2 < cc2.lineDistance) {
                            justPair(point, ep, "edge/line closest pair");
                        }
                    }
                    ep = p.closest(0);
                    if (!point.hasBeenPairedYet && (point.isUnsure || ep.pairings.closest(0).equals(point))) {
                        if (p.pairingMap.get(ep).parallelity < 0.15) {
                            justPair(point, ep, "both-sided closeness and parallelity");
                        }
                    }
                }
            }
        };
    }

    private formPairs secondaryCloseParallel() {
        return new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // find good pairs who are kinda close and parallel and still unpaired
                if (!p.pairingMap.isEmpty() && p.closest.size() > 1) {
                    EdgePoint cc1 = p.closest(0);
                    if (p.pairingMap.get(cc1).rawDistance * 2 < p.closestDist(1)
                            && p.pairingMap.get(cc1).rawDistance < partOfAvg(5)) {
                        if (p.pairingMap.get(cc1).parallelity < 0.2) {
                            justPair(point, cc1, "secondary closest item");
                        }
                    }
                }
            }
        };
    }

    private formPairs closeOpposite() {
        return new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // use closest opposite point detection and find pairs that are
                // physically close to this opposite point by raw/edge/line distance
                if (p.closestOpposite != null) {
                    if (!p.pairingMap.isEmpty()) {
                        // opposite point matches some point in physical proximity 
                        EdgePoint cp = getClosest(p.closestOpposite, true, false);
                        if (cp != null && GEO.getDist(cp, p.closestOpposite) < partOfAvg(10)
                                && (cp.equals(p.closest(0)) || cp.equals(p.closestEdge(0)) || cp.equals(p.closestLine(0)))) {
                            justPair(point, p.closestOpposite, "conditional close opposite pairing");
                            cp.hasBeenPairedYet = true;
                        } else {
                            // same thing but try buddies instead
                            cp = getClosest(p.closestOpposite, true, true);
                            if (cp != null && GEO.getDist(cp, p.closestOpposite) < partOfAvg(10)
                                    && (cp.equals(p.closest(0)) || cp.equals(p.closestEdge(0)) || cp.equals(p.closestLine(0)))) {
                                justPair(point, p.closestOpposite, "conditional close opposite pairing");
                                cp.hasBeenPairedYet = true;
                            }
                        } // doesn't, but is still very close 
                        if (!point.hasBeenPairedYet && GEO.getDist(point, p.closestOpposite) < partOfAvg(10)) {
                            justPair(point, p.closestOpposite, "conditional raw opposite pairing");
                        }
                    } else {
                        // shapes without other points, but opposite point is close and/or shape is big
                        if ((ROI.getSize() > SET.avgCornerlessSize() * 2 && p.closestOppositeDistance < diam(2))
                                || p.closestOppositeDistance < partOfAvg(10)) {
                            justPair(point, p.closestOpposite, "direct opposite pairing");
                        }
                    }
                }
            }
        };
    }

    private formPairs goodRemaining() {
        return new formPairs() {
            @Override
            void logic(EdgePoint point) {
                if (!p.pairingMap.isEmpty()) {
                    // find shapes with just one possible pairing which is good enough
                    if (p.pairingMap.size() == 1) {
                        EdgePoint cc = p.closest(0);
                        double cd = p.closestDist(0);
                        if (cd < diam(2)
                                && cd < p.ownEndDistance
                                && cd < cc.pairings.ownEndDistance
                                && p.pairingMap.get(cc).parallelity < 0.33) {
                            justPair(point, cc, "good dual pairing");
                        }
                    } // find very close remaining pairings
                    else {
                        if (p.closestOpposite != null) {
                            EdgePoint cp = getClosest(p.closestOpposite, false, false);
                            if (cp != null && GEO.getDist(cp, p.closestOpposite) < partOfAvg(10)) {
                                justPair(point, p.closestOpposite, "closest free opposite pairing");
                                cp.hasBeenPairedYet = true;
                            }
                        }
                        if (!point.hasBeenPairedYet) {
                            EdgePoint cc1 = p.closestLine(0);
                            if (p.pairingMap.get(cc1).lineDistance < partOfAvg(10) && (p.pairingMap.get(cc1).rawDistance < p.closestDist(0) * 1.5 || cc1.equals(p.closestEdge(0)))) {
                                if (GEO.getParallelFactor(point.direction, GEO.getDirection(point, cc1)) > 0.4) {
                                    justPair(point, cc1, "close line pairing");
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    private formPairs finalUnpaired() {
        return new formPairs() {
            @Override
            void logic(EdgePoint point) {
                if (!p.pairingMap.isEmpty()) {
                    EdgePoint c1, c2;
                    // detect unpaired close/parallel pairs, closest to only each other anymore
                    c1 = getClosest(point, true, false);
                    if (c1 != null) {
                        c2 = getClosest(c1, true, false);
                        if (c2 != null && point.equals(c2)) {
                            if (p.pairingMap.containsKey(c1) && p.pairingMap.get(c1).parallelity < 0.33) {
                                justPair(point, c1, "close pair left");
                            }
                        }
                    }
                    // same but with buddies
                    if (!point.hasBeenPairedYet) {
                        c1 = getClosest(point, true, true);
                        if (c1 != null) {
                            c2 = getClosest(c1, true, false);
                            if (c2 != null && point.equals(c2)) {
                                if (p.pairingMap.containsKey(c1) && p.pairingMap.get(c1).parallelity < 0.16) {
                                    justPair(point, c1, "close pair left");
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    private formPairs solitaryClose() {
        return new formPairs() {
            @Override
            void logic(EdgePoint point) {
                //look for unpaired solitary buddies with very short lines
                if (p.ownEndDistance < partOfAvg(20)) {
                    pairWithEnd(point, "solitary short buddy");
                } else if (p.closestOpposite != null) {
                    if (p.closestOppositeDistance < partOfAvg(20)) {
                        justPair(point, p.closestOpposite, "solitary short buddy opposite");
                    }
                }
            }
        };
    }

    protected void segment() {
        primaryCloseParallel().pair(ROI);
        commonMidpoint().pair(ROI);
        closePairs().pair(ROI);
        closePairs().pairBuddies(ROI);
        secondaryCloseParallel().pair(ROI);
        closeOpposite().pair(ROI);
        goodRemaining().pair(ROI);
        finalUnpaired().pair(ROI);
        solitaryClose().pairBuddies(ROI);
    }

    protected void segmentSure() {
        primaryCloseParallel().pair(ROI);
    }

    protected void segmentSuperSure() {
        ultraCloseParallel().pair(ROI);
    }

    @Deprecated
    protected void segment2() {
        System.out.println("//////////////////////////////////////\n" + "Start segmentation for the shape:");
        new formPairs() {
            @Override
            void logic(EdgePoint point) {
                System.out.println("");
                // very closeby pairings
                if (p.closestFriend != null && p.closestFriend.pairings.closestFriend != null) {
                    // both are friends w each other
                    if (p.closestFriend.pairings.closestFriend.equals(point)) {
                        if ((p.closestFriendDistance < partOfAvg(10)) && (p.closestFriend.pairings.closestFriendDistance < partOfAvg(10))) {
                            if (p.closestLineFriend != null && p.closestFriendDistanceRaw > GEO.getDist(p.closestLineFriend, point)
                                    && GEO.getDist(p.closestFriend, p.closestLineFriend) > p.closestLineFriendDistance
                                    && p.closestLineFriendDistance * 2 < p.closestFriendDistanceRaw) {
                                pairWithLineFriend(point, "closer proximity (dist " + p.closestLineFriendDistance + ")");
                            } else {
                                pairWithFriend(point, "close proximity (for me " + p.closestFriendDistance + ", for her " + p.closestFriend.pairings.closestFriendDistance + ", w part of " + partOfAvg(10) + ")");
                            }
                        } else if ((p.closestFriendDistance < partOfAvg(5))
                                && (p.closestFriend.pairings.closestFriendDistance < partOfAvg(5))
                                && GEO.getParallelFactor(point, p.closestFriend) < 0.1) {
                            if (angleTest(point, p.closestFriend, 60)) {
                                pairWithFriend(point, "close proximity and parallelity (for me " + p.closestFriendDistance + ", for her " + p.closestFriend.pairings.closestFriendDistance + ", w part of " + partOfAvg(5) + ")");
                            }
                        }
                    }
                }
            }
        }.pair(ROI);
        new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // very closeby pairings parallel nonfriend
                EdgePoint closest = getClosest(point, true, false);
                if (closest != null && getClosest(closest, true, false).equals(point)) {
                    double dist = GEO.getDist(point, closest);
                    // both are friends w each other
                    if (p.closestFriend != null && closest.pairings.closestFriend != null) {
                        if (dist < partOfAvg(10) && (p.closestFriend.equals(closest) || dist < p.closestFriendDistanceRaw)
                                && (closest.pairings.closestFriend.equals(point) || dist < closest.pairings.closestFriendDistanceRaw)) {
                            if ((GEO.getParallelFactor(point, closest) < 0.25)) {
                                pairTwo(point, closest, "two very closeby");
                            }
                        }
                    }
                }
            }
        }.pair(ROI);
        new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // very parallel pairings
                EdgePoint parallel = getParallelFriend(point);
                if (parallel != null && !parallel.hasBeenPairedYet) {
                    double pointdist = GEO.getDist(point, parallel.line.end);
                    double paralleldist = GEO.getDist(point.line.end, parallel);
                    if ((pointdist < partOfAvg(10) || paralleldist < partOfAvg(10))
                            && (GEO.getDist(point, parallel) < partOfAvg(5) || (parallel.equals(p.closestFriend) && p.closestFriendDistance < partOfAvg(5)))) {
                        if (angleTest(point, p.closestFriend, 60)) {
                            pairTwo(point, parallel, "very parallel (dists " + pointdist + ", for her " + paralleldist + ")");
                        }
                    }
                }
            }
        }.pair(ROI);
        new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // very parallel pairings w friend/buddy
                if (p.closestBuddy != null && p.closestBuddy.pairings.closestFriend != null
                        && p.closestBuddy.pairings.closestFriend.equals(point)) {
                    double dist = p.closestBuddyDistanceRaw;
                    if ((GEO.getParallelFactor(point, p.closestBuddy) < 0.15)
                            && ((p.closestBuddyDistance + p.closestBuddy.pairings.closestFriendDistance * 2 < dist * 2))) {
                        pairWithBuddy(point, "very close and parallel");
                    }
                }
            }
        }.pair(ROI);
        new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // very close line pairings
                if (!point.hasBeenPairedYet && p.closestLineFriend != null) {
                    if (p.closestLineFriendDistance < 5 && p.closestLineFriendDistance < p.closestFriendDistance) {
                        if (GEO.getParallelFactor(point, p.closestLineFriend) < 0.15) {
                            pairWithLineFriend(point, "very close (dist " + p.closestLineFriendDistance + ")");
                        }
                    }
                }
            }
        }.pair(ROI);
        new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // shapes with only one concave point
                if (ROI.edgeData.cornerPoints.size() == 1) {
                    // there is a buddy semi-close to the opposite side point
                    if (p.closestBuddy != null && (ROI.getSize() > SET.avgCornerlessSize() * 2
                            || (ROI.getSize() > SET.avgCornerlessSize() && p.closestBuddyDistanceRaw < diam(2)))) {
                        if (p.closestBuddyDistance < partOfAvg(10)) {
                            pairWithBuddy(point, "the only cornerpoint (dist " + p.closestBuddyDistance + ")");
                            // or no buddies but large area
                        } else {
                            pairWithEnd(point, "opposite side as the only cornerpoint");
                        }
                    }
                }
            }
        }.pair(ROI);
        new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // pairings with two buddies parallel and close, both each others closest buddies
                if (p.closestBuddy != null) {
                    Pairings cBp = p.closestBuddy.pairings;
                    if (cBp.closestBuddy != null && !p.closestBuddy.hasBeenPairedYet && cBp.closestBuddy.equals(point)) {
                        double dist = p.closestBuddyDistanceRaw;
                        if ((GEO.getParallelFactor(point, p.closestBuddy) < 0.15)
                                && ((p.closestBuddyDistance < dist && cBp.closestBuddyDistance < dist && dist < diam(2)) || dist < partOfAvg(10))) {
                            pairWithBuddy(point, "two buddy pairing (pf " + GEO.getParallelFactor(point, p.closestBuddy) + ")");
                        }
                    }
                }
            }
        }.pairBuddies(ROI);
        new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // one intersection with just one line (=use that)
                if (p.intersectors.size() == 1) {
                    // the other pair doesn't have any more pairings
                    EdgePoint suspect = p.intersectors.get(0);
                    if (suspect.pairings.intersectors.size() == 1 && !suspect.hasBeenPairedYet) {
                        // the intersection lies between the points
                        if (GEO.getDist(point, suspect) < GEO.getDist(point, Line.intersection(point.line, suspect.line))) {
                            pairTwo(point, p.intersectors.get(0), "the only intersection");
                        }
                    } // other pairings exist
                    else {
                        // false positive intersection if parallel lines
                        if (GEO.getParallelFactor(point, p.intersectors.get(0)) > 0.35) {
                            System.out.println("False positive detection for " + point + " with " + p.intersectors.get(0));
                            if (!p.intersectors.get(0).hasBeenPairedYet) {
                                p.intersectors.get(0).pairings.intersectors.remove(point);
                                //singlePairings(p.intersectors.get(0));
                            }
                            p.intersectors.clear();
                        }
                    }
                }
            }
        }.pair(ROI);
        new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // multiple intersections with other lines (=create common midpoint)
                if (!p.intersectors.isEmpty()) {
                    List<EdgePoint> pool = intersectorPool(point, false);
                    // all others have been paired already
                    if (pool.size() == 1 && pool.get(0).equals(point)) {
                        if (p.closestBuddy != null && (p.closestBuddyDistanceRaw < p.closestFriendDistanceRaw)) {
                            pairWithBuddy(point, "intersect pairing since only one left");
                        } else {
                            EdgePoint closest = getClosest(point, false, false);
                            if (closest != null && p.closestFriend != null) {
                                if (GEO.getDist(closest, point) * 1.5 < p.closestFriendDistance) {
                                    pairTwo(point, closest, "intersect pairing closest since only one left");
                                } else {
                                    if (p.closestLineFriend != null && p.closestFriendDistanceRaw > GEO.getDist(point, p.closestLineFriend)) {
                                        pairWithLineFriend(point, "intersect pairing since only one left");
                                    } else {
                                        pairWithFriend(point, "intersect pairing since only one left");
                                    }
                                }
                            } else if (p.closestFriend != null) {
                                pairWithFriend(point, "intersect pairing since no closest and only one left");
                            }
                        }
                    } // everyone only intersects with each other
                    else if (onlyCommonIntersections(pool)) {
                        pairPool(pool, "allpoints");
                    } // complex web of interactions... 
                    else {
                        // only common if only sures counted?
                        pool = intersectorPool(point, true);
                        if (pool.size() > 1 && onlyCommonIntersections(pool)) {
                            pairPool(pool, "surepoints");
                        } else if (p.intersectors.size() == 1) {
                            Point is = Line.intersection(point.line, p.intersectors.get(0).line);
                            if (GEO.getDist(point, p.intersectors.get(0)) / 2. > GEO.getDist(p.intersectors.get(0), is)
                                    || GEO.getDist(point, p.intersectors.get(0)) / 2. > GEO.getDist(point, is)) {
                                pairTwo(point, p.intersectors.get(0), "close intersection");
                            }
                        }
                    }
                    if (!point.hasBeenPairedYet) {
                        EdgePoint p = bestIntersector(point);
                        if (p != null) {
                            pairTwo(point, p, "best intersector");
                        }
                    }
                }
            }
        }.pair(ROI);
        new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // no intersections with other lines (=use closest)
                if (p.intersectors.isEmpty()) {
                    // i have a buddy and she is physically closer to me than my actual friends
                    if (p.closestBuddy != null && p.closestFriend != null) {
                        EdgePoint closest = getClosest(point, true, false);
                        if ((closest == null || p.closestBuddyDistanceRaw < GEO.getDist(point, closest) * 2) && p.closestBuddyDistance < partOfAvg(10)
                                && ((p.closestBuddyDistanceRaw < p.closestFriendDistanceRaw) || ((p.closestBuddyDistance * 2 < p.closestFriendDistance)))) {
                            pairWithBuddy(point, "no intersections");
                        }
                    }
                    // i have a friend (=there is at least 1 another point)
                    if (!point.hasBeenPairedYet && p.closestFriend != null) {
                        // i am also her closest friend!
                        if (p.closestFriend.pairings.closestFriend != null && p.closestFriend.pairings.closestFriend.equals(point)) {
                            pairWithFriend(point, "closest bidirectional since no intersections");
                        } // i am not her friend but i'll take her anyway
                        // at least if she is close enough and not taken
                        else if (p.closestFriendDistance < partOfEdge(5) && !p.closestFriend.hasBeenPairedYet) {
                            pairWithFriend(point, "closest anyway since no intersections");
                        } // or anyway if no other option and overall the closest
                        else if (GEO.getParallelFactor(point, p.closestFriend) < 0.15) {
                            if (getClosest(point, false, false).equals(p.closestFriend)
                                    || (getClosest((EdgePoint) point.line.end, false, false).equals(p.closestFriend)
                                    && p.closestFriendDistance < partOfAvg(10) && p.closestLineFriendDistance > p.closestFriendDistance * 2)) {
                                EdgePoint np = getLineMidPoint(point, p.closestFriend);
                                pairTwo(point, np, "closest midpoint anyway since only option and no intersections");
                            }
                        }
                    }
                }
            }
        }.pair(ROI);
        new formPairs() {
            @Override
            void logic(EdgePoint point) {
                // if there is a buddy unpaired and semi-close, not for single-point rois
                if (ROI.edgeData.cornerPoints.size() > 1) {
                    if (p.closestBuddy != null && !p.closestBuddy.hasBeenPairedYet
                            && p.closestFriend != null && (p.closestFriend.hasBeenPairedYet
                            || p.closestBuddyDistanceRaw * 1.5 < p.closestFriendDistanceRaw)) {
                        pairWithBuddy(point, "orphan pairing semi-close");
                    } else {
                        // pair with any yet free friends or buddies semi-close
                        EdgePoint closest = getClosest(point, true, false);
                        EdgePoint closestBud = getClosest(point, true, true);
                        if (closest != null && GEO.getDist(point, closest) < GEO.getDist(point, point.line.end)) {
                            pairTwo(point, closest, "orphan pairing closest");
                        }
                        if (!point.hasBeenPairedYet) {
                            if (closestBud != null && GEO.getDist(point, closestBud) < GEO.getDist(point, point.line.end)) {
                                pairTwo(point, closestBud, "orphan pairing closest buddy");
                            }
                        }
                        if (!point.hasBeenPairedYet) {
                            if (p.closestBuddy == null) {
                                if (p.closestFriend != null && GEO.getDist(point.line.end, p.closestFriend) < partOfAvg(5)) {
                                    EdgePoint np = getLineMidPoint(point, p.closestFriend);
                                    pairTwo(point, np, "orphan pairing midpoint");
                                }
                            } else if (!p.closestBuddy.hasBeenPairedYet) {
                                pairWithBuddy(point, "orphan pairing final");
                            }
                        }
                        if (!point.hasBeenPairedYet) {
                            pairWithEnd(point, "orphan pairing");
                        }
                    }
                }
            }
        }.pair(ROI);
    }

    private boolean angleTest(EdgePoint point, EdgePoint point2, int angle) {
        return GEO.getDirDifference(point.direction, GEO.getDirection(point, point2))
                + GEO.getDirDifference(point2.direction, GEO.getDirection(point, point2)) < angle;
    }

    private int partOfEdge(int i) {
        return ROI.outEdge.list.size() / i;
    }

    private int partOfAvg(int i) {
        return (int) (SET.targetsize * Math.PI / i);
        // double size = SET.avgCornerlessSize() == 0 ? SET.avgSize() : SET.avgCornerlessSize();
        //  return (int) GEO.circleCircumference(size) / i;
    }

    private double diam(int i) {
        return ROI.getDimension() / (double) i;
    }

    private void pairWithBuddy(EdgePoint p, String desc) {
        if (ROI.lineDoesntGoThroughVoid(p, p.pairings.closestBuddy, ROI.area)) {
            double pfact = GEO.getParallelFactor(p, p.pairings.closestBuddy);
            if (pfact < 0.25) {
                System.out.println("Paired " + p.x + "x," + p.y + "y with buddy (" + p.pairings.closestBuddy.x + "x," + p.pairings.closestBuddy.y + ") as " + desc);
                drawSegmentLine(p, p.pairings.closestBuddy);
                markPaired(p, p.pairings.closestBuddy);
            } else {
                System.out.println("Pairing failed (" + p.x + "x," + p.y + "y with buddy " + p.pairings.closestBuddy.x + "x," + p.pairings.closestBuddy.y + ") due to parallel)");
            }
        } else {
            System.out.println("Pairing failed (" + p.x + "x," + p.y + "y with buddy " + p.pairings.closestBuddy.x + "x," + p.pairings.closestBuddy.y + ") due to void)");
        }
    }

    private void pairWithEnd(EdgePoint p, String desc) {
        if (p.line.end == null) {
            System.out.println("NULL POINTER EXCEPTION " + p);
        } else if (ROI.getSize() > SET.avgCornerlessSize() * 2 && ((EdgePoint) p.line.end).angle < 180) {
            System.out.println("Paired " + p.x + "x," + p.y + "y with end (" + p.line.end.x + "x," + p.line.end.y + ") as " + desc);
            drawSegmentLine(p, p.line.end);
            p.hasBeenPairedYet = true;
            segmentedSomething = true;
        } else {
            System.out.println("Pairing failed (" + p.x + "x," + p.y + "y with end)");
        }
    }

    private void pairWithFriend(EdgePoint p, String desc) {
        System.out.println("Paired " + p.x + "x," + p.y + "y with friend (" + p.pairings.closestFriend.x + "x," + p.pairings.closestFriend.y + ") as " + desc);
        drawSegmentLine(p, p.pairings.closestFriend);
        markPaired(p, p.pairings.closestFriend);
    }

    private void pairWithLineFriend(EdgePoint p, String desc) {
        System.out.println("Paired " + p.x + "x," + p.y + "y with line friend (" + p.pairings.closestLineFriend.x + "x," + p.pairings.closestLineFriend.y + ") as " + desc);
        drawSegmentLine(p, p.pairings.closestLineFriend);
        markPaired(p, p.pairings.closestLineFriend);
    }

    private void pairTwo(EdgePoint p1, EdgePoint p2, String desc) {
        if (ROI.lineDoesntGoThroughVoid(p1, p2, ROI.area)) {
            System.out.println("Paired " + p1.x + "x," + p1.y + "y with other (" + p2.x + "x," + p2.y + ") as " + desc);
            drawSegmentLine(p1, p2);
            markPaired(p1, p2);
        } else {
            System.out.println("Pairing failed (" + p1.x + "x," + p1.y + "y with " + p2.x + "x," + p2.y + ") due to void)");
        }
    }

    private void justPair(EdgePoint p1, EdgePoint p2, String desc) {
        System.out.println("Paired " + p1.x + "x," + p1.y + "y with (" + p2.x + "x," + p2.y + ") as " + desc);
        drawSegmentLine(p1, p2);
        markPaired(p1, p2);
    }

    private void pairPool(List<EdgePoint> pool, String desc) {
        // if only two, draw line from one to another directly
        if (pool.size() == 2) {
            pairTwo(pool.get(0), pool.get(1), "bidirectional midpoint pairing " + desc);
        } // if many, make a center point
        else {
            System.out.println("Midpoint pairing " + desc + " for " + pool);
            Point midPoint = GEO.createCommonMidpoint(pool);
            segmentedSomething = true;
            pool.forEach(pp -> {
                drawSegmentLine(pp, midPoint);
                pp.hasBeenPairedYet = true;
            });
        }
    }

    private EdgePoint bestIntersector(EdgePoint point) {
        List<EdgePoint> possible = new ArrayList<>();
        point.pairings.intersectors.forEach(p -> {
            if (point.pairings.isPossiblePairing(p)) {
                if ((point.pairings.closestFriend == null || GEO.getDist(point, p) < GEO.getDist(point, point.pairings.closestFriend))
                        && GEO.getParallelFactor(point, p) <= 0.25) {
                    possible.add(p);
                }
            }
        });
        if (possible.isEmpty()) {
            return null;
        } else {
            possible.sort((Comparator<EdgePoint>) (EdgePoint p1, EdgePoint p2) -> (int) (GEO.getDist(point, p1) - GEO.getDist(point, p2)));
            return possible.get(0);
        }
    }

    private List<EdgePoint> intersectorPool(EdgePoint point, boolean onlySurePoints) {
        List<EdgePoint> allIntersections = new ArrayList<>();
        addAllUnpaired(point, allIntersections, onlySurePoints);
        point.pairings.intersectors.forEach(p -> {
            addAllUnpaired(p, allIntersections, onlySurePoints);
        });
        return allIntersections.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    private void addAllUnpaired(EdgePoint point, List array, boolean onlySurePoints) {
        for (int i = 0; i < point.pairings.intersectors.size(); i++) {
            EdgePoint pp = point.pairings.intersectors.get(i);
            if (!pp.hasBeenPairedYet) {
                array.add(pp);
            }
        }
        if (!onlySurePoints) {
            for (int i = 0; i < point.pairings.intersectorsSecondary.size(); i++) {
                EdgePoint pp = point.pairings.intersectorsSecondary.get(i);
                if (!pp.hasBeenPairedYet) {
                    array.add(pp);
                }
            }
        }
    }

    private void markPaired(EdgePoint p1, EdgePoint p2) {
        p1.hasBeenPairedYet = true;
        p2.hasBeenPairedYet = true;
        segmentedSomething = true;
    }

    private void drawSegmentLine(Point p1, Point p2) {
        new lineDrawer() {
            @Override
            public void action(int x, int y) {
                ROI.drawPoint(x, y, false, lineThickness);
            }
        }.drawLine(p1, p2);
    }

    private boolean onlyCommonIntersections(List<EdgePoint> pool) {
        EdgePoint p1, p2;
        for (int i = 0; i < pool.size(); i++) {
            p1 = pool.get(i);
            if (!p1.isUnsure) {
                for (int j = 0; j < pool.size(); j++) {
                    p2 = pool.get(j);
                    if (!p1.equals(p2) && !p1.pairings.intersectors.contains(p2) && !p1.pairings.intersectorsSecondary.contains(p2)) {
                        System.out.println("Only common intersections NOT detected for pool " + pool);
                        return false;
                    }
                }
            }
        }
        System.out.println("Only common intersections detected for pool " + pool);
        return true;
    }

    private EdgePoint getParallelFriend(EdgePoint point) {
        EdgePoint maxPoint = null, currentPoint;
        double maxValue = 1;
        for (int i = 0; i < point.pairings.possibleFriends.size(); i++) {
            currentPoint = point.pairings.possibleFriends.get(i);
            double diff = GEO.getParallelFactor(point, currentPoint);
            if (diff < maxValue) {
                maxValue = diff;
                maxPoint = currentPoint;
            }
        }
        if (maxPoint != null) {
            System.out.println("Parallel friend best match with " + maxValue + " w angles of " + point.direction + " and " + maxPoint.direction + " for " + point + ", match being " + maxPoint);
        } else {
            // System.out.println("Parallel friend best match not found, null");
        }
        return (maxValue < 0.1) ? maxPoint : null;
    }

    private EdgePoint getClosest(EdgePoint point, boolean onlyUnpaired, boolean buddies) {
        EdgePoint maxPoint = null, currentPoint;
        double maxValue = Integer.MAX_VALUE;
        List<EdgePoint> list = buddies ? ROI.edgeData.cornerCandidates : ROI.edgeData.cornerPoints;
        for (int i = 0; i < list.size(); i++) {
            currentPoint = list.get(i);
            if (!currentPoint.equals(point) && (!onlyUnpaired || !currentPoint.hasBeenPairedYet)) {
                double dist = GEO.getDist(point, currentPoint);
                if (dist < maxValue) {
                    maxValue = dist;
                    maxPoint = currentPoint;
                }
            }
        }
        return (maxValue < Integer.MAX_VALUE) ? maxPoint : null;
    }

    private EdgePoint getLineMidPoint(EdgePoint point, EdgePoint point2) {
        double direction = GEO.getDirection(point, GEO.getMidPoint(point.line.end, point2));
        Point np = ROI.findPointAtTheOppositeSide(point, (int) direction);
        return new EdgePoint(np.x, np.y);
    }
}
