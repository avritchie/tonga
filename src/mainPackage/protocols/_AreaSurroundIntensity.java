package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator;
import static mainPackage.PanelCreator.ControlType.COLOUR;
import static mainPackage.PanelCreator.ControlType.LAYER;
import static mainPackage.PanelCreator.ControlType.SPINNER;
import static mainPackage.PanelCreator.ControlType.TOGGLE;
import mainPackage.counters.SetCounters;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

public class _AreaSurroundIntensity extends Protocol {

    @Override
    public String getName() {
        return "Calculate surrounding staining";
    }

    @Override
    protected PanelCreator.ControlReference[] getParameters() {
        return new PanelCreator.ControlReference[]{
            new PanelCreator.ControlReference(LAYER, "Track objects at which layer"),
            new PanelCreator.ControlReference(LAYER, "The image with the stain"),
            new PanelCreator.ControlReference(COLOUR, "Background is which color", -2),
            new PanelCreator.ControlReference(SPINNER, "Radius", 50),
            new PanelCreator.ControlReference(TOGGLE, "Results as average per image", 0)};
    }

    @Override
    protected Processor getProcessor() {
        boolean perimg = param.toggle[0];

        return new ProcessorFast("Objects") {

            ImageData tempMask;
            ROISet set;

            @Override
            protected void methodInit() {
                set = new ImageTracer(inImage[0], param.color[0]).trace();
                set.getExtendedMasks(param.spinner[0]);
                set.findOuterMaskEdges();
                tempMask = set.drawToImageData(true);
                //Filters.gaussApprox().runSingle(inImage[2], 2, true)
                set.quantifyStainOnMaskAgainstChannel(tempMask, COL.BLACK, inImage[1]);
                tempMask = new ImageData(set.drawSurroundArray(true), sourceWidth[0], sourceHeight[0]);
            }

            @Override
            protected void methodCore(int pos) {
                int c = tempMask.pixels32[pos];
                outImage[0].pixels32[pos] = c == COL.DGRAY ? inImage[1].pixels32[pos] : c;
            }

            @Override
            protected void methodFinal() {
                if (perimg) {
                    setDatasBy(SetCounters.countObjectStainsImage(set).runSingle(sourceImage));
                } else {
                    setDatasBy(SetCounters.countObjectStainsSingle(set).runSingle(sourceImage));
                }
            }
        };
    }
}
