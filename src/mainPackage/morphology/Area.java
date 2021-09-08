package mainPackage.morphology;

public class Area {

    public boolean[][] area; // 2D box with "true" in pixels part of the area
    public boolean[][] annotations; // initially empty, for storing drawed lines etc.
    public int width; // alueen leveys
    public int height; // alueen korkeus
    public int xstart; // koordinaatti "neliön" kulmassa
    public int ystart; // koordinaatti "neliön" kulmassa
    public int firstxpos; // ekan valkoisen pikselin x
    public int firstypos; // ekan valkoisen pikselin y

    public Area(boolean[][] area, int xstart, int ystart, int xfirst, int yfirst) {
        this.area = area;
        this.width = area.length;
        this.height = area[0].length;
        this.annotations = new boolean[area.length][area[0].length];
        this.xstart = xstart;
        this.ystart = ystart;
        this.firstxpos = xfirst;
        this.firstypos = yfirst;
    }
}
