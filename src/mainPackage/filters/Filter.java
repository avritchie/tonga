package mainPackage.filters;

import java.io.File;
import java.io.IOException;
import mainPackage.TongaLayer;
import javax.imageio.ImageIO;
import mainPackage.MappedImage;
import mainPackage.IO;
import mainPackage.ImageData;
import mainPackage.PanelCreator;
import static mainPackage.PanelCreator.ControlType.RANGE;
import static mainPackage.PanelCreator.ControlType.SLIDER;
import static mainPackage.PanelCreator.ControlType.SPINNER;
import mainPackage.PanelCreator.ControlReference;
import mainPackage.PanelParams;
import mainPackage.Settings;
import mainPackage.Tonga;
import static mainPackage.PanelCreator.ControlType.COLOUR;
import mainPackage.PanelUtils;

public abstract class Filter {

    public static ControlReference[] noParams = new ControlReference[0];
    public static ControlReference[] threshold = new ControlReference[]{
        new ControlReference(SLIDER, "Threshold")};
    public static ControlReference[] limits = new ControlReference[]{
        new ControlReference(RANGE, new Integer[]{0, 255}, "Minimum%Maximum")};
    public static ControlReference[] radius = new ControlReference[]{
        new ControlReference(SPINNER, "Radius (px)", 2)};
    public static ControlReference[] iters = new ControlReference[]{
        new ControlReference(SPINNER, "Repeat how many times", 2)};
    public static ControlReference[] bgcol = new ControlReference[]{
        new ControlReference(COLOUR, "Background colour", -2)};

    public String processName;
    public ControlReference[] parameterData;
    public PanelCreator panelCreator;
    public PanelParams param;
    public int iterations32, iterations16;
    public int width, height;
    public boolean conditional; //execute only on given pixels
    public int conditionalColor; //only execute pixels which are not of this color
    public int[] conditionalPixels; //the given pixels, must be conditionalcolor/other
    public ImageData destination; //execute directly to the destination array

    public Filter(String name, ControlReference[] params) {
        this(name, params, 1, 1);
    }

    public Filter(String name, ControlReference[] params, int iterations32, int iterations16) {
        this.iterations32 = iterations32;
        this.iterations16 = iterations16;
        processName = name;
        parameterData = params;
        param = new PanelParams(parameterData);
    }

    public Filter(String name, ControlReference[] params, int iterations) {
        this(name, params, iterations, iterations);
    }

    public final String getName() {
        return processName;
    }

    public final void loadComponents() {
        panelCreator = new PanelCreator(parameterData);
        PanelUtils.updateComponents(panelCreator);
    }

    public final void loadParams() {
        param.getFilterParameters(panelCreator);
    }

    protected int iterations(boolean bits) {
        //override to alter
        return bits ? iterations16 : iterations32;
    }

    abstract protected ImageData[] getIDArray(Object img);

    abstract protected int getIterations(int imageIndex, int[] selectedIndices);

    abstract protected void setDimensions(Object img);

    abstract protected Object handleImage(Object img);

    public ImageData[][] runSingle() {
        int selectedImage = Tonga.getImageIndex();
        int[] selectedIndexes = Tonga.imageAsSelectedLayerArray(Tonga.getImage());
        Tonga.loader().setIterations(calculateIterations(false));
        return new ImageData[][]{runSingle(selectedImage, selectedIndexes)};
    }

    public ImageData[][] runAll() {
        int selectedImage = Tonga.getImageIndex();
        int[] selectedIndexes = Tonga.imageAsSelectedLayerArray(Tonga.getImage());
        int totalImages = Tonga.getImageList().size();
        ImageData[][] data = new ImageData[totalImages][selectedIndexes.length];
        Tonga.loader().setIterations(calculateIterations(true));
        //List<Integer> procImgs = new ArrayList<>();
        for (int imageIndex = 0; imageIndex < totalImages; imageIndex++) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            if (Tonga.layerStructureMatches(selectedImage, imageIndex, selectedIndexes)) {
                data[imageIndex] = runSingle(imageIndex, selectedIndexes);
                //procImgs.add(imageIndex);
            } else {
                data[imageIndex] = null;
            }
        }
        /*int threads = Tonga.getCpuThreads() / 2;
        Thread[] coreThreads = new Thread[threads];
        List<Integer>[] ctInd = new List[threads];
        for (int i = 0; i < procImgs.size(); i++) {
            int ctI = (int) ((threads / (double) procImgs.size()) * i);
            if (ctInd[ctI] == null) {
                ctInd[ctI] = new ArrayList<>();
            }
            ctInd[ctI].add(procImgs.get(i));
        }
        for (int i = 0; i < threads; i++) {
            coreThreads[i] = new Thread((new Runnable() {
                int index;

                public Runnable setTarget(int ind) {
                    this.index = ind;
                    return this;
                }

                @Override
                public void run() {
                    ctInd[index].forEach(imageIndex -> {
                        data[imageIndex] = runSingle(imageIndex);
                    });
                }
            }).setTarget(i));
            coreThreads[i].setName("Core" + i + processName);
            coreThreads[i].start();
        }
        for (int i = 0; i < threads; i++) {
            try {
                coreThreads[i].join();
            } catch (InterruptedException ex) {
                Tonga.catchError(ex, "Waiter thread interrupted.");
            }
        }*/
        return data;
    }

    protected Object runSingle(ImageData[] layers) {
        ImageData[] tongas = new ImageData[layers.length];
        for (int i = 0; i < layers.length; i++) {
            ImageData finalImage = (ImageData) handle(layers[i]);
            finalImage.name(Settings.settingBatchProcessing() ? layers[i].name + "-" + processName : processName);
            tongas[i] = finalImage;
        }
        return tongas;
    }

    public ImageData runSingle(ImageData layer) {
        if (layer == null) {
            Tonga.catchError(new NullPointerException(), "Filter was given an empty layer to work with.");
        }
        return ((ImageData[]) runSingle(new ImageData[]{layer}))[0];
    }

    public final ImageData[] runSingle(int image, int[] indexes) {
        param.setImageFilterParameters(param, Tonga.getImage(image));
        if (Settings.settingBatchProcessing()) {
            String[] pointers = Tonga.layersAsPointerArray(Tonga.imageLayersFromIndexList(image, indexes));
            ImageData[] imgs = new ImageData[pointers.length];
            for (int i = 0; i < pointers.length; i++) {
                try {
                    imgs[i] = new ImageData(IO.getImageFromFile(pointers[i]));
                    imgs[i].name(pointers[i]);
                } catch (Exception ex) {
                    Tonga.catchError(ex, "Unable to read the file " + pointers[i]);
                }
            }
            return getIDArray(runSingle(imgs));
        }
        return getIDArray(runSingle(Tonga.layersAsImageDataArray(Tonga.imageLayersFromIndexList(image, indexes))));
    }

    public ImageData runSingle(TongaLayer layer, Object... parameters) {
        return runSingle(new ImageData(layer), parameters);
    }

    public ImageData runSingle(ImageData layer, Object... parameters) {
        param.setFilterParameters(parameterData, parameters);
        return runSingle(layer);
    }

    public void runTo(ImageData source, Object... parameters) {
        runTo(source, source, parameters);
    }

    public void runTo(ImageData source, ImageData dest, Object... parameters) {
        param.setFilterParameters(parameterData, parameters);
        if (dest == null) {
            Tonga.catchError(new NullPointerException(), "Filter was given an empty destination to work to.");
        }
        destination = dest;
        handle(source);
    }

    //run the filter only on pixels where condlayer is not black; both must be the same size (!)
    public ImageData runConditional(ImageData layer, ImageData condlayer, int condCol, Object... parameters) {
        conditional = true;
        conditionalColor = condCol;
        conditionalPixels = condlayer.pixels32;
        ImageData output = runSingle(layer, parameters);
        conditional = false;
        conditionalColor = -1;
        conditionalPixels = null;
        return output;
    }

    protected final Object handle(Object img) {
        setDimensions(img);
        double cProg = Tonga.loader().getProgress();
        double iProg = Tonga.iterations();
        Object handled = handleImage(img);
        if (cProg == Tonga.loader().getProgress()) {
            if (iProg == Tonga.iterations()) {
                Tonga.iteration();
            }
            Tonga.loader().appendProgress(1.);
        }
        return handled;
    }

    public final static void publish(ImageData[][] images, String name) {
        Tonga.loader().maxProgress();
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        for (int image = 0; image < images.length; image++) {
            if (images[image] != null) {
                for (ImageData i : images[image]) {
                    if (i != null) {
                        int index = images.length == 1 ? Tonga.getImageIndex() : image;
                        if (Settings.settingBatchProcessing()) {
                            String file = i.name.replaceAll("/", "-") + ".png";
                            try {
                                ImageIO.write(i.toStreamedImage(), "png", new File(file));
                            } catch (IOException ex) {
                                Tonga.catchError(ex, "Unable to write the file " + file);
                            }
                            Tonga.injectNewLayer(new TongaLayer(file, name), index);
                        } else {
                            Tonga.injectNewLayer(i.toLayer(), index);
                        }
                    }
                }
            }
        }
        Tonga.getImage().stack = false;
        Tonga.publishLayerList();
    }

    protected final int calculateIterations(boolean all) {
        int firstImage = all ? 0 : Tonga.getImageIndex();
        int lastImage = all ? Tonga.getImageList().size() : firstImage + 1;
        int[] selectedIndices = Tonga.getLayerIndexes();
        int counter = 0;
        for (int imageIndex = firstImage; imageIndex < lastImage; imageIndex++) {
            if (Tonga.layerStructureMatches(Tonga.getImageIndex(), imageIndex, selectedIndices)) {
                counter += getIterations(imageIndex, selectedIndices);
            }
        }
        return counter;
    }
}
