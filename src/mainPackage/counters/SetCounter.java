package mainPackage.counters;

import javafx.scene.paint.Color;
import mainPackage.ImageData;
import mainPackage.TongaImage;
import mainPackage.morphology.CellSet;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public abstract class SetCounter extends Counter {

    public SetCounter(String name, String[] columns, String[] descs) {
        super(name, columns, descs);
    }

    @Override
    protected void handle(Object[] newRow, ImageData img) {
        processor(newRow);
    }

    protected void processor(Object[] newRow) {
    }

    public TableData runSingle(TongaImage image) {
        return runSingle(image, null);
    }

    protected ROISet getROISet(ROISet traced, ImageData targetImage) {
        return traced == null ? new ImageTracer(targetImage, Color.BLACK).trace() : traced;
    }

    protected CellSet getCellSet(CellSet traced, ImageData targetImage) {
        return new CellSet(getROISet(traced, targetImage));
    }

}
