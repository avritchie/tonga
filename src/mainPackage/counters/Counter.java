package mainPackage.counters;

import mainPackage.TongaTable;
import mainPackage.MappedImage;
import mainPackage.IO;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.Tonga;
import mainPackage.TongaImage;
import mainPackage.TongaLayer;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

public abstract class Counter {

    TableData data;
    String imageName;
    Length imageScaling;
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
            TongaImage image = Tonga.getImageList().get(i);
            TongaLayer layer = image.layerList.get(index);
            if (image.layerList.size() - 1 >= index && layer.layerName.equals(commonName)) {
                handle(image.imageName, image.imageScaling, retrieveImage(layer));
            }
        }
        return pack();
    }

    public TableData runSingle() {
        resetData();
        Tonga.loader().setIterations(1);
        TongaImage image = Tonga.getImage();
        TongaLayer layer = Tonga.getLayerList().get(Tonga.getLayerIndex());
        handle(image.imageName, image.imageScaling, retrieveImage(layer));
        return pack();
    }

    public TableData runSingle(TongaImage image, ImageData source) {
        handle(image.imageName, image.imageScaling, source);
        return data;
    }

    private void handle(String name, Length scale, ImageData layer) {
        imageName = name;
        if (imageScaling == null || imageScaling.unit().getSymbol().equals(scale.unit().getSymbol())) {
            imageScaling = scale;
        } else {
            imageScaling = null;
        }
        Object[] newRow = data.newRow(imageName);
        handle(newRow, layer);
    }

    protected void handle(Object[] newRow, ImageData img) {
        preProcessor(img, newRow);
        pixelProcessor(img, newRow);
        postProcessor(img, newRow);
    }

    protected void pixelProcessor(ImageData targetImage, Object[] rowToEdit) {
        if (targetImage.bits == 8) {
            Iterate.pixels(targetImage, (int p) -> {
                pixelIterator32(targetImage.pixels32, p, rowToEdit);
            });
        } else {
            try {
                Iterate.pixels(targetImage, (int p) -> {
                    pixelIterator16(targetImage.pixels16, p, rowToEdit);
                });
            } catch (UnsupportedOperationException ex) {
                Tonga.log.info("The method {} does not support 16-bit images.", counterName);
                targetImage.set8BitPixels();
                Iterate.pixels(targetImage, (int p) -> {
                    pixelIterator32(targetImage.pixels32, p, rowToEdit);
                });
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

    public static Object[] scaleUnit(Object pixels, int dimensions, Length scaling) {
        if (scaling == null) {
            return new Object[]{pixels, null, null};
        } else {
            Double value = TableData.getType(pixels);
            if (value == null) {
                return new Object[]{pixels, null, null};
            }
            value = value * Math.pow(scaling.value().doubleValue(), dimensions);
            return new Object[]{pixels, value, scaling.unit()};
        }
    }

    protected Object scaleUnit(Object pixels, int dimensions) {
        return scaleUnit(pixels, dimensions, imageScaling);
    }

    public static void setUnit(TableData table) {
        for (int c = 0; c < table.columns.length; c++) {
            if (table.columns[c].contains("%unit")) {
                Unit<Length> rawUnit = (Unit<Length>) ((Object[]) (table.rows.get(0)[c]))[2];
                String unit = rawUnit == null ? null : rawUnit.getSymbol();
                boolean useUnits = unit != null;
                if (useUnits) {
                    for (int r = 0; r < table.rows.size(); r++) {
                        Object[] row = table.rows.get(r);
                        Unit<Length> rowUnit = (Unit<Length>) ((Object[]) row[c])[2];
                        if (rowUnit == null || !unit.equals(rowUnit.getSymbol())) {
                            useUnits = false;
                            break;
                        }
                    }
                }
                unit = useUnits ? unit : "px";
                if (!useUnits) {
                    table.columns[c] = table.columns[c].replace("%unit2", "pixels").replace("%unit", "pixels");
                    table.descriptions[c] = table.descriptions[c].replace("%unit2", "pixels").replace("%unit", "pixels") + " (no unit metadata available, can not be converted to real units)";
                    for (int r = 0; r < table.rows.size(); r++) {
                        Object[] row = table.rows.get(r);
                        row[c] = ((Object[]) row[c])[0];
                    }
                } else {
                    table.columns[c] = table.columns[c].replace("%unit2", unit + "²").replace("%unit", unit);
                    table.descriptions[c] = table.descriptions[c].replace("%unit2", unit + "²").replace("%unit", unit);
                    for (int r = 0; r < table.rows.size(); r++) {
                        Object[] row = table.rows.get(r);
                        row[c] = ((Object[]) row[c])[1];
                    }
                }
            }
        }
    }

    private TableData pack() {
        setUnit(data);
        return data;
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
