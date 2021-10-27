/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mainPackage.morphology;

import java.util.ArrayList;
import java.util.List;

public class CellSet extends ROISet {

    public CellSet(List<ROI> list, int width, int height) {
        super(list, width, height);
        this.list = nestListROIsToCells((List<ROI>) list);
        findOuterEdges();
        fillInnerHoles();
        analyzeCorners();
        estimateCellCounts();
    }

    public CellSet(ROISet set) {
        this(set.list, set.width, set.height);
    }

    public int totalCellCount() {
        return list.stream().mapToInt(o -> ((Cell) o).getCellEstimate()).sum();
    }

    private void estimateCellCounts() {
        double avg = avgCornerlessSize();
        list.forEach(o -> {
            int count;
            int pairs = o.cornerPairs();
            double ratio = (o.getSize() / avg);
            if (avg == 0) {
                count = pairs + 1;
            } else if (ratio < 2 && o.getCornerCount() == 0) {
                count = 1;
            } else if (pairs == 1) {
                if (ratio > 1) {
                    count = 2;
                } else {
                    count = 1;
                }
            } else if (o.getCornerCount() > 1) {
                count = (int) (Math.round(pairs + 1) * 0.7 + (ratio * 1.5) * 0.3);
            } else {
                count = (int) Math.floor(ratio * 1.33);
            }
            ((Cell) o).setCellEstimate(Math.max(1, count));
        });
    }

    private List<ROI> nestListROIsToCells(List<ROI> list) {
        List<ROI> newList = new ArrayList<>();
        list.forEach(o -> {
            newList.add(new Cell(o));
        });
        return newList;
    }

}
