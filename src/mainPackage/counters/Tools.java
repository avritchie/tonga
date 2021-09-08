package mainPackage.counters;

import java.util.ArrayList;
import java.util.List;
import mainPackage.TongaLayer;

public class Tools {

    public static int getHillMin(TableData histo, TongaLayer targetImage) {
        List<Integer> tops = new ArrayList<>();
        int dims = (int) (targetImage.layerImage.getHeight() * targetImage.layerImage.getWidth());
        for (int i = 1; i < histo.rows.size() - 1; i++) {
            int now = ((Integer) histo.rows.get(i)[2]);
            int next = ((Integer) histo.rows.get(i + 1)[2]);
            int prev = ((Integer) histo.rows.get(i - 1)[2]);
            if (next < now && prev < now) {
                tops.add(i);
            }
        }
        int fM = 0, sM = 0, mP = dims, mI = 0;
        for (int i = 1; i < tops.size(); i++) {
            int current = (Integer) histo.rows.get(tops.get(i))[2];
            int previous = (Integer) histo.rows.get(tops.get(i - 1))[2];
            if (current > previous) {
                fM = tops.get(i);
            } else {
                fM = tops.get(i - 1);
                sM = tops.get(i);
                break;
            }
        }
        for (int i = fM; i < sM; i++) {
            if (mP > ((Integer) histo.rows.get(i)[2])) {
                mP = ((Integer) histo.rows.get(i)[2]);
                mI = i;
            }
        }
        return mI;
    }
}
