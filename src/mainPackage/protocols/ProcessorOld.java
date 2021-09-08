package mainPackage.protocols;

import java.util.Arrays;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import mainPackage.ImageData;
import mainPackage.Tonga;

public abstract class ProcessorOld extends Processor {

    public PixelReader[] inReader;
    public PixelWriter[] outWriter;
    public WritableImage[] outImage;

    public ProcessorOld(int outputs, String[] output, int iters) {
        super(outputs, output, iters);
    }

    public ProcessorOld(int outputs, String output, int iters) {
        super(outputs, output, iters);
    }

    public ProcessorOld(int outputs, String[] output) {
        super(outputs, output);
    }

    public ProcessorOld(int outputs, String output) {
        super(outputs, output);
    }

    public ProcessorOld(String output, int iters) {
        super(output, iters);
    }

    public ProcessorOld(String output) {
        super(output);
    }

    @Override
    protected void setProcessorImages() {
        inReader = Arrays.stream(sourceLayer).map((a) -> a.layerImage.getFXImage().getPixelReader()).toArray(PixelReader[]::new);
        outImage = getWriters();
        outWriter = Arrays.stream(outImage).map((a) -> a.getPixelWriter()).toArray(PixelWriter[]::new);
    }

    protected WritableImage[] getWriters() {
        WritableImage[] images = new WritableImage[outputImageNumber];
        for (int i = 0; i < outputImageNumber; i++) {
            images[i] = new WritableImage(sourceWidth[0], sourceHeight[0]);
        }
        return images;
    }

    protected void methodCore(int x, int y) {
        //no core needed
    }

    @Override
    protected void pixelProcessor() {
        // default processor
        Tonga.iteration();
        for (int y = 0; y < sourceHeight[0]; y++) {
            for (int x = 0; x < sourceWidth[0]; x++) {
                methodCore(x, y);
            }
            Tonga.loader().appendProgress(sourceHeight[0]);
            if (Tonga.loader().getTask().isInterrupted()) {
                return;
            }
        }
    }

    @Override
    public ImageData[] getProcessedImages() {
        ImageData[] layers = new ImageData[outImage.length];
        for (int i = 0; i < outImage.length; i++) {
            layers[i] = new ImageData(outImage[i], outputNames[i]);
        }
        return layers;
    }
}
