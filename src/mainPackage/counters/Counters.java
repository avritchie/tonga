package mainPackage.counters;

import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.utils.RGB;
import mainPackage.utils.STAT;

public class Counters {

    public static Counter countRGBCMYK() {
        return new Counter("Count coloured pixels", new String[]{"Image", "Total",
            "Red", "Green", "Blue", "Yellow", "Fuchsia", "Cyan", "Black", "White"}) {
            @Override
            protected void pixelIterator32(int[] pixels, int p, Object[] row) {
                int col = pixels[p];
                row[1] = ((Integer) row[1]) + 1;
                if (col == COL.RED) {
                    row[2] = ((Integer) row[2]) + 1;
                }
                if (col == (COL.GREEN)) {
                    row[3] = ((Integer) row[3]) + 1;
                }
                if (col == COL.BLUE) {
                    row[4] = ((Integer) row[4]) + 1;
                }
                if (col == COL.YELLOW) {
                    row[5] = ((Integer) row[5]) + 1;
                }
                if (col == COL.FUCHSIA) {
                    row[6] = ((Integer) row[6]) + 1;
                }
                if (col == COL.CYAN) {
                    row[7] = ((Integer) row[7]) + 1;
                }
                if (col == COL.BLACK) {
                    row[8] = ((Integer) row[8]) + 1;
                }
                if (col == COL.WHITE) {
                    row[9] = ((Integer) row[9]) + 1;
                }
            }
        };
    }

    public static Counter countRBStain() {
        return new Counter("Count red values", new String[]{"Image", "Tissue %", "<html><b>Stain ‰</b></html>", "Tissue px", "Stain raw"}) {
            int totalpixels, blackpixels;
            double redvalue;

            @Override
            protected void preProcessor(ImageData targetImage, Object[] rowToEdit) {
                totalpixels = 0;
                blackpixels = 0;
                redvalue = 0;
            }

            @Override
            protected void pixelIterator32(int[] pixels, int p, Object[] row) {
                int col = pixels[p];
                totalpixels++;
                if (col == COL.BLACK) {
                    blackpixels++;
                } else {
                    redvalue += (1 - ((col & 0xFF) / 255.));
                }
            }

            @Override
            protected void postProcessor(ImageData targetImage, Object[] row) {
                row[1] = STAT.decToPerc(1 - (double) blackpixels / totalpixels);
                row[2] = STAT.decToProm(redvalue / (totalpixels - blackpixels));
                row[3] = totalpixels - blackpixels;
                row[4] = redvalue;
            }
        };
    }

    public static Counter countBWBG() {
        return new Counter("Count background", new String[]{"Image", "Area px", "Coverage %", "<html><b>Background %</b></html>", "Background avg raw"}) {
            int white, bgpixels;
            int intval;
            double decval;

            @Override
            protected void preProcessor(ImageData targetImage, Object[] rowToEdit) {
                white = 0;
                intval = 0;
                decval = 0;
            }

            @Override
            protected void pixelIterator16(short[] pixels, int p, Object[] row) {
                if (pixels[p] == COL.UWHITE) {
                    white++;
                } else {
                    intval += pixels[p] & 0xFFFF;
                    decval += (pixels[p] & 0xFFFF) / 65535.;
                }
            }

            @Override
            protected void pixelIterator32(int[] pixels, int p, Object[] row) {
                if (pixels[p] == COL.WHITE) {
                    white++;
                } else {
                    intval += pixels[p] & 0xFF;
                    decval += (pixels[p] & 0xFF) / 255.;
                }
            }

            @Override
            protected void postProcessor(ImageData targetImage, Object[] row) {
                bgpixels = targetImage.totalPixels() - white;
                row[1] = bgpixels;
                row[2] = STAT.decToPerc(((double) bgpixels) / targetImage.totalPixels());
                row[3] = STAT.decToPerc(decval / bgpixels);
                row[4] = intval / (double) bgpixels;
            }
        };
    }

    public static Counter analHisto() {
        return new Counter("Histogram values", new String[]{"Image", "Point", "Bright", "Red", "Green", "Blue", "Saturation"}) {
            TableData histo;

            @Override
            protected void preProcessor(ImageData targetImage, Object[] row) {
                histo = createTable(data.columns, 256, imageName);
                histo.rows.forEach(i -> {
                    for (int j = 2; j < i.length; j++) {
                        i[j] = box(0);
                    }
                });
            }

            @Override
            protected void pixelIterator32(int[] pixels, int p, Object[] row) {
                int col = pixels[p];
                int bright = RGB.brightness(col);
                int saturation = (int) (RGB.saturation(col) * 255);
                int red = (col >> 16) & 0xFF;
                int green = (col >> 8) & 0xFF;
                int blue = (col) & 0xFF;
                rowIntInc(histo, bright, 2);
                rowIntInc(histo, red, 3);
                rowIntInc(histo, green, 4);
                rowIntInc(histo, blue, 5);
                rowIntInc(histo, saturation, 6);
            }

            @Override
            protected void postProcessor(ImageData targetImage, Object[] row) {
                data = histo;
            }
        };
    }
}
