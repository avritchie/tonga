package mainPackage.morphology;

import java.awt.Point;
import java.util.List;

public class ListArea {

    List<? extends Point> list;
    boolean[][] area;

    public ListArea(List<? extends Point> list, boolean[][] area) {
        this.list = list;
        this.area = area;
    }

    public List<? extends Point> getPoints() {
        return list;
    }

    protected Point findPosition(Point point) {
        for (int i = 0; i < list.size(); i++) {
            Point pp = list.get(i);
            if (point.x == pp.x && point.y == pp.y) {
                return pp;
            }
        }
        return null;
    }
}
