package mainPackage.counters;

import mainPackage.TongaTable;
import mainPackage.MappedImage;
import mainPackage.IO;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator;
import mainPackage.PanelCreator.ControlReference;
import mainPackage.PanelParams;
import mainPackage.PanelUtils;
import mainPackage.Tonga;
import mainPackage.TongaImage;
import mainPackage.TongaLayer;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

public abstract class Counter {

    public TableData data;
    public String imageName;
    public Length imageScaling;
    public int imageWidth;
    public int imageHeight;
    public Object[] row;
    public String counterName;
    public String[] columnNames;
    public String[] columnDescriptions;

    public ControlReference[] parameterData;
    public PanelCreator panelCreator;
    public PanelParams param;

    public Counter(String counter, String[] columns, String[] descs) {
        this(counter, columns, descs, null);
    }

    public Counter(String counter, String[] columns, String[] descs, ControlReference[] params) {
        counterName = counter;
        columnNames = columns;
        columnDescriptions = descs;
        resetData();
        parameterData = params != null ? params : new ControlReference[0];
        param = new PanelParams(parameterData);
    }

    public final void loadComponents() {
        panelCreator = new PanelCreator(parameterData);
        PanelUtils.updateComponents(panelCreator);
    }

    public final void loadParams() {
        param.getFilterParameters(panelCreator);
    }

    public final String getName() {
        return counterName;
    }

    public TableData runAll() {
        int index = Tonga.getLayerIndex();
        int images = Tonga.getImageList().size();
        String commonName = Tonga.getLayer().layerName;
        resetData();
        Tonga.loader().setIterations(images);
        for (int i = 0; i < images; i++) {
            TongaImage image = Tonga.getImageList().get(i);
            TongaLayer layer = image.getLayer(index);
            if (image.layerCount() - 1 >= index && layer.layerName.equals(commonName)) {
                handle(image, retrieveImage(layer));
            }
        }
        return pack();
    }

    public TableData runSingle() {
        resetData();
        Tonga.loader().setIterations(1);
        TongaImage image = Tonga.getImage();
        TongaLayer layer = Tonga.getLayerList().get(Tonga.getLayerIndex());
        handle(image, retrieveImage(layer));
        return pack();
    }

    public TableData runSingle(TongaImage image, ImageData source) {
        handle(image, source);
        return data;
    }

    public TableData runSingle(TongaImage image, ImageData source, Object... parameters) {
        param.setFilterParameters(parameterData, parameters);
        return runSingle(image, source);
    }

    protected void handle(TongaImage image, ImageData layer) {
        imageName = image.imageName;
        //layer value is not necessarily passed from protocols
        if (layer != null) {
            imageWidth = layer.width;
            imageHeight = layer.height;
        }
        imageScaling = image.imageScaling;
        handle(layer);
    }

    protected void handle(ImageData img) {
        initRows();
        preProcessor(img);
        pixelProcessor(img);
        postProcessor(img);
    }

    protected void pixelProcessor(ImageData targetImage) {
        if (targetImage.bits == 8) {
            Iterate.pixels(targetImage, (int p) -> {
                pixelIterator32(targetImage.pixels32, p);
            });
        } else {
            try {
                Iterate.pixels(targetImage, (int p) -> {
                    pixelIterator16(targetImage.pixels16, p);
                });
            } catch (UnsupportedOperationException ex) {
                Tonga.log.info("The method {} does not support 16-bit images.", counterName);
                targetImage.set8BitPixels();
                Iterate.pixels(targetImage, (int p) -> {
                    pixelIterator32(targetImage.pixels32, p);
                });
            }
        }
    }

    public void Rows(int rows, RowWriter i) {
        // make sure an initial row is set before running this
        for (int index = 0; index < rows; index++) {
            i.writeRow(index);
            if (index < rows - 1) {
                row = data.newRow(row[0].toString());
            }
        }
    }

    public interface RowWriter {

        void writeRow(int index);
    }

    protected void initRows() {
        row = data.newRow(imageName);
    }

    protected void preProcessor(ImageData targetImage) {
    }

    protected void postProcessor(ImageData targetImage) {
    }

    protected void pixelIterator32(int[] pixels, int p) {
    }

    protected void pixelIterator16(short[] pixels, int p) {
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
                try {
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
                } catch (ClassCastException ex) {
                    Tonga.catchError(ex, "Unit scaling failed. Column values do not contain a scale object.");
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
        Tonga.frame().resultTable.publishData(tableData);
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
