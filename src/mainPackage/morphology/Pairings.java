package mainPackage.morphology;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Pairings {

    public Pairings() {
        intersectors = new ArrayList<>();
        intersectorsSecondary = new ArrayList<>();
        possibleFriends = new ArrayList<>();
        possibleBuddies = new ArrayList<>();
        pairingMap = new HashMap<>();
        closestFriend = null;
        closestFriendDistance = 0;
        closestFriendDistanceRaw = 0;
        closestBuddy = null;
        closestBuddyDistance = 0;
        closestBuddyDistanceRaw = 0;
        closestAny = null;
        closestAnyDistance = 0;
        closestAnyDistanceRaw = 0;
        closestLineFriend = null;
        closestLineFriendDistance = 0;
        closestOpposite = null;
        closestOppositeDistance = 0;
        closestConcave = null;
        closestConcaveAngle = 360;
        closestConcaveDistance = 0;
        ownEndDistance = 0;
    }

    public HashMap<EdgePoint, Pairing> pairingMap; // pairing map
    public ArrayList<EdgePoint> closest; // closest point by raw distance
    public ArrayList<EdgePoint> closestEdge; // closest point by edge distance (from the opposite edge)
    public ArrayList<EdgePoint> closestLine; // closest point by line distance
    public List<EdgePoint> intersectors; // keiden muiden pisteiden viivojen kanssa leikkaa
    public List<EdgePoint> intersectorsSecondary; // keiden unsure-pisteiden viivojen kanssa leikkaa
    public EdgePoint closestFriend; // lähin vastakkaisen puolen varma piste
    public EdgePoint closestBuddy; // lähin vastakkaisen puolen epävarma piste
    public EdgePoint closestAny; // lähin vastakkaisen puolen mikä tahansa concave piste
    public EdgePoint closestLineFriend; // leikkauslinjasta missä tahansa kohdassa lähin piste;
    public EdgePoint closestOpposite; // 40 asteen säteellä directionista mihin kohtaan vastakkaista reunaa on lyhin matka;
    public EdgePoint closestConcave; // 15 asteen säteellä directionista missä kohdassa vastakkaista reunaa on suurin concaveness;
    public List<EdgePoint> possibleFriends; // kaikki ne, keiden kanssa linkkaus on mahdollinen
    public List<EdgePoint> possibleBuddies; // kaikki ne, keiden kanssa linkkaus on mahdollinen
    public int closestFriendDistance; // etäisyys tästä pisteestä REUNAA PITKIN alkaen vastakkaiselta puolelta
    public int closestBuddyDistance; // etäisyys tästä pisteestä REUNAA PITKIN alkaen vastakkaiselta puolelta
    public int closestAnyDistance; // etäisyys tästä pisteestä REUNAA PITKIN alkaen vastakkaiselta puolelta
    public double ownEndDistance; // etäisyys omasta vastakkaispisteestä (line.end)
    public double closestOppositeDistance; // etäisyys tästä pisteestä suoraan
    public double closestConcaveAngle; // kyseisen pisteen kulma suoraan
    public double closestConcaveDistance; // etäisyys tästä pisteestä suoraan
    public double closestFriendDistanceRaw; // etäisyys tästä pisteestä suoraan
    public double closestBuddyDistanceRaw; // etäisyys tästä pisteestä suoraan
    public double closestAnyDistanceRaw; // etäisyys tästä pisteestä suoraan
    public double closestLineFriendDistance; // etäisyys tästä pisteestä suoraan

    boolean isPossiblePairing(Point cp) {
        return possibleFriends.contains((EdgePoint) cp);
    }

    void fill() {
        closest = pairingMap.values().stream().sorted(Comparator.comparing(o -> o.rawDistance)).map(p -> p.with).collect(Collectors.toCollection(ArrayList::new));
        closestEdge = pairingMap.values().stream().sorted(Comparator.comparing(o -> o.edgeDistance)).map(p -> p.with).collect(Collectors.toCollection(ArrayList::new));
        closestLine = pairingMap.values().stream().sorted(Comparator.comparing(o -> o.lineDistance)).map(p -> p.with).collect(Collectors.toCollection(ArrayList::new));
    }

    EdgePoint closest(int i) {
        return closest.get(i);
    }

    double closestDist(int i) {
        return pairingMap.get(closest(i)).rawDistance;
    }

    EdgePoint closestEdge(int i) {
        return closestEdge.get(i);
    }

    EdgePoint closestLine(int i) {
        return closestLine.get(i);
    }
}
