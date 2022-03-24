package mainPackage.counters;

import mainPackage.TongaTable;
import mainPackage.MappedImage;
import mainPackage.IO;
import mainPackage.ImageData;
import mainPackage.Tonga;
import mainPackage.TongaImage;
import mainPackage.TongaLayer;

public abstract class Counter {

    TableData data;
    String imageName;
    public String counterName;
    public String[] columnNames;
    public String[] columnDescriptions;

    public Counter(String counter, String[] columns, String[] descs) {
        counterName = counter;
        columnNames = columns;
        columnDescriptions = descs;
        resetData();
    }

    public TableData runAll() {
        int index = Tonga.getLayerIndex();
        int images = Tonga.getImageList().size();
        String commonName = Tonga.getLayer().layerName;
        resetData();
        Tonga.loader().setIterations(images);
        for (int i = 0; i < images; i++) {
            TongaImage pic = Tonga.getImageList().get(i);
            TongaLayer img = pic.layerList.get(index);
            if (pic.layerList.size() - 1 >= index && img.layerName.equals(commonName)) {
                runSingle(pic, img);
            }
        }
        return data;
    }

    public TableData runSingle() {
        resetData();
        Tonga.loader().setIterations(1);
        return runSingle(Tonga.getImage(), Tonga.getLayerList().get(Tonga.getLayerIndex()));
    }

    public TableData runSingle(String name, ImageData layer) {
        imageName = name;
        handle(layer);
        return data;
    }

    public TableData runSingle(TongaImage image, ImageData layer) {
        return runSingle(image.imageName, layer);
        }

    public TableData runSingle(TongaImage image, TongaLayer layer) {
        ImageData img = retrieveImage(layer);
        return runSingle(image.imageName, img);
    }

    protected void handle(ImageData img) {
        Object[] newRow = data.newRow(imageName);
        preProcessor(img, newRow);
        pixelProcessor(img, newRow);
        postProcessor(img, newRow);
    }

    protected void pixelProcessor(ImageData targetImage, Object[] rowToEdit) {
        Tonga.iteration();
        for (int y = 0; y < targetImage.height; y++) {
            for (int x = 0; x < targetImage.width; x++) {
        if (targetImage.bits == 8) {
                    pixelIterator32(targetImage.pixels32, y * targetImage.width + x, rowToEdit);
        } else {
            try {
                        pixelIterator16(targetImage.pixels16, y * targetImage.width + x, rowToEdit);
            } catch (UnsupportedOperationException ex) {
                Tonga.log.info("The method {} does not support 16-bit images.", counterName);
                targetImage.set8BitPixels();
                        pixelIterator32(targetImage.pixels32, y * targetImage.width + x, rowToEdit);
            }
        }
    }
            Tonga.loader().appendProgress(targetImage.height);
            if (Tonga.loader().getTask().isInterrupted()) {
                return;
            }
        }
    }

    protected void preProcessor(ImageData targetImage, Object[] rowToEdit) {
    }

    protected void postProcessor(ImageData targetImage, Object[] rowToEdit) {
    }

    protected void pixelIterator32(int[] pixels, int p, Object[] rowToEdit) {
    }

    protected void pixelIterator16(short[] pixels, int p, Object[] rowToEdit) {
        throw new UnsupportedOperationException("No 16-bit version available");
    }

    private void resetData() {
        data = new TableData(columnNames, columnDescriptions);
    }

    public static void publish(TableData tableData) {
        if (tableData == null || tableData.rows.isEmpty()) {
            Tonga.log.warn("Attempted to publish an empty data table.");
            return;
        }
        Tonga.loader().maxProgress();
        TongaTable.publishData(tableData);
    }

    private static ImageData retrieveImage(TongaLayer img) {
        MappedImage toQuantify = null;
        if (img.isPointer) {
            try {
                toQuantify = IO.getImageFromFile(img.path);
            } catch (Exception ex) {
                Tonga.catchError(ex, "Unable to read the file " + img.path);
            }
        } else {
            toQuantify = img.layerImage;
        }
        return new ImageData(toQuantify);
    }
}
