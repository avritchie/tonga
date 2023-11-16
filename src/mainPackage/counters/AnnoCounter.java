package mainPackage.counters;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import mainPackage.TongaAnnotation;
import mainPackage.TongaAnnotations;
import mainPackage.TongaImage;

public abstract class AnnoCounter extends Counter {

    TongaAnnotations annotations;

    public AnnoCounter(String name, String[] columns, String[] descs) {
        super(name, columns, descs);
    }

    public AnnoCounter(String name, String[] columns, String[] descs, ControlReference[] params) {
        super(name, columns, descs, params);
    }

    public TableData runSingle(TongaImage image, Object... parameters) {
        param.setFilterParameters(parameterData, parameters);
        return runSingle(image, (ImageData) null);
    }

    @Override
    protected void handle(TongaImage image, ImageData layer) {
        annotations = image.annotations;
        super.handle(image, layer);
    }

    @Override
    protected void initRows() {
        //
    }

    @Override
    protected void pixelProcessor(ImageData image) {
        for (int i = 0; i < annotations.getAnnotations().size(); i++) {
            annotationIterator(image, annotations.getAnnotations().get(i));
        }
    }

    abstract void annotationIterator(ImageData image, TongaAnnotation annotation);

    protected boolean correctGroup(TongaAnnotation annotation) {
        return annotation.getGroup() == param.annotationGroup[0] || param.annotationGroup[0] == -1;
    }

    protected boolean correctType(TongaAnnotation annotation) {
        return annotation.getType() == param.annotationType[0] || param.annotationType[0] == null;
    }
}
