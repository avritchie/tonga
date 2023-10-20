package mainPackage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import loci.common.DebugTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.ChannelMerger;
import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.gui.AWTImageTools;
import loci.formats.gui.BufferedImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.services.OMEXMLService;
import mainPackage.utils.HISTO;
import mainPackage.utils.RGB;
import ome.units.quantity.Length;
import ome.xml.model.primitives.Color;

public class StackImporter {

    protected static OMEXMLService xmlService;
    protected static Map<String, String> cols;
    protected static Loader LOADER;

    public static void boot() {
        LOADER = Tonga.loader();
        DebugTools.enableLogging("ERROR");
        try {
            xmlService = ((OMEXMLService) new ServiceFactory().getInstance(OMEXMLService.class));
            cols = new HashMap<>();
            cols.put("0x00ff00ff", "Green");
            cols.put("0x0000ffff", "Blue");
            cols.put("0xff0000ff", "Red");
            cols.put("0xffff00ff", "Yellow");
            cols.put("0xff00ffff", "Fuchsia");
            cols.put("0x00ffffff", "Cyan");
            cols.put("0xffffffff", "White");
            cols.put("0xff80ffff", "Pink");
            cols.put("0xffff80ff", "Light Yellow");
            cols.put("0x80ffffff", "Light Blue");
            cols.put("0xff8080ff", "Light Red");
            cols.put("0x80ff80ff", "Light Green");
            cols.put("0x8080ffff", "Violet");
            Tonga.log.info("Stack importer initialized successfully");
        } catch (DependencyException ex) {
            Tonga.catchError(ex);
        }
    }

    public static TongaImage[] openFile(File file) throws IOException, FormatException, ServiceException {
        List<TongaImage> returnableImages;
        BufferedImageReader input;
        OMEXMLMetadata xml;
        Color channelColor;
        int imageNumber; // images in a file
        int channelNumber; // channels in a image
        int splitChannelNumber; // how many new channels were split
        int timeNumber; // time points in a image
        int sliceNumber; // z-slices in a channel
        int bitNumber; // 16 for 16-bit
        int maxPixelValue; // 65535 for 16-bit
        int maxChannels = 1; // maximum number of channels;
        Length unitX, unitY; // the pixel scale unit
        String imageName;
        // read basic data
        input = new BufferedImageReader();
        xml = setMetadata(input, file);
        input = determineChannelReaderType(input, xml, file);
        imageNumber = input.getSeriesCount();
        returnableImages = new ArrayList<>();
        // iterate through images in the files
        for (int i = 0; i < imageNumber; i++) {
            // read basic data
            Tonga.log.debug("Names: {} | {}", xml.getImageName(i), file.getName());
            imageName = IO.fileName(xml.getImageName(i) == null ? file.getName() : xml.getImageName(i));
            input.setSeries(i);
            channelNumber = input.getEffectiveSizeC();
            splitChannelNumber = 0;
            timeNumber = input.getSizeT();
            sliceNumber = input.getSizeZ();
            unitX = xml.getPixelsPhysicalSizeX(i);
            unitY = xml.getPixelsPhysicalSizeY(i);
            bitNumber = input.getBitsPerPixel();
            maxPixelValue = (int) Math.pow(2, bitNumber) - 1;
            // create containing arrays
            Tonga.log.debug("-------------\n{}", imageName);
            Tonga.log.debug("Contains {} channels, {} z-layers, and {} timepoints", channelNumber, sliceNumber, timeNumber);
            if (unitX == null || unitY == null) {
                Tonga.log.debug("Does not have measurement metadata");
            } else {
                Tonga.log.debug("Has the scale {} {} / px (x), {} {} / px (y)", unitX.value(), unitX.unit().getSymbol(), unitY.value(), unitY.unit().getSymbol());
            }
            Tonga.log.debug("Has {} bits/pixel and the max value per pixel is {}", bitNumber, maxPixelValue);
            // iterate through timepoints
            for (int t = 0; t < timeNumber; t++) {
                TongaImage ti = new TongaImage(imageName + (timeNumber > 1 ? " T" + t : ""), getScaling(unitX, unitY));
                // iterate through channels
                for (int c = 0; c < channelNumber; c++) {
                    // read basic data
                    MappedImage image;
                    BufferedImage channel;
                    channelColor = xml.getChannelColor(i, c);
                    // handle z
                    if (sliceNumber > 1) {
                        // create a z-projection if necessary
                        // first load all the images of the channel as buffered images
                        BufferedImage[] slices = new BufferedImage[sliceNumber];
                        for (int z = 0; z < sliceNumber; z++) {
                            slices[z] = input.openImage(input.getIndex(z, c, t));
                        }
                        LOADER.appendProgress(1. / 4 / channelNumber / timeNumber / imageNumber);
                        // then convert buffered images into one with z-projection
                        channel = TongaRender.sliceProjection(slices, maxPixelValue);
                        LOADER.appendProgress(1. / 4 / channelNumber / timeNumber / imageNumber);
                    } else {
                        // no need for z-projection
                        channel = input.openImage(input.getIndex(0, c, t));
                        LOADER.appendProgress(1. / 2 / channelNumber / timeNumber / imageNumber);
                    }
                    // handle bits/formats and separate into channels if necessary
                    if (bitNumber == 8) {
                        // if 8bit then just force the output as ARGB
                        image = new MappedImage(channel);
                        ti.layerList.add(new TongaLayer(image, getChannelName(channelColor, ti)));
                        LOADER.appendProgress(1. / 2 / channelNumber / timeNumber / imageNumber);
                    } else {
                        // if not force the output as grayscale shorts
                        short[][] subchannel = new short[channel.getRaster().getNumBands()][]; //[channel][p]
                        // if the channel type is a 16-bit (A)RGB (=not supported)
                        if (channel.getRaster().getNumBands() > 1) {
                            // then separate into multiple grayscale 16bit channels
                            subchannel = TongaRender.separate16bit(channel);
                            splitChannelNumber = subchannel.length - 1;
                        } else {
                            // force 12-bits etc as grayscale shorts
                            subchannel[0] = TongaRender.make16bit(channel);
                        }
                        LOADER.appendProgress(1. / 4 / channelNumber / timeNumber / imageNumber);
                        // iterate any separated channels and add all as layers
                        for (short[] sub : subchannel) {
                            image = new MappedImage(sub, channel.getWidth(), channel.getHeight());
                            image.colour = getColour(channelColor);
                            if (Settings.settingAutoscaleType() == Settings.Autoscale.IMAGE) {
                                TongaRender.setDisplayRange(sub, image);
                            }
                            ti.layerList.add(new TongaLayer(image, getChannelName(channelColor, ti)));
                            LOADER.appendProgress(1. / 4 / subchannel.length / channelNumber / timeNumber / imageNumber);
                        }
                    }
                    maxChannels = Math.max(maxChannels, channelNumber + splitChannelNumber);
                }
                returnableImages.add(ti);
            }
        }
        TongaImage[] finalImages = returnableImages.toArray(new TongaImage[returnableImages.size()]);
        if (Settings.settingAutoscaleType() == Settings.Autoscale.FILE) {
            TongaRender.setDisplayRange(maxChannels, finalImages);
        }
        input.close(true);
        return finalImages;
    }

    private static OMEXMLMetadata setMetadata(BufferedImageReader input, File file) throws ServiceException, FormatException, IOException {
        OMEXMLMetadata xml;
        IMetadata metaData = xmlService.createOMEXMLMetadata();
        input.setMetadataStore(metaData);
        input.setId(file.getAbsolutePath());
        MetadataRetrieve retrieve = xmlService.asRetrieve(metaData);
        xml = xmlService.getOMEMetadata(retrieve);
        return xml;
    }

    private static BufferedImageReader determineChannelReaderType(BufferedImageReader input, OMEXMLMetadata xml, File file) {
        String name = xml.getImageName(0) == null ? file.getName() : xml.getImageName(0);
        input.setSeries(0);
        if (!name.equals(file.getName()) || input.getSeriesCount() > 1) {
            input = new BufferedImageReader(new ChannelSeparator(input));
            Tonga.log.debug("Channels will be separated");
        } else {
            input = new BufferedImageReader(new ChannelMerger(input));
            Tonga.log.debug("Channels will be merged");
        }
        return input;
    }

    private static String getColorName(Color c) {
        try {
            Tonga.log.debug("Color is " + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + " (" + c.getValue() + ")");
            javafx.scene.paint.Color col = new javafx.scene.paint.Color(1.0 / 255 * c.getRed(), 1.0 / 255 * c.getGreen(), 1.0 / 255 * c.getBlue(), 1);
            return cols.containsKey(col.toString()) ? cols.get(col.toString()) : col.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    private static String getChannelName(Color channel, TongaImage ti) {
        String colorName = getColorName(channel);
        if (colorName == null) {
            return ti.layerList.isEmpty() ? "Original" : "Layer";
        } else {
            return "Channel #" + colorName;
        }
    }

    @Deprecated
    private static int[] applyScaling(int[] rawChannel, int bitNumber, int slices) {
        int maxPixelRange; // 65536 for 16-bit
        if (Settings.settingAutoscaleType() == Settings.Autoscale.IMAGE) {
            maxPixelRange = (int) Math.pow(2, bitNumber) * slices;
            int[] histo = HISTO.getHistogramBWXbit(rawChannel, maxPixelRange);
            int[] posLowHigh = HISTO.getImportScaled(histo);
            Tonga.log.debug("{} is the lowest point, index is {}; {} is the highest point, index is {}", posLowHigh[0], histo[posLowHigh[1]], posLowHigh[1], histo[posLowHigh[0]]);
            Tonga.log.debug("To scale to 0-255 it should be substracted {} and scaled with {}", posLowHigh[0], (maxPixelRange / 2. / posLowHigh[1]));
            rawChannel = scaleBits(rawChannel, posLowHigh[0], maxPixelRange / 2. / posLowHigh[1]);
        }
        return rawChannel;
    }

    @Deprecated
    static int[] scaleBits(int[] i, int s, double f) {
        for (int p = 0; p < i.length; p++) {
            i[p] = (int) ((i[p] - s) * f);
        }
        return i;
    }

    @Deprecated
    static BufferedImage bitTobit8Color(int[] i, int slices, int bitdivider, int maxvalue, ome.xml.model.primitives.Color c, int w, int h) {
        int cc = c.getRed() << 16 | c.getGreen() << 8 | c.getBlue() | 255 << 24;
        Tonga.log.debug("There are {} slices and the divider is {}, maxvalue {}", slices, bitdivider, maxvalue);
        int div = slices * bitdivider;
        for (int p = 0; p < i.length; p++) {
            i[p] = RGB.argbColored(Math.min(maxvalue * slices, i[p]) / div, cc);
        }
        BufferedImage nb = AWTImageTools.blankImage(w, h, 4, FormatTools.INT8);
        nb.setRGB(0, 0, w, h, i, 0, w);
        return nb;
    }

    static boolean isStackImage(File file) throws ServiceException, FormatException, IOException, IllegalStateException {
        BufferedImageReader input = new BufferedImageReader();
        try {
            input.setId(file.getAbsolutePath());
        } catch (FormatException fe) {
            Tonga.log.warn("The file format is unsupported: {}", fe.getMessage());
        }
        input.setSeries(0);
        int imageNumber = input.getSeriesCount();
        int channelNumber = input.getEffectiveSizeC();
        boolean isStack = imageNumber > 1 || channelNumber > 1;
        Tonga.log.debug(isStack ? "This image seems to be a stack image" : "This image doesn't seem to be a stack image");
        return isStack;
    }

    private static int getColour(Color channelColor) {
        if (channelColor == null) {
            return 0xFFFFFFFF;
        } else {
            return channelColor.getRed() << 16 | channelColor.getGreen() << 8 | channelColor.getBlue() | 0xFF << 24;
        }
    }

    private static Length getScaling(Length unitX, Length unitY) {
        if (unitX == null || unitY == null) {
            return null;
        } else if (unitX.compareTo(unitY) == 0) {
            return unitX;
        } else {
            return null;
        }
    }
}
