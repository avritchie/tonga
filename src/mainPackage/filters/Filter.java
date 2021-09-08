package mainPackage.filters;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javafx.scene.image.Image;
import mainPackage.TongaLayer;
import mainPackage.TongaImage;
import javax.imageio.ImageIO;
import mainPackage.CachedImage;
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

    public String getName() {
        return processName;
    }

    public void loadComponents() {
        panelCreator = new PanelCreator(parameterData);
    }

    public ImageData[][] runSingle() {
        Tonga.loader().setIterations(calculateIterations(false));
        return new ImageData[][]{runSingle(Tonga.getImageIndex())};
    }

    public ImageData[][] runAll() {
        int[] selectedIndexes = Tonga.getLayerIndexes();
        int totalImages = Tonga.getImageList().size();
        ImageData[][] data = new ImageData[totalImages][selectedIndexes.length];
        Tonga.loader().setIterations(calculateIterations(true));
        //List<Integer> procImgs = new ArrayList<>();
        for (int imageIndex = 0; imageIndex < totalImages; imageIndex++) {
            if (Thread.interrupted()) {
                return null;
            }
            if (Tonga.layerStructureMatches(Tonga.getImageIndex(), imageIndex, selectedIndexes)
                    && selectedIndexes[selectedIndexes.length - 1] < Tonga.getLayerList(imageIndex).size()) {
                data[imageIndex] = runSingle(imageIndex);
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
                System.out.println("Terrible fate.");
            }
        }*/
        return data;
    }

    abstract protected ImageData[] getIDArray(Object img
    );

    protected Object runSingle(ImageData[] layers) {
        ImageData[] tongas = new ImageData[layers.length];
        for (int i = 0; i < layers.length; i++) {
            ImageData finalImage = handle(layers[i]);
            finalImage.name(Settings.settingBatchProcessing() ? layers[i].name + "-" + processName : processName);
            tongas[i] = finalImage;
        }
        return tongas;
    }

    public Object runSingle(CachedImage[] layers) {
        return runSingle(ImageData.convertToImageData(layers));
    }

    public Object runSingle(Image[] layers) {
        return runSingle(ImageData.convertToImageData(layers));
    }

    public ImageData runSingle(ImageData layer) {
        if (layer == null) {
            Tonga.catchError(new NullPointerException(), "Filter was given an empty layer to work with.");
        }
        return ((ImageData[]) runSingle(new ImageData[]{layer}))[0];
    }

    public ImageData runSingle(Image layer) {
        return ((ImageData[]) runSingle(new ImageData[]{new ImageData(layer)}))[0];
    }

    public ImageData runSingle(CachedImage layer) {
        return ((ImageData[]) runSingle(new ImageData[]{new ImageData(layer)}))[0];
    }

    public ImageData runSingle(TongaLayer layer) {
        return runSingle(layer.layerImage);
    }

    public ImageData[] runSingle(int image) {
        if (Settings.settingBatchProcessing()) {
            String[] pointers = selectedImagesAsPointerArray(image);
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
        return getIDArray(runSingle(selectedImagesAsImageDataArray(image)));
    }

    public ImageData runSingle(TongaLayer layer, Object... parameters) {
        return runSingle(new ImageData(layer), parameters);
    }

    public ImageData runSingle(ImageData layer, Object... parameters) {
        param.setFilterParameters(parameterData, parameters);
        return runSingle(layer);
    }

    private ImageData[] selectedImagesAsImageDataArray(int image) {
        if (Tonga.getImage(image).stack) {
            return Arrays.stream(Tonga.getLayerIndexes()).mapToObj(i -> Tonga.getLayerList(image).get(i))
                    .filter(tl -> !tl.isGhost).map(tl -> new ImageData(tl)).toArray(ImageData[]::new);
        } else {
            return Arrays.stream(Tonga.getLayerIndexes()).mapToObj(i -> new ImageData(Tonga.getLayerList(image).get(i).layerImage)).toArray(ImageData[]::new);
        }
    }

    private String[] selectedImagesAsPointerArray(int image) {
        return Arrays.stream(Tonga.getLayerIndexes()).mapToObj(i -> Tonga.getLayerList(image).get(i).path).toArray(String[]::new);
    }

    protected ImageData handle(Object img) {
        setDimensions(img);
        double cProg = Tonga.loader().getProgress();
        ImageData rid = handleImage(img);
        if (cProg == Tonga.loader().getProgress()) {
            Tonga.loader().appendProgress(1.);
        }
        return rid;
    }

    abstract protected void setDimensions(Object img);

    abstract protected ImageData handleImage(Object img);

    public static void publish(ImageData[][] images, String name) {
        Tonga.loader().maxProgress();
        if (Thread.interrupted()) {
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
                                ImageIO.write(i.toImage(), "png", new File(file));
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

    protected int calculateIterations(boolean all) {
        int firstImage = all ? 0 : Tonga.getImageIndex();
        int lastImage = all ? Tonga.getImageList().size() : firstImage + 1;
        int[] selectedIndexes = Tonga.getLayerIndexes();
        int counter = 0;
        for (int imageIndex = firstImage; imageIndex < lastImage; imageIndex++) {
            if (Tonga.layerStructureMatches(Tonga.getImageIndex(), imageIndex, selectedIndexes)) {
                List<TongaLayer> ll = Tonga.getLayerList(imageIndex);
                for (int i = 0; i < selectedIndexes.length; i++) {
                    if (selectedIndexes[i] < ll.size()) {
                        counter += iterations(ll.get(selectedIndexes[i]).layerImage == null ? false
                                : ll.get(selectedIndexes[i]).layerImage.bits == 16);
                    }
                }
            }
        }
        return counter;
    }

    protected int iterations(boolean bits) {
        //override to alter
        return bits ? iterations16 : iterations32;
    }

    public void getParams() {
        param.getFilterParameters(panelCreator);
    }
}
