package mainPackage.filters;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import mainPackage.Tonga;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public abstract class FilterSet extends FilterFast {

    ROISet inSet;
    ROISet outSet;
    boolean setHandling;
    OutputType outType;

    public FilterSet(String name, ControlReference[] params, OutputType out) {
        super(name, params);
        outType = out;
    }

    public FilterSet(String name, ControlReference[] params, OutputType out, int iter) {
        super(name, params, iter);
        outType = out;
    }

    public enum OutputType {
        PLAINSET,
        EFFECTSET,
        OUTDATA
    }

    protected abstract void processorSet();

    @Override
    protected void processor16() {
        throw new UnsupportedOperationException("Set filters only function in 8-bits");
    }

    @Override
    protected void processor() {
        inSet = new ImageTracer(inData, param.colorARGB[0]).trace();
        processorSet();
    }

    @Override
    protected void setDimensions(Object source) {
        if (source instanceof ROISet) {
            ROISet rs = (ROISet) source;
            width = rs.width;
            height = rs.height;
        } else {
            if (source instanceof Object[]) {
                super.setDimensions(((Object[]) source)[1]);
            } else {
                super.setDimensions(source);
            }
        }
    }

    @Override
    protected Object handleImage(Object source) {
        inSet = null;
        outSet = null;
        // for filter sets who are outputting set data, only in FiltersSet class
        if (setHandling) {
            if (source instanceof ROISet) {
                inSet = (ROISet) source;
                processorSet();
            } else if (source instanceof Object[]) {
                setInput(((Object[]) source)[1]);
                inSet = (ROISet) ((Object[]) source)[0];
                processorSet();
                clean();
            } else if (source instanceof ImageData) {
                setInput(source);
                processor();
                clean();
            } else {
                Tonga.catchError("Requested set handling for a set filter with incorrect source data");
            }
            if (outSet == null) {
                Tonga.catchError("Executed set handling for a set filter without set output");
            }
            return outSet;
        } else {
            // for filter sets not outputting set data, in FiltersPass class
            if (source instanceof ROISet) {
                setOutput(false);
                inSet = (ROISet) source;
                processorSet();
            }
            if (source instanceof Object[]) {
                inSet = (ROISet) ((Object[]) source)[0];
                setInput(((Object[]) source)[1]);
                setOutput(false);
                processorSet();
            } else if (source instanceof ImageData) {
                setInput(source);
                setOutput(false);
                processor();
            }
            switch (outType) {
                case PLAINSET:
                    setOutputBy(inSet);
                    break;
                case EFFECTSET:
                    setOutputBy(inSet.drawToImageData());
                    break;
                case OUTDATA:
                    //is always a default option
                    break;
            }
            ImageData out = outData;
            clean();
            return out;
        }
    }

    public ROISet runSet(ImageData set, Object... parameters) {
        setHandling = true;
        param.setFilterParameters(parameterData, parameters);
        ROISet rs = (ROISet) handle(set);
        setHandling = false;
        return rs;
    }

    public ROISet runSet(ROISet set, Object... parameters) {
        setHandling = true;
        param.setFilterParameters(parameterData, parameters);
        ROISet rs = (ROISet) handle(set);
        setHandling = false;
        return rs;
    }

    public ROISet runSet(ROISet set, ImageData id, Object... parameters) {
        setHandling = true;
        param.setFilterParameters(parameterData, parameters);
        ROISet rs = (ROISet) handle(new Object[]{set, id});
        setHandling = false;
        return rs;
    }

    public ImageData runSingle(ROISet set, Object... parameters) {
        param.setFilterParameters(parameterData, parameters);
        ImageData rid = (ImageData) handle(set);
        return rid;
    }

    public ImageData runSingle(ROISet set, ImageData id, Object... parameters) {
        param.setFilterParameters(parameterData, parameters);
        ImageData rid = (ImageData) handle(new Object[]{set, id});
        return rid;
    }

    protected void setOutsetBy(ROISet set) {
        outSet = set;
    }

    protected void setOutputBy(ROISet id) {
        id.drawTo(outData, true);
    }
}
