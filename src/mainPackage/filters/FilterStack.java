package mainPackage.filters;

import javafx.scene.image.Image;
import mainPackage.*;
import mainPackage.PanelCreator.ControlReference;

public abstract class FilterStack extends Filter {

    public FilterStack(String name, ControlReference[] params) {
        super(name, params);
    }

    @Override
    protected ImageData handleImage(Object layerr) {
        return processor((ImageData[]) layerr);
    }

    @Override
    protected void setDimensions(Object imgs) {
        ImageData[] layers = (ImageData[]) imgs;
        width = (int) layers[0].width;
        height = (int) layers[0].height;
    }

    @Override
    public ImageData runSingle(MappedImage[] layers) {
        return ((ImageData) super.runSingle(layers));
    }

    @Override
    public ImageData runSingle(ImageData layer) {
        return (ImageData) runSingle(new ImageData[]{layer});
    }

    @Override
    public ImageData runSingle(Image layer) {
        return (ImageData) runSingle(new ImageData[]{new ImageData(layer)});
    }

    @Override
    public ImageData runSingle(MappedImage layer) {
        return (ImageData) runSingle(new ImageData[]{new ImageData(layer)});
    }

    public ImageData runSingle(ImageData[] layer, Object... parameters) {
        param.setFilterParameters(parameterData, parameters);
        return runSingle(layer);
    }

    @Override
    public ImageData runSingle(ImageData[] layers) {
        ImageData finalImage = handle(layers);
        finalImage.name(Settings.settingBatchProcessing() ? layers[0].name + "-" + processName : processName);
        return finalImage;
    }

    @Override
    protected int calculateIterations(boolean all) {
        int firstImage = all ? 0 : Tonga.getImageIndex();
        int lastImage = all ? Tonga.getImageList().size() : firstImage + 1;
        int[] selectedIndexes = Tonga.getLayerIndexes();
        int counter = 0;
        for (int imageIndex = firstImage; imageIndex < lastImage; imageIndex++) {
            if (Tonga.layerStructureMatches(Tonga.getImageIndex(), imageIndex, selectedIndexes)) {
                counter += iterations(false);
            }
        }
        return counter;
    }

    protected abstract ImageData processor(ImageData[] layers);

    @Override
    protected ImageData[] getIDArray(Object o) {
        return new ImageData[]{(ImageData) o};
    }
}
