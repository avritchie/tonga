package mainPackage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import loci.common.DebugTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.ChannelMerger;
import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.formats.gui.BufferedImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.services.OMEXMLService;
import mainPackage.utils.HISTO;
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
        } catch (DependencyException ex) {
            Tonga.catchError(ex);
        }
    }

    public static TongaImage[] openFile(File file) throws IOException, FormatException, ServiceException {
        TongaImage[] returnableImages;
        BufferedImageReader input;
        OMEXMLMetadata xml;
        Color channelColor;
        int imageNumber; // images in a file
        int channelNumber; // channels in a image
        int sliceNumber; // z-slices in a channel
        int bitNumber; // 16 for 16-bit
        int maxPixelValue; // 65535 for 16-bit
        int maxChannels = 1; // maximum number of channels;
        // read basic data
        input = new BufferedImageReader();
        xml = setMetadata(input, file);
        input = determineChannelReaderType(input, xml, file);
        imageNumber = input.getSeriesCount();
        returnableImages = createImages(imageNumber);
        // iterate through images in the files
        for (int i = 0; i < imageNumber; i++) {
            // read basic data
            TongaImage ti = returnableImages[i];
            System.out.println("Names: " + xml.getImageName(i) + " | " + file.getName());
            ti.imageName = IO.fileName(xml.getImageName(i) == null ? file.getName() : xml.getImageName(i));
            input.setSeries(i);
            channelNumber = input.getEffectiveSizeC();
            sliceNumber = input.getSizeZ();
            bitNumber = input.getBitsPerPixel();
            maxPixelValue = (int) Math.pow(2, bitNumber) - 1;
            maxChannels = Math.max(maxChannels, channelNumber);
            // create containing arrays
            System.out.println("-------------\n" + ti.imageName);
            System.out.println("Contains " + channelNumber + " channels and " + sliceNumber + " z-layers");
            System.out.println("Has " + bitNumber + " bits/pixel and the max value per pixel is " + maxPixelValue);
            // iterate through channels
            for (int c = 0; c < channelNumber; c++) {
                // read basic data
                CachedImage image;
                channelColor = xml.getChannelColor(i, c);
                // create containing arrays
                if (bitNumber == 8 && sliceNumber == 1) {
                    image = new CachedImage(input.openImage(input.getIndex(0, c, 0)));
                } else {
                    // convert 16-bit etc to 8-bit
                    short[] channel;
                    BufferedImage[] slices = new BufferedImage[sliceNumber];
                    // first load all the images of the channel as buffered images
                    // then convert buffered image to int/short array with z-projection
                    LOADER.appendProgress(1. / 2 / channelNumber / imageNumber);
                    for (int z = 0; z < sliceNumber; z++) {
                        slices[z] = input.openImage(input.getIndex(z, c, 0));
                    }
                    LOADER.appendProgress(1. / 4 / channelNumber / imageNumber);
                    channel = TongaRender.sliceProjection(slices, maxPixelValue);
                    LOADER.appendProgress(1. / 4 / channelNumber / imageNumber);
                    image = new CachedImage(channel, slices[0].getWidth(), slices[0].getHeight());
                    image.colour = getColour(channelColor);
                    if (Settings.settingAutoscaleType() == Settings.Autoscale.IMAGE) {
                        TongaRender.setDisplayRange(channel, image);
                    }
                }
                ti.layerList.add(new TongaLayer(image, getChannelName(channelColor, ti)));
            }
        }
        if (Settings.settingAutoscaleType() == Settings.Autoscale.CHANNEL) {
            TongaRender.setDisplayRange(maxChannels, returnableImages);
        }
        input.close(true);
        return returnableImages;
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

    private static TongaImage[] createImages(int images) {
        TongaImage[] imageArray = new TongaImage[images];
        System.out.println("Contains " + images + " images");
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
            System.out.println("Channels will be separated");
        } else {
            input = new BufferedImageReader(new ChannelMerger(input));
            System.out.println("Channels will be merged");
        }
        return input;
    }

    private static String getColorName(Color c) {
        try {
            System.out.println("Color is " + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + " (" + c.getValue() + ")");
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

    private static int[] applyScaling(int[] rawChannel, int bitNumber, int slices) {
        int maxPixelRange; // 65536 for 16-bit
        if (Settings.settingAutoscaleType() == Settings.Autoscale.IMAGE) {
            maxPixelRange = (int) Math.pow(2, bitNumber) * slices;
            int[] histo = HISTO.getHistogramBWXbit(rawChannel, maxPixelRange);
            int[] posLowHigh = HISTO.getMinMaxAdapt(histo, Settings.settingAutoscaleAggressive() ? 0.1 : 0);
            System.out.println(Arrays.toString(histo));
            System.out.println(histo[posLowHigh[0]] + " is the lowest point, index is " + posLowHigh[0] + "; " + histo[posLowHigh[1]] + " is the highest point, index is " + posLowHigh[1]);
            System.out.println("To scale to 0-255 it should be substracted " + posLowHigh[0] + " and scaled with " + (maxPixelRange / 2. / posLowHigh[1]));
            rawChannel = TongaRender.scaleBits(rawChannel, posLowHigh[0], maxPixelRange / 2. / posLowHigh[1]);
        }
        return rawChannel;
    }

    static boolean isStackImage(File file) throws ServiceException, FormatException, IOException {
        BufferedImageReader input = new BufferedImageReader();
        setMetadata(input, file);
        input.setSeries(0);
        int imageNumber = input.getSeriesCount();
        int channelNumber = input.getEffectiveSizeC();
        boolean isStack = imageNumber > 1 || channelNumber > 1;
        //System.out.println(isStack ? "This image seems to be a stack image" : "This image doesn't seem to be a stack image");
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
