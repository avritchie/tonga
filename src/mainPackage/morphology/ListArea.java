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
}
