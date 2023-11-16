package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import mainPackage.filters.Filters;

public class TestProtocol extends Protocol {

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Input layer")};
    }

    @Override
    protected Processor getProcessor() {

        return new ProcessorFast(2, "Test", 1) {

            @Override
            protected void pixelProcessor() {
                ImageData layerCorrect = initTempData();
                Filters.gamma().runTo(inImage[0], layerCorrect, 0.5);
                setOutputBy(layerCorrect, 1);
                Filters.autoscale().runTo(layerCorrect);
                setOutputBy(layerCorrect);
            }
        };
    }
}
