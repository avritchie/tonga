package mainPackage.counters;

import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import mainPackage.CachedImage;
import mainPackage.IO;
import mainPackage.ImageData;
import mainPackage.Settings;
import mainPackage.Tonga;
import mainPackage.TongaImage;
import mainPackage.TongaLayer;

public abstract class Counter {

    TableData data;
    String imageName;
    public String counterName;

    public Counter(String counter, String[] columns) {
        counterName = counter;
        data = new TableData(columns);
    }

    public TableData runAll() {
        int index = Tonga.getLayerIndex();
        int images = Tonga.getImageList().size();
        String commonName = Tonga.getLayer().layerName;
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

    public static void publish(TableData tableData) {
        if (tableData == null) {
            Tonga.log.warn("Attempted to publish an empty data table.");
            return;
        }
        Tonga.loader().maxProgress();
        boolean renew = !Settings.settingResultsAppend();
        // jos on asetus ja olemassaolevan datan rakenne on sama
        if (Settings.settingResultsAppend()) {
            DefaultTableModel model = (DefaultTableModel) Tonga.frame().resultTable.getModel();
            if (model.getRowCount() == 0 && model.getColumnCount() == 0) {
                renew = true;
            } else if (model.getColumnCount() != tableData.columns.length) {
                for (int i = 0; i < model.getColumnCount(); i++) {
                    if (!model.getColumnName(i).equals(tableData.columns[i])) {
                        renew = true;
                        break;
                    }
                }
            }
        }
        if (renew) {
            SwingUtilities.invokeLater(() -> {
                Tonga.frame().resultTable.setModel(new DefaultTableModel(tableData.getAsArray(), tableData.columns) {
                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return tableData.rows.get(0)[columnIndex].getClass();
                    }
                });
                Tonga.frame().tabbedPane.repaint();
                Tonga.frame().tabbedPane.setSelectedIndex(3);
            });
        } else {
            DefaultTableModel model = (DefaultTableModel) Tonga.frame().resultTable.getModel();
            tableData.rows.forEach(d -> {
                model.addRow(d);
            });
            SwingUtilities.invokeLater(() -> {
                Tonga.frame().tabbedPane.repaint();
                Tonga.frame().tabbedPane.setSelectedIndex(3);
            });
        }
    }

    public static TableData createTable(String[] columns, int rowNumber, String rowTitle) {
        TableData ret = new TableData(columns);
        for (int i = 0; i < rowNumber; i++) {
            ret.newRow(rowTitle)[1] = box(i);
        }
        return ret;
    }

    public static Integer box(int i) {
        return Integer.valueOf(i);
    }

    public static void rowIntInc(TableData data, int row, int column) {
        Integer target = (Integer) data.rows.get(row)[column];
        data.rows.get(row)[column] = target + 1;
    }

    private static ImageData retrieveImage(TongaLayer img) {
        CachedImage toQuantify = null;
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
