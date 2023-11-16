package mainPackage.filters;

import mainPackage.ImageData;
import mainPackage.MappedImage;
import mainPackage.PanelCreator.ControlReference;
import mainPackage.Settings;

public abstract class FilterStack extends Filter {

    public FilterStack(String name, ControlReference[] params) {
        super(name, params);
    }

    protected abstract ImageData processor(ImageData[] layers);

    @Override
    protected ImageData[] getIDArray(Object o) {
        return new ImageData[]{(ImageData) o};
    }

    @Override
    protected int getIterations(int imageIndex, int[] selectedIndices) {
        return iterations(false);
    }

    @Override
    protected void setDimensions(Object imgs) {
        ImageData[] layers = (ImageData[]) imgs;
        width = (int) layers[0].width;
        height = (int) layers[0].height;
    }

    @Override
    protected Object handleImage(Object layerr) {
        return processor((ImageData[]) layerr);
    }

    @Override
    public ImageData runSingle(ImageData layer) {
        return (ImageData) runSingle(new ImageData[]{layer});
    }

    public ImageData runSingle(ImageData[] layer, Object... parameters) {
        param.setFilterParameters(parameterData, parameters);
        return runSingle(layer);
    }

    @Override
    public ImageData runSingle(ImageData[] layers) {
        ImageData finalImage = (ImageData) handle(layers);
        finalImage.name(Settings.settingBatchProcessing() ? layers[0].name + "-" + processName : processName);
        return finalImage;
    }
}
