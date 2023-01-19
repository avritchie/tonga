package mainPackage.counters;

import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.utils.RGB;
import mainPackage.utils.STAT;

public class Counters {

    public static Counter countRGBCMYK() {
        return new Counter("Count coloured pixels",
                new String[]{"Image", "Total", "Red", "Green", "Blue", "Yellow", "Fuchsia", "Cyan", "Black", "White"},
                new String[]{"The name of the image",
                    "Total number of pixels",
                    "Total number of red pixels",
                    "Total number of green pixels",
                    "Total number of blue pixels",
                    "Total number of yellow pixels",
                    "Total number of fuchsia pixels",
                    "Total number of cyan pixels",
                    "Total number of black pixels",
                    "Total number of white pixels"}) {
            @Override
            protected void pixelIterator32(int[] pixels, int p) {
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

    public static Counter countIFChannels() {
        return new Counter("Count RGB channels intensities while the background is black",
                new String[]{"Image", "Area %unit2", "Red sum", "Red ‰", "Green Sum", "Green ‰", "Blue sum", "Blue ‰"},
                new String[]{"The name of the image",
                    "Total size of the detected tissue area in %unit2",
                    "Total sum of the red channel intensity",
                    "The average red channel intensity in promilles",
                    "Total sum of the green channel intensity",
                    "The average green channel intensity in promilles",
                    "Total sum of the blue channel intensity",
                    "The average blue channel intensity in promilles",}) {

            int areapixels;
            double redvalue;
            double greenvalue;
            double bluevalue;

            @Override
            protected void preProcessor(ImageData targetImageToEdit) {
                areapixels = 0;
                redvalue = 0;
                greenvalue = 0;
                bluevalue = 0;
            }

            @Override
            protected void pixelIterator32(int[] pixels, int p) {
                int col = pixels[p];
                if (col != COL.BLACK) {
                    areapixels++;
                    redvalue += ((col >> 16) & 0xFF) / 255.;
                    greenvalue += ((col >> 8) & 0xFF) / 255.;
                    bluevalue += (col & 0xFF) / 255.;
                }
            }

            @Override
            protected void postProcessor(ImageData targetImage) {
                row[1] = scaleUnit(areapixels, 2);
                row[2] = redvalue;
                row[3] = STAT.decToProm(redvalue / areapixels);
                row[4] = greenvalue;
                row[5] = STAT.decToProm(greenvalue / areapixels);
                row[6] = bluevalue;
                row[7] = STAT.decToProm(bluevalue / areapixels);
            }
        };
    }

    public static Counter countRBStain() {
        return new Counter("Count red values",
                new String[]{"Image", "Tissue %", "<html><b>Stain ‰</b></html>", "Tissue %unit2", "Stain raw"},
                new String[]{"The name of the image",
                    "How big percentage of the image is considered to be tissue",
                    "How much staining intensity there is in the tissue area, in promilles",
                    "How big is the tissue area in the image in %unit2",
                    "The raw value of the total staining intensity detecteed in the image"}) {
            int totalpixels, blackpixels;
            double redvalue;

            @Override
            protected void preProcessor(ImageData targetImageToEdit) {
                totalpixels = 0;
                blackpixels = 0;
                redvalue = 0;
            }

            @Override
            protected void pixelIterator32(int[] pixels, int p) {
                int col = pixels[p];
                totalpixels++;
                if (col == COL.BLACK) {
                    blackpixels++;
                } else {
                    redvalue += (1 - ((col & 0xFF) / 255.));
                }
            }

            @Override
            protected void postProcessor(ImageData targetImage) {
                row[1] = STAT.decToPerc(1 - (double) blackpixels / totalpixels);
                row[2] = STAT.decToProm(redvalue / (totalpixels - blackpixels));
                row[3] = scaleUnit(totalpixels - blackpixels, 2);
                row[4] = redvalue;
            }
        };
    }

    public static Counter countBWBG() {
        return new Counter("Count background",
                new String[]{"Image", "Area %unit2", "Coverage %", "<html><b>Background %</b></html>", "Background avg raw"},
                new String[]{"The name of the image",
                    "The total size of the detected background area(s) in %unit2",
                    "The percentage of the detected background area(s) from the total image size",
                    "The average relative background intensity out of the maximum",
                    "The average raw intensity measurement from every pixel in the background area"}) {
            int white, bgpixels;
            int intval;
            double decval;

            @Override
            protected void preProcessor(ImageData targetImageToEdit) {
                white = 0;
                intval = 0;
                decval = 0;
            }

            @Override
            protected void pixelIterator16(short[] pixels, int p) {
                if (pixels[p] == COL.UWHITE) {
                    white++;
                } else {
                    intval += pixels[p] & 0xFFFF;
                    decval += (pixels[p] & 0xFFFF) / 65535.;
                }
            }

            @Override
            protected void pixelIterator32(int[] pixels, int p) {
                if (pixels[p] == COL.WHITE) {
                    white++;
                } else {
                    intval += pixels[p] & 0xFF;
                    decval += (pixels[p] & 0xFF) / 255.;
                }
            }

            @Override
            protected void postProcessor(ImageData targetImage) {
                bgpixels = targetImage.totalPixels() - white;
                row[1] = scaleUnit(bgpixels, 2);
                row[2] = STAT.decToPerc(((double) bgpixels) / targetImage.totalPixels());
                row[3] = STAT.decToPerc(decval / bgpixels);
                row[4] = intval / (double) bgpixels;
            }
        };
    }

    public static Counter analHisto() {
        return new Counter("Histogram values",
                new String[]{"Image", "Point", "Bright", "Red", "Green", "Blue", "Saturation"},
                new String[]{"The name of the image",
                    "The brightness level, between 0 (black) and 255 (fully bright)",
                    "The number of pixels in the image with the total brightness in this level",
                    "The number of pixels in the image with the red channel brightness in this level",
                    "The number of pixels in the image with the green channel brightness in this level",
                    "The number of pixels in the image with the blue channel brightness in this level",
                    "The number of pixels in the image with the saturation in this level",}) {
            TableData histo;

            @Override
            protected void preProcessor(ImageData targetImage) {
                histo = TableData.createTable(data.columns, data.descriptions, 256, imageName);
                histo.rows.forEach(i -> {
                    for (int j = 2; j < i.length; j++) {
                        i[j] = TableData.box(0);
                    }
                });
            }

            @Override
            protected void pixelIterator32(int[] pixels, int p) {
                int col = pixels[p];
                int bright = RGB.brightness(col);
                int saturation = (int) (RGB.saturation(col) * 255);
                int red = (col >> 16) & 0xFF;
                int green = (col >> 8) & 0xFF;
                int blue = (col) & 0xFF;
                histo.rowIntInc(bright, 2);
                histo.rowIntInc(red, 3);
                histo.rowIntInc(green, 4);
                histo.rowIntInc(blue, 5);
                histo.rowIntInc(saturation, 6);
            }

            @Override
            protected void postProcessor(ImageData targetImage) {
                data = histo;
            }
        };
    }
}
