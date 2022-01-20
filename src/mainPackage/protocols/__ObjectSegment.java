package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Settings;
import mainPackage.Tonga;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

public class __ObjectSegment extends Protocol {

    @Override
    public String getName() {
        return "Object segmentation";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Objects are on which layer"),
            new ControlReference(COLOUR, "Background is which colour", new int[]{0}),
            new ControlReference(SPINNER, "Average object diameter (pixels)", 60),
            new ControlReference(TOGGLE, "Perform twice", 0),};
    }

    @Override
    protected Processor getProcessor() {
        Color bg = param.color[0];
        int nucleusSize = param.spinner[0];

        return new ProcessorFast(Tonga.log.isDebugEnabled() ? 2 : 1, "Objects", 5) {

            ImageData temp;

            @Override
            protected void pixelProcessor() {
            }

            @Override
            protected void methodFinal() {
                int nuclSize = Settings.settingsOverrideSizeEstimate() ? (int) (sourceWidth[0] / 10.) : nucleusSize;
                ROISet set = new ImageTracer(inImage[0], bg).trace();
                set.targetSize(nuclSize);
                set.findOuterEdges();
                set.findInnerEdges();
                set.analyzeCorners();
                set.analyzeCornerIntersections();
                set.segment(0);
                //this image is optional and for debug use
                if (Tonga.log.isDebugEnabled()) {
                    outImage[1] = set.drawToImageData();
                }
                if (param.toggle[0]) {
                    set = new ImageTracer(set.drawToImageData(true), COL.BLACK).trace();
                    set.targetSize(nuclSize);
                    set.findOuterEdges();
                    set.findInnerEdges();
                    set.analyzeCorners();
                    set.analyzeCornerIntersections();
                    set.segment(0);
                }
                outImage[0] = set.drawToImageData(true);
                //set = new ImageTracer(outImage[0], COL.BLACK).trace();
                //datas.add(SetCounters.countObjectsSingle(set).runSingle(sourceImage, outImage[0]));
            }
        };
    }
}
