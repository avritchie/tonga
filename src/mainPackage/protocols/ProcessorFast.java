package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.Tonga;
import mainPackage.morphology.ROISet;
import mainPackage.utils.IMG;

public abstract class ProcessorFast extends Processor {

    public ImageData[] inImage;
    public ImageData[] outImage;

    public ProcessorFast(int outputs, String[] output, int iters) {
        super(outputs, output, iters);
    }

    public ProcessorFast(int outputs, String output, int iters) {
        super(outputs, output, iters);
    }

    public ProcessorFast(int outputs, String[] output) {
        super(outputs, output);
    }

    public ProcessorFast(int outputs, String output) {
        super(outputs, output);
    }

    public ProcessorFast(String output, int iters) {
        super(output, iters);
    }

    public ProcessorFast(String output) {
        super(output);
    }

    @Override
    protected void setProcessorImages() {
        inImage = new ImageData[sourceLayer.length];
        for (int i = 0; i < sourceLayer.length; i++) {
            inImage[i] = new ImageData(sourceLayer[i]);
            inImage[i].setPixels();
            if (inImage[i].bits == 16) {
                inImage[i].set8BitPixels();
            }
        }
        outImage = new ImageData[outputImageNumber];
        for (int i = 0; i < outputImageNumber; i++) {
            outImage[i] = new ImageData(sourceWidth[0], sourceHeight[0]);
        }
    }

    protected void methodCore(int pos) {
        // no processing needed
    }

    @Override
    protected void pixelProcessor() {
        Tonga.iteration();
        for (int y = 0; y < sourceHeight[0]; y++) {
            for (int x = 0; x < sourceWidth[0]; x++) {
                methodCore(y * sourceWidth[0] + x);
            }
            Tonga.loader().appendProgress(sourceHeight[0]);
            if (Tonga.loader().getTask().isInterrupted()) {
                return;
            }
        }
    }

    @Override
    public ImageData[] getProcessedImages() {
        for (int i = 0; i < outputImageNumber; i++) {
            outImage[i].name = outputNames[i];
        }
        return outImage;
    }

    protected void outImageAs16Bits(int i) {
        outImage[i] = new ImageData(new short[sourceWidth[0] * sourceHeight[0]], sourceWidth[0], sourceHeight[0], null);
        outImage[i].setAttributes(inImage[i]);
    }

    public int pos(int x, int y) {
        return y * sourceWidth[0] + x;
    }

    protected void setOutputBy(ImageData[] id) {
        int max = Math.min(outImage.length, id.length);
        for (int i = 0; i < max; i++) {
            //outImage[i].pixels32 = id[i].pixels32;
            IMG.copyPixels(id[i].pixels32, outImage[i].pixels32);
        }
    }

    protected void setOutputBy(ROISet set) {
        setOutputBy(set, 0);
    }

    protected void setOutputBy(ROISet set, int i) {
        outImage[i].pixels32 = set.drawToArray();
    }

    protected void setOutputBy(ImageData id) {
        setOutputBy(id, 0);
    }

    protected void setOutputBy(ImageData id, int i) {
        //outImage[i].pixels32 = id.pixels32;
        IMG.copyPixels(id.pixels32, outImage[i].pixels32);
    }

    protected void setSampleOutputBy(ImageData id, int i) {
        if (Tonga.log.isDebugEnabled()) {
            try {
                IMG.copyPixels(id.pixels32, outImage[i].pixels32);
            } catch (NullPointerException ex) {
                if (id.bits == 16) {
                    id.set8BitPixels();
                    IMG.copyPixels(id.pixels32, outImage[i].pixels32);
                }
            }
        }
    }
}
