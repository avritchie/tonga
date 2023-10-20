package mainPackage.filters;

import java.util.List;
import mainPackage.utils.IMG;
import mainPackage.*;
import mainPackage.PanelCreator.ControlReference;

public abstract class FilterFast extends Filter {

    ImageData inData;
    ImageData outData;
    int[] in32;
    int[] out32;
    short[] in16;
    short[] out16;

    public FilterFast(String name, ControlReference[] params) {
        super(name, params);
    }

    public FilterFast(String name, ControlReference[] params, int iter) {
        super(name, params, iter);
    }

    public FilterFast(String name, ControlReference[] params, int iter32, int iter16) {
        super(name, params, iter32, iter16);
    }

    protected abstract void processor();

    protected abstract void processor16();

    @Override
    protected ImageData[] getIDArray(Object o) {
        return ((ImageData[]) o);
    }

    @Override
    protected int getIterations(int imageIndex, int[] selectedIndices) {
        int counter = 0;
        List<TongaLayer> ll = Tonga.getLayerList(imageIndex);
        for (int i = 0; i < selectedIndices.length; i++) {
            if (selectedIndices[i] < ll.size()) {
                counter += iterations(ll.get(selectedIndices[i]).layerImage == null ? false
                        : ll.get(selectedIndices[i]).layerImage.bits == 16);
            }
        }
        return counter;
    }

    @Override
    protected void setDimensions(Object imgs) {
        ImageData imgg = (ImageData) imgs;
        width = imgg.width;
        height = imgg.height;
    }

    @Override
    protected Object handleImage(Object imgg) {
        setInput(imgg);
        boolean shortMode = inData.bits == 16;
        if (shortMode) {
            try {
                setOutput(true);
                processor16();
            } catch (UnsupportedOperationException uoe) {
                Tonga.log.info("The method {} does not support 16-bit images.", this.getName());
                inData.set8BitPixels();
                in32 = inData.pixels32;
                shortMode = false;
            }
        }
        if (!shortMode) {
            setOutput(false);
            processor();
        }
        if (inData == outData) {
            //Tonga.log.debug("Filter destination is the same as its source for {}", this.processName);
        }
        ImageData out = outData;
        clean();
        return out;
    }

    protected void setInput(Object imgg) {
        inData = (ImageData) imgg;
        inData.setPixels();
        in32 = inData.pixels32;
        in16 = inData.pixels16;
    }

    protected void setOutput(boolean bits) {
        //use the destination array if available
        if (destination != null) {
            outData = destination;
            //if doesnt fit, make it fit
            if (destination.bits != (bits ? 16 : 8)) {
                if (bits) {
                    destination.make16(inData);
                } else {
                    destination.make8();
                }
            }
        } else {
            //if not, allocate a destination array
            if (bits) {
                outData = new ImageData(new short[width * height], width, height);
                outData.setAttributes(inData);
            } else {
                outData = new ImageData(new int[width * height], width, height);
            }
        }
        out32 = outData.pixels32;
        out16 = outData.pixels16;
    }

    protected void clean() {
        inData = null;
        outData = null;
        in32 = null;
        in16 = null;
        out32 = null;
        out16 = null;
        destination = null;
    }

    protected void set16BitScaleRange(int from, int to, boolean bit8) {
        if (bit8) {
            double r = (outData.max - outData.min) / 255.;
            from *= r;
            to *= r;
        }
        outData.max = (int) (outData.min + from);
        outData.min = (int) (outData.min + to);
        IMG.copyPixels(in16, out16);
    }

    protected void setOutputBy(ImageData id) {
        outData.pixels32 = id.pixels32;
    }

    protected void setOutputBy(int[] id) {
        outData.pixels32 = id;
    }
}
