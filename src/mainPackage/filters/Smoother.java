package mainPackage.filters;

import java.util.Arrays;
import mainPackage.Iterate;
import static mainPackage.filters.Filter.bgcol;

public class Smoother {

    public static FilterFast run() {
        return new FilterFast("Smoothed", bgcol) {
            @Override
            protected void processor() {
                System.arraycopy(in32, 0, out32, 0, inData.totalPixels());
                Iterate.pixels(this, (int pos) -> {
                    int c = in32[pos];
                    if (c != param.colorARGB[0]) {
                        smoothPixel(in32, out32, pos);
                    }
                });
            }

            private void smoothPixel(int[] in, int[] out, int pos) {
                int[][] hits = new int[3][3];
                for (int x = -1; x < 2; x++) {
                    for (int y = -1; y < 2; y++) {
                        try {
                            hits[x + 1][y + 1] = in[pos - x - (y * width)] != param.colorARGB[0] ? 1 : 0;
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            hits[x + 1][y + 1] = 0;
                        }
                    }
                }
                int[] ints = Arrays.stream(hits).map(v -> Arrays.stream(v).sum()).mapToInt(i -> i).toArray();
                int hitnumb = Arrays.stream(ints).sum();
                int emptys = (int) Arrays.stream(ints).filter(i -> i == 0).count();
                int partials = (int) Arrays.stream(ints).filter(i -> i == 2).count();
                if (hitnumb == 4 && emptys == 1 && partials == 2) {
                    out[pos] = param.colorARGB[0];
                }
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }
}
