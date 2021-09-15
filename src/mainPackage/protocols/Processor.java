package mainPackage.protocols;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mainPackage.IO;
import mainPackage.ImageData;
import mainPackage.Settings;
import mainPackage.Tonga;
import mainPackage.TongaImage;
import mainPackage.TongaLayer;
import mainPackage.counters.TableData;

public abstract class Processor {

    protected String[] outputNames;
    public int[] sourceWidth;
    public int[] sourceHeight;
    public TongaImage sourceImage;
    public TongaLayer[] sourceLayer;
    protected int iterations;
    protected int outputImageNumber;
    protected List<TableData> datas;
    protected TableData data;

    public Processor(int outputs, String[] output, int iters) {
        outputNames = output;
        outputImageNumber = outputs;
        iterations = iters;
        datas = new ArrayList<>();
        data = null;
    }

    public Processor(int outputs, String output, int iters) {
        this(outputs, new String[]{output}, iters);
    }

    public Processor(int outputs, String[] output) {
        this(outputs, output, 10);
        System.out.println("Iteration parameter missing: " + output[0] + ":" + getClass().getName());
    }

    public Processor(int outputs, String output) {
        this(outputs, new String[]{output});
    }

    public Processor(String output, int iters) {
        this(1, new String[]{output}, iters);
    }

    public Processor(String output) {
        this(1, output);
    }

    protected void initTableData(String[] titles) {
        data = new TableData(titles);
    }

    protected ImageData[] internalProcessing(TongaImage imageSource, TongaLayer[] layerAccess) {
        sourceImage = imageSource;
        sourceLayer = layerAccess;
        processorInit();
        methodInit();
        pixelProcessor();
        methodFinal();
        processorFinalize();
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return getProcessedImages();
    }

    private void processorInit() {
        if (Settings.settingBatchProcessing()) {
            Arrays.stream(sourceLayer).forEach(i -> {
                try {
                    if (i.layerImage == null) {
                        System.out.println("Loading from " + i.path);
                        i.layerImage = IO.getImageFromFile(i.path);
                    }
                } catch (Exception ex) {
                    Tonga.catchError(ex, "Unable to read the file " + i.path);
                }
            });
        }
        sourceWidth = Arrays.stream(sourceLayer).mapToInt((a) -> (int) a.layerImage.getWidth()).toArray();
        sourceHeight = Arrays.stream(sourceLayer).mapToInt((a) -> (int) a.layerImage.getHeight()).toArray();
        if (outputNames.length < outputImageNumber) {
            String outputName = outputNames[0];
            outputNames = new String[outputImageNumber];
            for (int i = 0; i < outputNames.length; i++) {
                outputNames[i] = outputName;
            }
        }
        setProcessorImages();
    }

    private void processorFinalize() {
        if (Settings.settingBatchProcessing()) {
            Arrays.stream(sourceLayer).forEach(p -> {
                p.layerImage = null;
            });
        }
    }

    protected void methodInit() {
        // no processing needed
    }

    protected void methodFinal() {
        // no processing needed
    }

    abstract void pixelProcessor();

    abstract void setProcessorImages();

    abstract ImageData[] getProcessedImages();

    protected void setDatasBy(Protocol prt) {
        datas.add(prt.results);
    }

    protected void setDatasBy(TableData prt) {
        datas.add(prt);
    }

    protected void newResultRow(Object... cols) {
        Object[] newRow = data.newRow(sourceImage.imageName);
        for (int i = 0; i < cols.length; i++) {
            Object cc = cols[i];
            newRow[i + 1] = cc.getClass().equals(int.class) ? (Integer) cc
                    : cc.getClass().equals(double.class) ? (Double) cc
                    : cc.getClass().equals(String.class) ? (String) cc : cc;
        }
    }
}
