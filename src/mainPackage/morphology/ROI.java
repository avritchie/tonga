package mainPackage.morphology;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import mainPackage.utils.GEO;
import mainPackage.utils.DRAW.lineDrawer;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.MappedImage;
import mainPackage.Tonga;
import mainPackage.utils.COL;
import mainPackage.utils.DRAW;
import mainPackage.utils.IMG;
import mainPackage.utils.RGB;
import mainPackage.utils.STAT;

public class ROI {

    public ListArea outEdge; //ulkoreunat
    public ListArea innEdge; //sisäreunat
    public EdgeAnalyzer edgeData; //kulmat ja muu reunatieto
    public ROISet set; //setti, johon tämä ROI kuuluu
    public Area area; //objektin täyttämä alue
    public int xcenter, ycenter;
    public int xcentroid, ycentroid;
    public ImageData originalImage;
    public ROI mask; //maski joka ulottuu arean ulkopuolelle
    public ROI parent; //inner objektien yms parent
    // alla olevat arvot ylikirjoitetaan tarvittaessa/käytettäessä
    private Area outArea; //objektin ulkopuolinen alue
    private Area innArea; //objektin sisäinen alue
    private int size;
    private double circularity;
    private double quantitativeValue; //arvo johon voi storee kamaa
    private STAT quantitativeSTAT; //statsit quantitative arvoista
    private int classID; //ryhmän arvo
    private double classifyValue; //arvo jota käytetään ryhmittelyyn
    private int color; //tämän roin väri

    public ROI(ImageData id, Area a) {
        this(id, a, COL.WHITE);
    }

    public ROI(ImageData id, Area a, int c) {
        this.originalImage = id;
        this.area = a;
        this.xcenter = area.width / 2 + area.xstart;
        this.ycenter = area.height / 2 + area.ystart;
        this.color = c;
    }

    public int getColor() {
        return color;
    }

    public int getSize() {
        if (size == 0) {
            calculateSize();
        }
        return size;
    }

    public int getWidth() {
        return area.width;
    }

    public int getHeight() {
        return area.height;
    }

    public int getDimension() {
        return Math.max(area.width, area.height);
    }

    public double[] getDimensionsInAngle(double a) {
        int[] ct = getCentroid();
        Point mp = new Point(ct[0], ct[1]);
        double aa = GEO.getPerpendicularAngle(a);
        double hyp = GEO.hypotenuse(getWidth(), getHeight());
        Point[] bounds = new Point[]{
            GEO.getPointInDirection(mp, a, hyp),
            GEO.getPointInDirection(mp, a, -hyp),
            GEO.getPointInDirection(mp, aa, hyp),
            GEO.getPointInDirection(mp, aa, -hyp)};
        double[] dists = new double[]{
            binaryBounds(this, bounds[0], mp),
            binaryBounds(this, bounds[1], mp),
            binaryBounds(this, bounds[2], mp),
            binaryBounds(this, bounds[3], mp)};
        double xx = dists[0] + dists[1];
        double yy = dists[2] + dists[3];
        return new double[]{xx, yy};
    }

    private static double binaryBounds(ROI roi, Point maxBound, Point midBound) {
        //binary search principle on finding a max distance between two points where
        //a perpendicular line still intersects with the ROI area
        Line ll = new Line(midBound, maxBound);
        double h = ll.length;
        double b = ll.length;
        lineDrawer ld = new DRAW.lineDrawer() {
            @Override
            public void action(int x, int y) {
                int xx = x - roi.area.xstart, yy = y - roi.area.ystart;
                if (xx >= 0 && yy >= 0 && xx < roi.area.area.length && yy < roi.area.area[0].length) {
                    if (roi.area.area[xx][yy]) {
                        abortLineDrawing();
                    }
                }
            }
        };
        while (b > 1) {
            Line l = ll.getPerpendicularAlong(h);
            l.setStartEndPoints(l.mid, ll.length);
            ld.drawLine(l.start, l.end);
            b /= 2;
            if (!ld.keepOnGoing) {
                h += b;
            } else {
                h -= b;
            }
            ld.keepOnGoing = true;
        }
        return h;
    }

    public int getClassID() {
        return classID;
    }

    public double getStainSum(double avgRawBg) {
        //subtract the background value
        return quantitativeValue - (avgRawBg * quantitativeSTAT.getN());
    }

    public double getStainAvg(double avgRawBg) {
        //subtract the background value
        return quantitativeValue / quantitativeSTAT.getN() - avgRawBg;
    }

    public double getStainSum() {
        return quantitativeValue;
    }

    public double getStainAvg() {
        return quantitativeValue / quantitativeSTAT.getN();
    }

    public STAT getStainSTAT() {
        return quantitativeSTAT;
    }

    public double getClassifierValue() {
        return classifyValue;
    }

    public boolean pointIsInside(Point p) {
        return area.area[p.x - area.xstart][p.y - area.ystart];
    }

    public boolean touchesImageEdges() {
        return (area.xstart == 0 || area.ystart == 0 || area.xstart + area.width == originalImage.width || area.ystart + area.height == originalImage.height);
    }

    public boolean touchesImageEdgesUL() {
        return (area.xstart == 0 || area.ystart == 0);
    }

    public boolean touchesImageEdgesBR() {
        return (area.xstart + area.width == originalImage.width || area.ystart + area.height == originalImage.height);
    }

    public int[] getCentroid() {
        if (xcentroid == 0 && ycentroid == 0) {
            calculateCentroid();
        }
        return new int[]{xcentroid, ycentroid};
    }

    public double getCircularity() {
        if (circularity == 0.0d) {
            circularity = calculateCircularity();
        }
        return circularity;
    }

    public Area getOutArea() {
        if (outArea == null) {
            outArea = EdgeTracer.outerFill(this);
        }
        return outArea;
    }

    public Area getInnArea() {
        if (innArea == null) {
            innArea = EdgeTracer.innerFill(this);
        }
        return innArea;
    }

    public int getCornerCount() {
        if (edgeData == null) {
            edgeData = new EdgeAnalyzer(this, set.targetsize);
        }
        return edgeData.cornerPoints.size();
    }

    public int getPossibleCornerCount() {
        if (edgeData == null) {
            edgeData = new EdgeAnalyzer(this, set.targetsize);
        }
        return edgeData.cornerCandidates.size();
    }

    private double calculateCircularity() {
        return 1 - Math.abs(1 - ((4 * Math.PI * getSize()) / Math.pow((area.width + area.height) / 2 * 3.125, 2)));
    }

    private void calculateSize() {
        int s = 0;
        for (int x = 0; x < area.width; x++) {
            for (int y = 0; y < area.height; y++) {
                if (area.area[x][y]) {
                    s++;
                }
            }
        }
        size = s;
    }

    private void calculateCentroid() {
        long xc = 0, yc = 0, s = 0;
        for (int x = 0; x < area.width; x++) {
            for (int y = 0; y < area.height; y++) {
                if (area.area[x][y]) {
                    xc += x;
                    yc += y;
                    s++;
                }
            }
        }
        xcentroid = area.xstart + (int) (xc / s);
        ycentroid = area.ystart + (int) (yc / s);
    }

    public void setClassifyValue(double value) {
        this.classifyValue = value;
    }

    public void setClass(int group) {
        this.classID = group;
    }

    public void classify(Classifier i) {
        setClass(i.classify(this));
    }

    public interface Classifier {

        int classify(ROI r);
    }

    protected void quantifyColor(ImageData img) {
        Quantifier quant = new Quantifier(this);
        Iterate.areaPixels(this, (int p) -> {
            double v = RGB.saturation(img.pixels32[p]);
            quant.measure(v);
        });
        quant.saveValues();
    }

    public void quantifyStain(ImageData img) {
        Quantifier quant = new Quantifier(this);
        Iterate.areaPixels(this, (int p) -> {
            boolean bits = img.bits == 16;
            int v = bits
                    ? img.pixels16[p] & COL.UWHITE
                    : RGB.brightness(img.pixels32[p]);
            quant.measure(v, bits);
        });
        quant.saveValues();
    }

    protected void quantifyMaskStain(ImageData exclude, int excludeColor, ImageData img) {
        Quantifier quant = new Quantifier(mask);
        Iterate.areaPixels(this.mask, (int p) -> {
            if (exclude.pixels32[p] == excludeColor) {
                boolean bits = img.bits == 16;
                int v = bits
                        ? img.pixels16[p] & COL.UWHITE
                        : RGB.brightness(img.pixels32[p]);
                quant.measure(v, bits);
            }
        });
        quant.saveValues();
    }

    public ImageData drawArea(ImageData source) {
        int padding = 10;
        int width = area.width + padding * 2, height = area.height + padding * 2;
        int[] outArr = new int[width * height];
        IMG.fillArray(outArr, width, height, COL.BLACK);
        for (int x = 0; x < area.width; x++) {
            for (int y = 0; y < area.height; y++) {
                if (area.area[x][y]) {
                    outArr[x + 10 + (y + 10) * width] = source.pixels32[(x + area.xstart) + ((y + area.ystart) * source.width)];
                }
            }
        }
        return new ImageData(outArr, width, height);
    }

    private class Quantifier {

        private int pixels = 0;
        private double stain = 0;
        private double[] pixelstain;

        public Quantifier(ROI roi) {
            pixelstain = new double[roi.area.height * roi.area.width];
        }

        public void measure(double v) {
            pixelstain[pixels] = v;
            stain += v;
            pixels++;
        }

        public void measure(int v, boolean is16bit) {
            pixelstain[pixels] = v;
            stain += v / (is16bit ? 65535. : 255.);
            pixels++;
        }

        private void pack() {
            pixelstain = Arrays.copyOf(pixelstain, pixels);
        }

        public void saveValues() {
            pack();
            quantitativeValue = stain;
            quantitativeSTAT = new STAT(pixelstain);
        }
    }

    protected void getExtendedROI(int r) {
        boolean[][] newArea = new boolean[area.width + r * 2][area.height + r * 2];
        boolean[][] outArea = getOutArea().area;
        outEdge.list.forEach(pp -> {
            for (int xx = -r; xx <= r; xx++) {
                for (int yy = -r; yy <= r; yy++) {
                    int xl = pp.x + xx, yl = pp.y + yy;
                    if (!newArea[xl + r][yl + r]) {
                        newArea[xl + r][yl + r] = xl >= 0 && yl >= 0
                                && xl < area.width && yl < area.height
                                && !outArea[xl + 1][yl + 1]
                                        ? false
                                        : GEO.getDist(pp, new Point(xl, yl)) <= r;
                    }
                }
            }
        });
        int axs = area.xstart < r ? Math.abs(area.xstart - r) : 0;
        int ays = area.ystart < r ? Math.abs(area.ystart - r) : 0;
        int axe = area.xstart - r + newArea.length;
        axe = axe > originalImage.width ? newArea.length - (axe - originalImage.width) : newArea.length;
        int aye = area.ystart - r + newArea[0].length;
        aye = aye > originalImage.height ? newArea[0].length - (aye - originalImage.height) : newArea[0].length;
        boolean[][] finalArea = new boolean[axe - axs][aye - ays];
        for (int j = axs; j < axe; j++) {
            finalArea[j - axs] = Arrays.copyOfRange(newArea[j], ays, aye);
        }
        Area roiArea = new Area(finalArea, Math.max(0, area.xstart - r), Math.max(0, area.ystart - r), Math.max(0, area.firstxpos - r), Math.max(0, area.firstypos - r));
        mask = new ROI(originalImage, roiArea, this.color);
    }

    protected void fixEdges(int width, int height, boolean separate) {
        Boolean[] edges = touchingEdges(width, height);
        if (Arrays.asList(edges).contains(true)) {
            int areaWidth = area.width - 1;
            int areaHeight = area.height - 1;
            int upLeft = -1, upRight = -1;
            int downLeft = -1, downRight = -1;
            int leftUp = -1, leftDown = -1;
            int rightUp = -1, rightDown = -1;
            for (int x = 0; x <= areaWidth; x++) {
                if (edges[1]) {
                    if (area.area[x][0] && upLeft == -1) {
                        upLeft = x;
                    }
                    if (area.area[areaWidth - x][0] && upRight == -1) {
                        upRight = areaWidth - x;
                    }
                }
                if (edges[3]) {
                    if (area.area[x][areaHeight] && downLeft == -1) {
                        downLeft = x;
                    }
                    if (area.area[areaWidth - x][areaHeight] && downRight == -1) {
                        downRight = areaWidth - x;
                    }
                }
            }
            for (int x = 0; x <= areaWidth; x++) {
                if (edges[1]) {
                    if (separate) {
                        area.area[x][1] = area.area[x][0];
                        area.area[x][0] = false;
                        if (x >= upLeft && x <= upRight) {
                            area.area[x][1] = true;
                        }
                    } else {
                        if (x >= upLeft && x <= upRight) {
                            area.area[x][0] = true;
                        }
                    }
                }
                if (edges[3]) {
                    if (separate) {
                        area.area[x][areaHeight - 1] = area.area[x][areaHeight];
                        area.area[x][areaHeight] = false;
                        if (x >= downLeft && x <= downRight) {
                            area.area[x][areaHeight - 1] = true;
                        }
                    } else {
                        if (x >= downLeft && x <= downRight) {
                            area.area[x][areaHeight] = true;
                        }
                    }
                }
            }
            for (int y = 0; y <= areaHeight; y++) {
                if (edges[0]) {
                    if (area.area[0][y] && leftDown == -1) {
                        leftDown = y;
                    }
                    if (area.area[0][areaHeight - y] && leftUp == -1) {
                        leftUp = areaHeight - y;
                    }
                }
                if (edges[2]) {
                    if (area.area[areaWidth][y] && rightDown == -1) {
                        rightDown = y;
                    }
                    if (area.area[areaWidth][areaHeight - y] && rightUp == -1) {
                        rightUp = areaHeight - y;
                    }
                }
            }
            for (int y = 0; y <= areaHeight; y++) {
                if (edges[0]) {
                    if (separate) {
                        area.area[1][y] = area.area[0][y];
                        area.area[0][y] = false;
                        if (y >= leftDown && y <= leftUp) {
                            area.area[1][y] = true;
                        }
                    } else {
                        if (y >= leftDown && y <= leftUp) {
                            area.area[0][y] = true;
                        }
                    }
                }
                if (edges[2]) {
                    if (separate) {
                        area.area[areaWidth - 1][y] = area.area[areaWidth][y];
                        area.area[areaWidth][y] = false;
                        if (y >= rightDown && y <= rightUp) {
                            area.area[areaWidth - 1][y] = true;
                        }
                    } else {
                        if (y >= rightDown && y <= rightUp) {
                            area.area[areaWidth][y] = true;
                        }
                    }
                }
            }
        }
    }

    protected int cornerPairs() {
        if (Tonga.log.isTraceEnabled()) {
            Tonga.log.trace("----\nVARMAT\n{}", getCornerCount());
            edgeData.cornerPoints.forEach(p -> {
                Tonga.log.trace(p.toString());
            });
            Tonga.log.trace("----\nEPÄVARMAT\n{}", getPossibleCornerCount());
            edgeData.cornerCandidates.forEach(p -> {
                Tonga.log.trace(p.toString());
            });
        }
        //////////////
        List<EdgePoint> corners = edgeData.cornerPoints;
        List<EdgePoint> cornersPossible = edgeData.cornerCandidates;
        EdgePoint cur, comp;
        ArrayList<EdgePoint> pairs = new ArrayList<>();
        double rad = Math.sqrt(getSize() / Math.PI);
        for (int c1 = 0; c1 < getCornerCount(); c1++) {
            cur = (EdgePoint) corners.get(c1);
            for (int c2 = 0; c2 < getCornerCount(); c2++) {
                comp = (EdgePoint) corners.get(c2);
                if (!comp.equals(cur)) {
                    if (GEO.getDist(cur, comp) < rad) {
                        if (Math.abs(cur.direction - comp.direction) > 90) {
                            pairs.add(cur);
                            pairs.add(comp);
                            //drawLine(cur, comp, true);
                        }
                    }
                }
            }
        }
        return (int) pairs.stream().distinct().count() / 2;
    }

    protected HashMap cornerPairsNew() {
        // this is unused
        Tonga.log.trace("----\n{}\n{}", getCornerCount(), getPossibleCornerCount());
        List<EdgePoint> corners = edgeData.cornerPoints;
        List<EdgePoint> cornersPossible = edgeData.cornerCandidates;
        EdgePoint cur, comp;
        HashMap<Integer, EdgePoint[]> pairs = new HashMap<>();
        for (int c1 = 0; c1 < getCornerCount(); c1++) {
            cur = (EdgePoint) corners.get(c1);
            for (int c2 = 0; c2 < getCornerCount(); c2++) {
                comp = (EdgePoint) corners.get(c2);
                if (!comp.equals(cur)) {
                    if (GEO.getDist(cur, comp) < 1) {
                        if (Math.abs(cur.direction - comp.direction) > 90) {
                            pairs.put(cur.hashCode() + comp.hashCode(), new EdgePoint[]{cur, comp});
                            //drawLine(cur, comp, true);
                        }
                    }
                }
            }
        }
        return pairs;
    }

    protected void drawGuideLines() {
        edgeData.cornerPoints.forEach(p -> {
            new lineDrawer() {
                @Override
                public void action(int x, int y) {
                    drawPoint(x, y, true, 2);
                }
            }.drawLine(p, GEO.getPointInDirection(p, (int) p.direction, 20));
        });
    }

    public void sectionBasedOnIntersections(ROISet set, int mode) {
        try {
            Segmentor segmentor = new Segmentor(this, set);
            switch (mode) {
                case 0:
                    segmentor.segment();
                    break;
                case 1:
                    segmentor.segmentStrong();
                    break;
                case 2:
                    segmentor.segmentSure();
                    break;
                case 3:
                    segmentor.segmentSuperSure();
                    break;
            }
        } catch (Exception ex) {
            Tonga.log.trace("Pairing issue for the object x{}y{}", xcenter, ycenter);
            Tonga.catchError(ex, "The concave point pairing logic crashed.");
        }
    }

    protected void findPossiblePairings(EdgePoint point) {
        edgeData.cornerPoints.forEach(p -> {
            if (!point.equals(p)) {
                if (lineDoesntGoThroughVoid(p, point, area)) {
                    point.pairings.possibleFriends.add(p);
                    point.pairings.pairingMap.put(p, new Pairing(point, p));
                }
            }
        });
        edgeData.cornerCandidates.forEach(p -> {
            if (!point.equals(p)) {
                if (lineDoesntGoThroughVoid(p, point, area)) {
                    point.pairings.possibleBuddies.add(p);
                    point.pairings.pairingMap.put(p, new Pairing(point, p));
                }
            }
        });
    }

    public double getLineDistance(EdgePoint thisPoint, EdgePoint otherPoint) {
        lineDrawer lineSearcher = new lineDrawer() {
            Double dist = Double.MAX_VALUE;

            @Override
            public Object returnData() {
                return dist;
            }

            @Override
            public void action(int x, int y) {
                double tdist = GEO.getDist(new Point(x, y), otherPoint);
                if (tdist < dist) {
                    dist = tdist;
                }
            }
        };
        lineSearcher.drawLine(thisPoint, thisPoint.line.end);
        return (Double) lineSearcher.returnData();
    }

    public Line getLongThinLine() {
        // if the object is long and thin, where is it pointing at?
        Point[] dirs = new Point[8];
        double itw = Math.max(area.width, area.height);
        for (int i = 0; i < itw; i++) {
            int xn = (int) (i * area.width / itw);
            int xr = area.width - 1 - (int) (i * area.width / itw);
            int yn = (int) (i * area.height / itw);
            int yr = area.height - 1 - (int) (i * area.height / itw);
            int hm = area.width / 2;
            int vm = area.height / 2;
            if (dirs[0] == null && area.area[xn][yn]) { // ⭨
                dirs[0] = new Point(xn, yn);
            }
            if (dirs[1] == null && area.area[xr][yn]) { // ⭩
                dirs[1] = new Point(xr, yn);
            }
            if (dirs[2] == null && area.area[xn][yr]) { // ⭧
                dirs[2] = new Point(xn, yr);
            }
            if (dirs[3] == null && area.area[xr][yr]) { // ⭦
                dirs[3] = new Point(xr, yr);
            }
            if (dirs[4] == null && area.area[xn][vm]) { // →
                dirs[4] = new Point(xn, vm);
            }
            if (dirs[5] == null && area.area[xr][vm]) { // ←
                dirs[5] = new Point(xr, vm);
            }
            if (dirs[6] == null && area.area[hm][yn]) { // ↓
                dirs[6] = new Point(hm, yn);
            }
            if (dirs[7] == null && area.area[hm][yr]) { // ↑
                dirs[7] = new Point(hm, yr);
            }
        }
        double diag1d = GEO.getDist(dirs[0], dirs[3]) / 1.41;
        double diag2d = GEO.getDist(dirs[1], dirs[2]) / 1.41;
        double diag3d = GEO.getDist(dirs[4], dirs[5]);
        double diag4d = GEO.getDist(dirs[6], dirs[7]);
        double maxDiag = Math.max(diag1d / diag2d, diag2d / diag1d);
        double maxHVdg = Math.max(diag3d / diag4d, diag4d / diag3d);
        double maxDim = Math.max(area.width / area.height, area.height / area.width);
        double maxx = Math.max(diag1d, Math.max(diag2d, Math.max(diag3d, diag4d)));
        double dir = diag1d == maxx ? 135 : diag2d == maxx ? 45 : diag3d == maxx ? 0 : diag4d == maxx ? 90 : -1;
        if (dir != -1 && ((maxDim >= 6 || maxHVdg >= 6 || maxDiag >= 6) || (maxDim >= 2 && (maxDiag > 3 || maxHVdg > 3)))) {
            Line l = new Line(xcenter, ycenter, dir);
            l.start = GEO.getPointInDirection(new Point(xcenter, ycenter), (int) dir, (int) (-maxx * 2));
            l.end = GEO.getPointInDirection(new Point(xcenter, ycenter), (int) dir, (int) (maxx * 2));
            return l;
        }
        return null;
    }

    protected void findClosestOpposite(EdgePoint point) {
        //look 20-40-60 degrees left and right towards the direction
        //see if by changing the direction just a little there would be a collision point much closer on the opposite edge
        double dmin = Integer.MAX_VALUE;
        EdgePoint fpnt = null;
        //int fin = 360;
        int rad = 20;
        while (rad <= 60) {
            for (int i = -rad; i < rad; i++) {
                if (i <= -(rad - 20) || i >= rad - 20) {
                    EdgePoint found = findPointAtTheOppositeSide(point, (int) point.direction + i);
                    double dist = GEO.getDist(found, point);
                    if (dist < dmin && GEO.getListDifference(outEdge.list.indexOf(point), outEdge.list.indexOf(found), outEdge.list.size()) > dist * 2) {
                        fpnt = found;
                        dmin = dist;
                        //fin = i;
                    }
                }
            }
            if (fpnt != null) {
                double newDirection = GEO.getDirection(point, fpnt);
                double dirIncrease = Math.abs(point.direction - newDirection);
                double distDecrease = point.pairings.ownEndDistance / (double) dmin;
                if (dirIncrease < 30
                        || dirIncrease < 45 && distDecrease > 2.5
                        || dirIncrease < 60 && distDecrease > 5) {
                    if (point.pairings.closestOpposite == null || point.pairings.closestOppositeDistance > dmin * 2) {
                        point.pairings.closestOpposite = fpnt;
                        point.pairings.closestOppositeDistance = dmin;
                    }
                }
            }
            rad += 20;
        }
    }

    protected void findConcaveOpposite(EdgePoint point) {
        //look "span" or "span"2x left and right towards the direction
        //see what would be the minimum angle found next to the point on the opposite edge
        //this is useful for making connections to points which were not detected as "true" concave points (=false negative)
        double amin = 360;
        EdgePoint fpnt = null;
        Point start = findPointAtTheOppositeSide(point, (int) point.direction);
        while (point.pairings.closestConcave == null || point.pairings.closestConcaveAngle > 160) {
            int span = EdgeAnalyzer.spanSize(set.targetsize);
            Point edgePoint = outEdge.findPosition(start);
            int base = outEdge.list.indexOf(edgePoint);
            ListArea list = outEdge;
            if (edgePoint == null && innEdge != null) {
                edgePoint = innEdge.findPosition(start);
                base = innEdge.list.indexOf(edgePoint);
                list = innEdge;
            }
            if (edgePoint != null) {
                int fspan = span;
                int lspan = list.list.size();
                while (fspan <= span * 3) {
                    Point prev = edgePoint;
                    for (int i = -fspan; i < fspan; i++) {
                        if (i <= -(fspan - span) || i >= (fspan - span)) {
                            EdgePoint found = i > lspan ? null : EdgeAnalyzer.getPos(list.list, base, i);
                            if (found == null) {
                                return;
                            } else if (edgeData.cornerPoints.contains(found)
                                    || edgeData.cornerCandidates.contains(found)
                                    || edgeData.primaryCornerPoints.contains(found)) {
                                // if the concave point is already detected as a real concave point, the whole process is redundant
                                // thus it is cancelled and null returned
                                point.pairings.closestConcave = null;
                                point.pairings.closestConcaveAngle = 360;
                                point.pairings.closestConcaveDistance = 0;
                                return;
                            } else if (found.angle < amin && GEO.getDist(found, prev) < 2) {
                                if (lineDoesntGoThroughVoid(point, found, area)) {
                                    fpnt = found;
                                    amin = found.angle;
                                }
                            }
                            prev = found;
                        }
                    }
                    if (fpnt != null && point.pairings.closestConcaveAngle > 160 && fspan <= span * 2) {
                        point.pairings.closestConcave = fpnt;
                        point.pairings.closestConcaveAngle = amin;
                        point.pairings.closestConcaveDistance = GEO.getDist(point, fpnt);
                    }
                    fspan += span;
                }
            }
            if (point.pairings.closestOpposite != null && start != point.pairings.closestOpposite) {
                start = point.pairings.closestOpposite;
            } else {
                return;
            }
        }
    }

    protected boolean lineDoesntGoThroughVoid(EdgePoint p1, EdgePoint p2, Area area) {
        boolean[] retIsOn = new boolean[]{true};
        new lineDrawer() {
            @Override
            public void action(int x, int y) {
                if (!area.area[x][y] && (GEO.getDist(new Point(x, y), p2) > 5 && GEO.getDist(new Point(x, y), p1) > 5)) {
                    retIsOn[0] = false;
                }
            }
        }.drawLine(p1, p2);
        Tonga.log.trace("For x{}y{} and x{}y{} the line void result is {}", p1.x, p1.y, p2.x, p2.y, retIsOn[0]);
        return retIsOn[0];
    }

    protected void drawPointLines() {
        lineDrawer drawer = new lineDrawer() {
            @Override
            public void action(int x, int y) {
                try {
                    drawPoint(x, y, true, 1);
                    if (!area.area[x][y]) {
                        abortLineDrawing();
                    }
                } catch (ArrayIndexOutOfBoundsException ex) {
                    abortLineDrawing();
                }
            }
        };
        edgeData.cornerPoints.forEach(p -> {
            drawer.drawLine(p, p.line.end);
        });
        edgeData.cornerCandidates.forEach(p -> {
            drawer.drawLine(p, p.line.end);
        });
    }

    protected void findIntersectionOnEdges(EdgePoint thisPoint) {
        EdgePoint found = findPointAtTheOppositeSide(thisPoint, (int) thisPoint.direction);
        if (found != null) {
            thisPoint.line.start = thisPoint;
            thisPoint.line.end = found;
            thisPoint.pairings.ownEndDistance = GEO.getDist(thisPoint, found);
            findFriendsAndBuddies(thisPoint, found);
            findPointClosestToTheLine(thisPoint, found);
        }
    }

    protected void findIntersectionOnOthers(EdgePoint point) {
        Tonga.log.trace("Line intersection for {}", point);
        edgeData.cornerPoints.forEach(p -> {
            if (!point.equals(p)) {
                Tonga.log.trace("Check with {}", p);
                Point inter = Line.intersection(point.line, p.line);
                Tonga.log.trace("Suggestion was {}", inter);
                if (inter != null && area.area[inter.x][inter.y]) {
                    Tonga.log.trace("Found intersection at the ROI area at {}", inter);
                    EdgePoint np = new EdgePoint(inter.x, inter.y);
                    if (!edgeData.interSectionPoints.contains(np)) {
                        edgeData.interSectionPoints.add(np);
                    }
                    point.pairings.intersectors.add(p);
                } else {
                    Tonga.log.trace("There lines do not intersect in the ROI area");
                }
            }
        });
        edgeData.cornerCandidates.forEach(p -> {
            Point inter = Line.intersection(point.line, p.line);
            try {
                if (area.area[inter.x][inter.y]) {
                    EdgePoint np = new EdgePoint(inter.x, inter.y);
                    if (!edgeData.interSectionPoints.contains(np)) {
                        edgeData.interSectionPoints.add(np);
                    }
                    point.pairings.intersectorsSecondary.add(p);
                }
            } catch (Exception ex) {
            }
        });
    }

    protected void findPointClosestToTheLine(EdgePoint thisPoint, EdgePoint excludeThis) {
        Point newPoint = GEO.getPointInDirection(thisPoint, (int) thisPoint.direction, area.area.length + area.area[0].length);
        // finds edgepoint closest to line at any position of the line and saves it
        lineDrawer lineSearcher = new lineDrawer() {
            EdgePoint friend = null;
            Double friendDist = Double.MAX_VALUE;

            @Override
            public Object returnData() {
                ArrayList<Object> ret = new ArrayList<>();
                ret.add(friend);
                ret.add(friendDist);
                return ret;
            }

            @Override
            public void action(int x, int y) {
                if (excludeThis.x == x && excludeThis.y == y) {
                    abortLineDrawing();
                }
                EdgePoint possibleFriend = findClosestFriend(thisPoint, friend, friendDist, x, y);
                if (possibleFriend != null && possibleFriend != thisPoint.pairings.closestFriend) {
                    friend = possibleFriend;
                    friendDist = Math.min(friendDist, GEO.getDist(new Point(x, y), friend));
                }
            }
        };
        ArrayList<Object> lineFriendData = (ArrayList<Object>) lineSearcher.drawLine(thisPoint, newPoint);
        thisPoint.pairings.closestLineFriend = (EdgePoint) lineFriendData.get(0);
        thisPoint.pairings.closestLineFriendDistance = (Double) lineFriendData.get(1);
        Tonga.log.trace("Closest line friend (w dist {}) at {}", thisPoint.pairings.closestLineFriendDistance, thisPoint.pairings.closestLineFriend);
    }

    protected void findFriendsAndBuddies(EdgePoint thisPoint, EdgePoint found) {
        // find the nearest friend next to the discovered point
        int startIndex = outEdge.list.indexOf(found);
        for (int i = 0; i < outEdge.list.size() / 2; i++) {
            Point prevPos = outEdge.list.get(edgePosition(startIndex - i, outEdge.list));
            Point nextPos = outEdge.list.get(edgePosition(startIndex + i, outEdge.list));
            evaluatePositionForFriendBuddy(prevPos, thisPoint, i);
            evaluatePositionForFriendBuddy(nextPos, thisPoint, i);
            if (thisPoint.pairings.closestBuddy != null && thisPoint.pairings.closestFriend != null) {
                break;
            }
        }
    }

    protected void findOuterEdges() {
        outEdge = EdgeTracer.outerEdges(this);
    }

    protected void findInnerEdges() {
        innEdge = EdgeTracer.innerEdges(this);
    }

    protected void filterCornersNotTouching(List<EdgePoint> points, ImageData mMask) {
        // remove edgepoints which are not touching white on the mMask
        Iterator<? extends Point> pIt = points.iterator();
        while (pIt.hasNext()) {
            Point t = pIt.next();
            int p = mMask.width * (t.y + area.ystart) + (t.x + area.xstart);
            if (mMask.pixels32[p] != COL.WHITE) {
                pIt.remove();
            }
        }
    }

    @Deprecated
    protected void filterUnsureCorners(ListArea larea) {
        // remove edgepoints which are not touching white on the mMask
        Iterator<? extends Point> pIt = larea.list.iterator();
        while (pIt.hasNext()) {
            EdgePoint t = (EdgePoint) pIt.next();
            if (t.isUnsure) {
                pIt.remove();
            }
        }
    }

    private void evaluatePositionForFriendBuddy(Point point, EdgePoint thisPoint, int i) {
        if (!point.equals(thisPoint)) {
            Pairings tp = thisPoint.pairings;
            if (tp.isPossiblePairing(point) && tp.closestFriend == null) {
                thisPoint.setAsFriend((EdgePoint) point, i);
            }
            if (edgeData.cornerCandidates.contains((EdgePoint) point) && tp.closestBuddy == null) {
                if (lineDoesntGoThroughVoid(thisPoint, (EdgePoint) point, area)) {
                    thisPoint.setAsBuddy((EdgePoint) point, i);
                }
            }
        }
    }

    protected EdgePoint findPointAtTheOppositeSide(EdgePoint thisPoint, int direction) {
        Point newPoint = GEO.getPointInDirection(thisPoint, direction, area.area.length + area.area[0].length);
        // finds a point in the outerEdge that intersects with the line drawn from point to the angle
        // if found, returns it; if not, returns null
        lineDrawer pointSearcher = new lineDrawer() {
            int px = -1, py = -1;
            EdgePoint found;

            @Override
            public Object returnData() {
                return found;
            }

            @Override
            public void action(int x, int y) {
                try {
                    if (outEdge.area[x][y] || (innEdge != null && innEdge.area[x][y])) {
                        px = x;
                        py = y;
                    }
                    if (!area.area[x][y]) {
                        found = getIntersectionPointOnEdge(px, py, thisPoint);
                        abortLineDrawing();
                    }
                } catch (IndexOutOfBoundsException ex) {
                    found = getIntersectionPointOnEdge(px, py, thisPoint);
                    abortLineDrawing();
                }
            }
        };
        return (EdgePoint) pointSearcher.drawLine(thisPoint, newPoint);
    }

    public static int edgePosition(int p, List<? extends Point> edge) {
        if (p < 0) {
            p += edge.size();
        } else if (p >= edge.size()) {
            p -= edge.size();
        }
        return p;
    }

    public static int edgePosition(int p, boolean[] edge) {
        if (p < 0) {
            p += edge.length;
        } else if (p >= edge.length) {
            p -= edge.length;
        }
        return p;
    }

    private EdgePoint getIntersectionPointOnEdge(int px, int py, EdgePoint thisPoint) {
        if (px != -1 || py != -1) {
            Point resultPoint = new Point(px, py);
            //edgeData.interSectionPoints.add(resultPoint);
            Point edgePoint = outEdge.findPosition(resultPoint);
            if (edgePoint == null && innEdge != null) {
                edgePoint = innEdge.findPosition(resultPoint);
            }
            return (EdgePoint) edgePoint;
        }
        return null;
    }

    public void drawPoint(int x, int y, boolean annotationLayer, int thickness) {
        // thickness=1 -> one pixel thickness
        int rad = thickness - 1;
        for (int i = -rad; i <= rad; i++) {
            for (int j = -rad; j <= rad; j++) {
                try {
                    if (annotationLayer) {
                        area.annotations[x + i][y + j] = true;
                    } else {
                        area.area[x + i][y + j] = false;
                    }
                } catch (ArrayIndexOutOfBoundsException ex) {
                    // do nothing if out of bounds
                }
            }
        }
    }

    private Boolean[] touchingEdges(int width, int height) {
        return new Boolean[]{area.xstart == 0, area.ystart == 0,
            (area.xstart + area.area.length) == width,
            (area.ystart + area.area[0].length) == height};
    }

    private EdgePoint findClosestFriend(EdgePoint thisPoint, EdgePoint bestMatch, double bestMatchDist, int x, int y) {
        double dist;
        EdgePoint fav;
        if (bestMatch == null) {
            dist = Double.MAX_VALUE;
            fav = null;
        } else {
            dist = bestMatchDist;
            fav = bestMatch;
        }
        for (EdgePoint cp : edgeData.cornerPoints) {
            if (thisPoint.pairings.isPossiblePairing(cp)) {
                double dd = GEO.getDist(cp, new Point(x, y));
                if (dd < dist) {
                    dist = dd;
                    fav = cp;
                }
            }
        }
        return fav;
    }
}
