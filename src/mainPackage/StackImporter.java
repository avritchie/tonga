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
        int timeNumber; // time points in a image
        int sliceNumber; // z-slices in a channel
        int bitNumber; // 16 for 16-bit
        int maxPixelValue; // 65535 for 16-bit
        int maxChannels = 1; // maximum number of channels;
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
            timeNumber = input.getSizeT();
            sliceNumber = input.getSizeZ();
            bitNumber = input.getBitsPerPixel();
            maxPixelValue = (int) Math.pow(2, bitNumber) - 1;
            maxChannels = Math.max(maxChannels, channelNumber);
            // create containing arrays
            Tonga.log.debug("-------------\n{}", imageName);
            Tonga.log.debug("Contains {} channels, {} z-layers, and {} timepoints", channelNumber, sliceNumber, timeNumber);
            Tonga.log.debug("Has {} bits/pixel and the max value per pixel is {}", bitNumber, maxPixelValue);
            // iterate through timepoints
            for (int t = 0; t < timeNumber; t++) {
                TongaImage ti = new TongaImage();
                ti.imageName = imageName + (timeNumber > 1 ? " T" + t : "");
                // iterate through channels
                for (int c = 0; c < channelNumber; c++) {
                    // read basic data
                    CachedImage image;
                    channelColor = xml.getChannelColor(i, c);
                    // create containing arrays
                    if (bitNumber == 8 && sliceNumber == 1) {
                        image = new CachedImage(input.openImage(input.getIndex(0, c, t)));
                        LOADER.appendProgress(1. / channelNumber / timeNumber / imageNumber);
                    } else {
                        // convert 16-bit etc to 8-bit
                        short[] channel;
                        BufferedImage[] slices = new BufferedImage[sliceNumber];
                        // first load all the images of the channel as buffered images
                        // then convert buffered image to int/short array with z-projection
                        LOADER.appendProgress(1. / 2 / channelNumber / timeNumber / imageNumber);
                        for (int z = 0; z < sliceNumber; z++) {
                            slices[z] = input.openImage(input.getIndex(z, c, t));
                        }
                        LOADER.appendProgress(1. / 4 / channelNumber / timeNumber / imageNumber);
                        channel = TongaRender.sliceProjection(slices, maxPixelValue);
                        LOADER.appendProgress(1. / 4 / channelNumber / timeNumber / imageNumber);
                        image = new CachedImage(channel, slices[0].getWidth(), slices[0].getHeight());
                        image.colour = getColour(channelColor);
                        if (Settings.settingAutoscaleType() == Settings.Autoscale.IMAGE) {
                            TongaRender.setDisplayRange(channel, image);
                        }
                    }
                    ti.layerList.add(new TongaLayer(image, getChannelName(channelColor, ti)));
                }
                returnableImages.add(ti);
            }
        }
        TongaImage[] finalImages = returnableImages.toArray(new TongaImage[returnableImages.size()]);
        if (Settings.settingAutoscaleType() == Settings.Autoscale.CHANNEL) {
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

    @Deprecated
    private static TongaImage[] createImages(int images) {
        TongaImage[] imageArray = new TongaImage[images];
        Tonga.log.debug("Contains {} images", images);
        for (int i = 0; i < imageArray.length; i++) {
            imageArray[i] = new TongaImage();
        }
        return imageArray;
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
            int[] posLowHigh = HISTO.getMinMaxAdapt(histo, Settings.settingAutoscaleAggressive() ? 0.1 : 0);
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
            setMetadata(input, file);
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
}
