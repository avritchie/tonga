package mainPackage.filters;

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

    @Override
    protected ImageData handleImage(Object imgg) {
        inData = (ImageData) imgg;
        inData.setPixels();
        in32 = inData.pixels32;
        in16 = inData.pixels16;
        if (inData.bits == 16) {
            try {
                outData = new ImageData(new short[inData.totalPixels()], width, height);
                outData.setAttributes(inData);
                out32 = outData.pixels32;
                out16 = outData.pixels16;
                processor16();
                return outData;
            } catch (UnsupportedOperationException uoe) {
                Tonga.log.info("The method {} does not support 16-bit images.", this.getName());
                inData.set8BitPixels();
                in32 = inData.pixels32;
            }
        }
        outData = new ImageData(new int[inData.totalPixels()], width, height);
        out32 = outData.pixels32;
        out16 = outData.pixels16;
        processor();
        return outData;
    }

    @Override
    protected void setDimensions(Object imgs) {
        ImageData imgg = (ImageData) imgs;
        width = imgg.width;
        height = imgg.height;
    }

    protected abstract void processor();

    protected abstract void processor16();

    protected void set16BitScaleRange(int from, int to) {
        double r = (outData.max - outData.min) / 255.;
        outData.max = (int) (outData.min + from * r);
        outData.min = (int) (outData.min + to * r);
        IMG.copyPixels(in16, out16);
    }

    @Override
    protected ImageData[] getIDArray(Object o) {
        return ((ImageData[]) o);
    }

    protected void setOutputBy(ImageData id) {
        outData.pixels32 = id.pixels32;
    }

    protected void setOutputBy(int[] id) {
        outData.pixels32 = id;
    }
}
