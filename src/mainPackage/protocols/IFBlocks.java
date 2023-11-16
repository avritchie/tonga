package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator;
import static mainPackage.PanelCreator.ControlType.LAYER;
import static mainPackage.PanelCreator.ControlType.SPINNER;
import static mainPackage.PanelCreator.ControlType.TOGGLE;
import mainPackage.counters.Counter;
import mainPackage.utils.COL;
import mainPackage.utils.RGB;

/**
 *
 * @author aritchie
 */
public class IFBlocks extends Protocol {

    @Override
    public String getName() {
        return "IF Blocks";
    }

    @Override
    protected PanelCreator.ControlReference[] getParameters() {
        return new PanelCreator.ControlReference[]{
            new PanelCreator.ControlReference(LAYER, "The original layer"),
            new PanelCreator.ControlReference(TOGGLE, "Exclude black areas", 1, new int[]{2, 1}),
            new PanelCreator.ControlReference(TOGGLE, "Exclude insufficient samples", 1),
            new PanelCreator.ControlReference(SPINNER, "Radius", 10)};
    }

    @Override
    protected Processor getProcessor() {
        return new ProcessorFast("Blocks", 1) {
            int[][][] grid;
            int rad = param.spinner[0];
            boolean remove = param.toggle[0];
            boolean insuff = param.toggle[1];
            int blocksx, blocksy;

            @Override
            protected void methodInit() {
                blocksx = (int) Math.ceil(sourceWidth[0] / (double) rad);
                blocksy = (int) Math.ceil(sourceHeight[0] / (double) rad);
                grid = new int[blocksx][][];
                for (int x = 0; x < blocksx; x++) {
                    grid[x] = new int[blocksy][];
                    for (int y = 0; y < blocksy; y++) {
                        grid[x][y] = new int[4];
                    }
                }
            }

            @Override
            protected void methodCore(int p) {
                int x = (p % inImage[0].width) / rad;
                int y = (p / inImage[0].width) / rad;
                int c = inImage[0].pixels32[p];
                if (!remove || RGB.brightness(c) > 0) {
                    grid[x][y][0] += (c >> 16) & 0xFF;
                    grid[x][y][1] += (c >> 8) & 0xFF;
                    grid[x][y][2] += (c) & 0xFF;
                    grid[x][y][3] += 1;
                }
            }

            @Override
            protected void methodFinal() {
                for (int x = 0; x < blocksx; x++) {
                    for (int y = 0; y < blocksy; y++) {
                        int col = COL.BLACK;
                        int count = grid[x][y][3];
                        if (count >= Math.max(1, !insuff ? 1 : rad * rad * 0.1)) {
                            int r = grid[x][y][0] / count;
                            int g = grid[x][y][1] / count;
                            int b = grid[x][y][2] / count;
                            col = RGB.argb(r, g, b);
                        }
                        for (int xx = 0; xx < rad; xx++) {
                            for (int yy = 0; yy < rad; yy++) {
                                if (x * rad + xx < sourceWidth[0] && (y * rad + yy) < sourceHeight[0]) {
                                    int p = x * rad + xx + (y * rad + yy) * sourceWidth[0];
                                    outImage[0].pixels32[p] = col;
                                }
                            }
                        }
                    }
                }
                addResultData(sourceImage, outImage[0]);
            }

            @Override
            protected Counter processorCounter() {
                return new Counter("Estimate nucleus size", new String[]{"Image", "X", "Y", "Red", "Green", "Blue"},
                        new String[]{"The name of the image",
                            "The X coordinate of the block",
                            "The Y coordinate of the block",
                            "Red channel value of the block",
                            "Green channel value of the block",
                            "Blue channel value of the block"}) {

                    @Override
                    protected void pixelProcessor(ImageData targetImage) {
                        String name = row[0].toString();
                        data.delLastRow();
                        for (int x = 0; x < blocksx; x++) {
                            for (int y = 0; y < blocksy; y++) {
                                int count = grid[x][y][3];
                                if (!insuff || count >= Math.max(1, rad * rad * 0.1)) {
                                    int r = count > 0 ? grid[x][y][0] / count : 0;
                                    int g = count > 0 ? grid[x][y][1] / count : 0;
                                    int b = count > 0 ? grid[x][y][2] / count : 0;
                                    row = data.newRow(name);
                                    row[1] = x;
                                    row[2] = y;
                                    row[3] = r;
                                    row[4] = g;
                                    row[5] = b;
                                }
                            }
                        }
                    }
                };
            }
        };
    }
}
