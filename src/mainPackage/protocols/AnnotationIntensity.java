package mainPackage.protocols;

import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.AnnoCounters;

public class AnnotationIntensity extends Protocol {

    @Override
    public String getName() {
        return "Annotation intensity";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Quantify intensity at which layer"),
            new ControlReference(ANNOTATION_TYPE, "Restrict to annotation type", 0),
            new ControlReference(ANNOTATION_GROUP, "Restrict to annotation group", 0),
            new ControlReference(TOGGLE, "Report results as individual objects")};
    }

    @Override
    protected Processor getProcessor() {
        boolean individual = param.toggle[0];

        return new ProcessorFast(0, "Annotations", 1) {

            @Override
            protected void pixelProcessor() {
            }

            @Override
            protected void methodFinal() {
                if (individual) {
                    addResultData(AnnoCounters.areaIntensity().runSingle(sourceImage, inImage[0], param.annotationType[0], param.annotationGroup[0]));
                } else {
                    addResultData(AnnoCounters.areaIntensityImage().runSingle(sourceImage, inImage[0], param.annotationType[0], param.annotationGroup[0]));
                }
            }
        };
    }
}
