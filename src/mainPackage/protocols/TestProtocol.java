package mainPackage.protocols;

import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

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
                ROISet set = new ImageTracer(inImage[0], COL.BLACK).trace();
                setOutputBy(set.drawToImageData());
            }
        };
    }
}
