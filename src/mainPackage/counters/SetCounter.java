package mainPackage.counters;

import javafx.scene.paint.Color;
import mainPackage.IO;
import mainPackage.ImageData;
import mainPackage.Settings;
import mainPackage.Tonga;
import mainPackage.TongaImage;
import mainPackage.TongaLayer;
import mainPackage.morphology.CellSet;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public abstract class SetCounter extends Counter {

    public SetCounter(String name,String[] columns) {
        super(name,columns);
    }

    @Override
    protected void handle(ImageData img) {
        Object[] newRow = data.newRow(imageName);
        processor(newRow);
    }

    protected void processor(Object[] newRow) {
    }

    public TableData runSingle(TongaImage image) {
        return runSingle(image.imageName, null);
    }

    protected ROISet getROISet(ROISet traced, ImageData targetImage) {
        return traced == null ? new ImageTracer(targetImage, Color.BLACK).trace() : traced;
    }

    protected CellSet getCellSet(CellSet traced, ImageData targetImage) {
        return new CellSet(getROISet(traced, targetImage));
    }

    private static TongaLayer retrieveImage(TongaLayer img) {
        if (Settings.settingBatchProcessing()) {
            try {
                return new TongaLayer(IO.getImageFromFile(img.path));
            } catch (Exception ex) {
                Tonga.catchError(ex, "Unable to read the file " + img.path);
            }
        } else {
            return img;
        }
        return null;
    }
}
