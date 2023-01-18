package mainPackage;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import javafx.application.Platform;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.table.TableModel;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import static mainPackage.Tonga.mainFrame;
import static mainPackage.Tonga.picList;

public class IO {

    protected static String getFolder(String text) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose a folder");
        chooser.setCurrentDirectory(new File(text));
        int val = chooser.showOpenDialog(Tonga.frame());
        if (val == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    protected static File[] getFile(String text, boolean allowmany) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Choose files");
        chooser.setMultiSelectionEnabled(allowmany);
        chooser.setCurrentDirectory(new File(text));
        int val = chooser.showOpenDialog(Tonga.frame());
        if (val == JFileChooser.APPROVE_OPTION) {
            if (allowmany) {
                return chooser.getSelectedFiles();
            } else {
                return new File[]{chooser.getSelectedFile()};
            }
        }
        return null;
    }

    protected static void toTSVfile(JTable table, File file) {
        try {
            TableModel model = table.getModel();
            try ( FileWriter excel = new FileWriter(file)) {
                for (int i = 0; i < model.getColumnCount(); i++) {
                    excel.write(model.getColumnName(i).replaceAll("<[^>]*>", "") + "\t");
                }
                excel.write("\n");
                for (int i = 0; i < model.getRowCount(); i++) {
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        Object obj = model.getValueAt(i, j);
                        String str;
                        if (obj instanceof Double) {
                            str = obj.toString().replace(".", ",");
                        } else {
                            str = obj.toString();
                        }
                        excel.write(str + "\t");
                    }
                    excel.write("\n");
                }
                Tonga.log.info("Table exported as TSV into {}", file);
            }
        } catch (IOException ex) {
            Tonga.catchError(ex, "Result exporting failed.");
        }
    }

    protected static boolean importStack(File file) throws IOException, FormatException, ServiceException {
        if (Settings.settingBatchProcessing()) {
            Tonga.setStatus("Stacked images cannot be opened in the batch-mode since the contents may vary.");
        } else {
            TongaImage[] images = StackImporter.openFile(file);
            picList.addAll(Arrays.asList(images));
            return true;
        }
        return false;
    }

    public static void importStacks(List<File> files) {
        if (Settings.settingBatchProcessing()) {
            Tonga.setStatus("Stacked images cannot be opened in the batch-mode since the contents may vary.");
        } else {
            new Importer() {
                int count;

                @Override
                void iterate() {
                    count = picList.size();
                    for (int i = 0; i < files.size(); i++) {
                        file = files.get(i);
                        readFile();
                    }
                }

                @Override
                void read(MappedImage mi) throws IOException, FormatException, ServiceException, FileNotFoundException {
                    throw new FormatException("The file could not be imported as a stack image.");
                }

                @Override
                void readBatch() throws Exception {
                    throw new FormatException("Attempted to import stacks in the batch mode.");
                }

                @Override
                boolean readStack() throws IOException, FormatException, ServiceException {
                    return importStack(file);
                }

                @Override
                String message() {
                    count = picList.size() - count;
                    return "Imported image file(s) containing a total of " + count + " new images";
                }
            }.importFile(files);
        }
        /*
        if (Settings.settingBatchProcessing()) {
            Tonga.setStatus("Stacked images cannot be opened in the batch-mode since the contents may vary.");
        } else {
            Thread thread = new Thread(() -> {
                Tonga.loader().setIterations(files.size());
                int failures = 0;
                boolean formatissue = false;
                int count = picList.size();
                for (int i = 0; i < files.size(); i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    File file = files.get(i);
                    try {
                        importStack(file);
                    } catch (IOException | ServiceException | FormatException ex) {
                        if (file.isDirectory()) {
                            Tonga.catchError(ex, "Folder importing is not supported.");
                        } else {
                            Tonga.catchError(ex, "Image file can not be imported.");
                        }
                        failures++;
                        if (ex instanceof FormatException || ex instanceof IllegalStateException) {
                            formatissue = true;
                            Tonga.log.warn("Unsupported format");
                        }
                        Tonga.loader().appendToNext();
                    }
                }
                count = picList.size() - count;
                if (failures == files.size()) {
                    Tonga.refreshChanges(files.get(0), "<font color=\"red\">Image importing failed.</font>" + (formatissue ? " Unsupported file format." : ""));
                } else {
                    Tonga.refreshChanges(files.get(0), "Imported image file(s) containing a total of " + count + " new images"
                            + (failures > 0 ? " but " + failures + " file" + (failures > 1 ? "s" : "") + " failed to be imported." : "."));
                }
            }
            );
            Tonga.bootThread(thread, "Importer", false, true);
        }*/
    }

    public static void importLayers(List<File> files, boolean toAllImages) {
        new Importer() {
            int imgEnts;
            int[] imgIds;
            int imgId;
            int mod;

            @Override
            void iterate() {
                if (toAllImages) {
                    // all images get new layers
                    imgEnts = Tonga.imageListModel.size();
                    imgIds = new int[imgEnts];
                    for (int i = 0; i < imgEnts; i++) {
                        imgIds[i] = i;
                    }
                } else {
                    // only selected images get new layers
                    imgEnts = Tonga.getImageIndexes().length;
                    imgIds = Tonga.getImageIndexes();
                }
                if (files.size() % imgEnts == 0) {
                    mod = files.size() / imgEnts;
                    for (int i = 0; i < imgEnts; i++) {
                        for (int j = 0; j < mod; j++) {
                            file = files.get(i * mod + j);
                            imgId = imgIds[i];
                            readFile();
                        }
                    }
                } else {
                    Tonga.setStatus("The number of files has to be divisible with the number of images (" + imgEnts + ")");
                    cancelled = true;
                }
            }

            @Override
            void read(MappedImage mi) throws Exception {
                Tonga.injectNewLayer(mi, "Layer", imgId);
            }

            @Override
            void readBatch() throws Exception {
                Tonga.injectNewLayer(file.getAbsolutePath(), "Layer", imgId);
            }

            @Override
            boolean readStack() throws IOException, FormatException, ServiceException {
                if (StackImporter.isStackImage(file)) {
                    stackissue = true;
                }
                return false;
            }

            @Override
            String message() {
                return "Imported " + mod + " new layers to " + imgEnts + " images";
            }
        }.importFile(files);
        /*
        if (Settings.settingBatchProcessing()) {
            int mod = files.size() / Tonga.imageListModel.size();
            for (int i = 0; i < Tonga.imageListModel.size(); i++) {
                for (int j = 0; j < mod; j++) {
                    File file = files.get(i * mod + j);
                    Tonga.injectNewLayer(file.getAbsolutePath(), "Layer", i);
                }
            }
            Tonga.refreshChanges(files.get(0), "Imported file pointers");
        } else {
            Thread thread = new Thread(() -> {
                Tonga.loader().setIterations(files.size());
                boolean formatissue = false;
                int failures = 0;
                int stacks = 0;
                int imgEnts;
                int[] imgIds;
                if (toAllImages) {
                    // all images get new layers
                    imgEnts = Tonga.imageListModel.size();
                    imgIds = new int[imgEnts];
                    for (int i = 0; i < imgEnts; i++) {
                        imgIds[i] = i;
                    }
                } else {
                    // only selected images get new layers
                    imgEnts = Tonga.getImageIndexes().length;
                    imgIds = Tonga.getImageIndexes();
                }
                if (files.size() % imgEnts == 0) {
                    int mod = files.size() / imgEnts;
                    for (int i = 0; i < imgEnts; i++) {
                        for (int j = 0; j < mod; j++) {
                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }
                            File file = files.get(i * mod + j);
                            try {
                                if (StackImporter.isStackImage(file)) {
                                    stacks++;
                                } else {
                                    Tonga.injectNewLayer(file, "Layer", imgIds[i]);
                                    Tonga.loader().appendProgress(1.0);
                                }
                            } catch (Exception ex) {
                                if (file.isDirectory()) {
                                    Tonga.catchError(ex, "Folder importing is not supported.");
                                } else {
                                    Tonga.catchError(ex, "Image file can not be imported.");
                                }
                                failures++;
                                if (ex instanceof FormatException || ex instanceof IllegalStateException) {
                                    formatissue = true;
                                    Tonga.log.warn("Unsupported format");
                                }
                                Tonga.loader().appendProgress(1.0);
                            }
                        }
                    }
                    if (failures == files.size()) {
                        Tonga.refreshChanges(files.get(0), "<font color=\"red\">Image importing failed.</font> "
                                + (stacks > 0 ? "Stack images can not be imported as layers." : "")
                                + (formatissue ? " Unsupported file format." : ""));
                    } else {
                        Tonga.refreshChanges(files.get(0), "Imported " + mod + " new layers to " + imgEnts + " images"
                                + (failures > 0 ? " but " + failures + " file" + (failures > 1 ? "s" : "") + " failed to be imported." : ".")
                                + (stacks > 0 ? "Stack images can not be imported as layers." : ""));
                    }
                } else {
                    Tonga.setStatus("Number of files has to be divisible with the number of images (" + imgEnts + ")");
                }
            });
            Tonga.bootThread(thread, "Importer", false, true);
        }*/
    }

    public static void importMultichannel(List<File> files) {
        Object[] reply = IO.askMultichannel();
        if ((boolean) reply[0]) {
            int channels = (int) reply[1];
            boolean order = (boolean) reply[2]; //true = channel order, false = image order
            int imgs = files.size() / channels;
            new Importer() {
                TongaImage image;

                @Override
                void iterate() {
                    if (files.size() % channels == 0) {
                        if (order) { //img1_ch1,img2_ch1,img1_ch2,img2_ch2
                            for (int i = 0; i < imgs; i++) {
                                for (int c = 0; c < channels; c++) {
                                    file = files.get(c * imgs + i);
                                    if (c == 0) {
                                        image = new TongaImage(file);
                                    }
                                    readFile();
                                }
                                if (!image.layerList.isEmpty()) {
                                    picList.add(image);
                                }
                            }
                        } else { //img1_ch1,img1_ch2,img1_ch3
                            for (int i = 0; i < files.size(); i++) {
                                file = files.get(i);
                                if (i % channels == 0) {
                                    image = new TongaImage(file);
                                }
                                readFile();
                                if (i % channels == channels - 1) {
                                    if (!image.layerList.isEmpty()) {
                                        picList.add(image);
                                    }
                                }
                            }
                        }
                    } else {
                        Tonga.setStatus("The number of files has to be divisible with the number of channels (" + channels + ")");
                        cancelled = true;
                    }
                }

                @Override
                void read(MappedImage mi) throws Exception {
                    image.layerList.add(new TongaLayer(mi, "Channel #" + (image.layerList.size() + 1)));
                }

                @Override
                void readBatch() throws Exception {
                    image.layerList.add(new TongaLayer(file.getAbsolutePath(), "Channel #" + (image.layerList.size() + 1)));
                }

                @Override
                boolean readStack() throws IOException, FormatException, ServiceException {
                    if (StackImporter.isStackImage(file)) {
                        stackissue = true;
                    }
                    return false;
                }

                @Override
                String message() {
                    return "Imported " + imgs + " new images with " + channels + " layers";
                }
            }.importFile(files);
        }
    }

    public static void importImages(List<File> files) {
        if (files.size() > 50 && picList.isEmpty() && !Settings.settingBatchProcessing()
                && Tonga.askYesNo("Use the batch mode", "You are trying to import a large number of images at once."
                        + "Do you want to switch to the batch processing mode?<br><br>"
                        + "In the batch mode you can execute protocols to the files directly without a need to import them first. "
                        + "This can be useful if you already know what protocol and settings you want to use.", true, false)) {
            Tonga.frame().boxSettingBatch.setSelected(true);
        }
        new Importer() {
            @Override
            void iterate() {
                for (int i = 0; i < files.size(); i++) {
                    file = files.get(i);
                    readFile();
                }
            }

            @Override
            void read(MappedImage mi) throws Exception {
                picList.add(new TongaImage(mi, file.getName(), "Original"));
                images++;
            }

            @Override
            void readBatch() throws Exception {
                picList.add(new TongaImage(file.getAbsolutePath(), "Original"));
                images++;
            }

            @Override
            boolean readStack() throws IOException, FormatException, ServiceException {
                if (StackImporter.isStackImage(file)) {
                    importStack(file);
                    stacks++;
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            String message() {
                return "Imported " + (images > 0 ? images + " new images" : "");
            }
        }.importFile(files);
        /*
        if (Settings.settingBatchProcessing()) {
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                TongaImage ti = new TongaImage(file);
                picList.add(ti);
                ti.layerList.add(new TongaLayer(file.getAbsolutePath(), "Original"));
            }
            Tonga.refreshChanges(files.get(0), "Imported file pointers");
        } else {
            Thread thread = new Thread(() -> {
                Tonga.loader().setIterations(files.size());
                boolean formatissue = false;
                int failures = 0;
                int stacks = 0;
                int images = 0;
                for (int i = 0; i < files.size(); i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    File file = files.get(i);
                    try {
                        if (StackImporter.isStackImage(file)) {
                            importStack(file);
                            stacks++;
                        } else {
                            picList.add(new TongaImage(file, "Original"));
                            images++;
                            Tonga.loader().appendProgress(1.0);
                        }
                    } catch (Exception ex) {
                        if (file.isDirectory()) {
                            Tonga.catchError(ex, "Folder importing is not supported.");
                        } else {
                            Tonga.catchError(ex, "Image file can not be imported.");
                        }
                        failures++;
                        if (ex instanceof FormatException || ex instanceof IllegalStateException) {
                            formatissue = true;
                            Tonga.log.warn("Unsupported format");
                        }
                        Tonga.loader().appendToNext();
                    }
                }
                if (failures == files.size()) {
                    Tonga.refreshChanges(files.get(0), "<font color=\"red\">Image importing failed.</font>" + (formatissue ? " Unsupported file format." : ""));
                } else {
                    Tonga.refreshChanges(files.get(0), "Imported "
                            + (images > 0 ? images + " new images" + (stacks > 0 ? " and " : "") : "")
                            + (stacks > 0 ? stacks + " stack image(s)" : "")
                            + (failures > 0 ? " but " + failures + " file" + (failures > 1 ? "s" : "") + " failed to be imported." : "."));
                }
            });
            Tonga.bootThread(thread, "Importer", false, true);
        }*/
    }

    public static void importImage(List<File> files) {
        if (files.size() > 5 && Tonga.askYesNo("Import as images", "You are trying to import ONE new image with " + files.size() + " layers. "
                + "Did you mean to import " + files.size() + " separate images with one layer each instead?", true, false)) {
            importImages(files);
        } else {
            new Importer() {
                TongaImage image;

                @Override
                void iterate() {
                    image = new TongaImage(files.get(0));
                    for (int i = 0; i < files.size(); i++) {
                        file = files.get(i);
                        readFile();
                    }
                    layers = image.layerList.size();
                    if (layers > 0) {
                        picList.add(image);
                    }
                }

                @Override
                void read(MappedImage mi) throws Exception {
                    image.layerList.add(new TongaLayer(mi, image.layerList.isEmpty() ? "Original" : "Layer"));
                }

                @Override
                void readBatch() throws Exception {
                    image.layerList.add(new TongaLayer(file.getAbsolutePath(), image.layerList.isEmpty() ? "Original" : "Layer"));
                }

                @Override
                boolean readStack() throws IOException, FormatException, ServiceException {
                    if (StackImporter.isStackImage(file)) {
                        importStack(file);
                        stacks++;
                        return true;
                    } else {
                        return false;
                    }
                }

                @Override
                String message() {
                    return "Imported " + (layers > 0 ? "a new image with " + layers + " layers" : "");
                }
            }.importFile(files);
            /*
            if (Settings.settingBatchProcessing()) {
                File file = files.get(0);
                TongaImage ti = new TongaImage(file);
                ti.layerList.add(new TongaLayer(file.getAbsolutePath(), "Original"));
                for (int i = 1; i < files.size(); i++) {
                    file = files.get(i);
                    ti.layerList.add(new TongaLayer(file.getAbsolutePath(), "Layer"));
                }
                picList.add(ti);
                Tonga.refreshChanges(files.get(0), "Imported file pointers");
            } else {
                Thread thread = new Thread(() -> {
                    Tonga.loader().setIterations(files.size());
                    boolean formatissue = false;
                    int failures = 0;
                    int stacks = 0;
                    int layers = 0;
                    TongaImage image = new TongaImage(files.get(0));
                    try {
                        for (int i = 0; i < files.size(); i++) {
                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }
                            File file = files.get(i);
                            TongaLayer layer;
                            try {
                                if (StackImporter.isStackImage(file)) {
                                    importStack(file);
                                    stacks++;
                                } else {
                                    layer = new TongaLayer(getImageFromFile(file), i == 0 ? "Original" : "Layer");
                                    image.layerList.add(layer);
                                    Tonga.loader().appendProgress(1.0);
                                }
                            } catch (ClosedChannelException ex) {
                                Tonga.log.warn("Interrupted wile BFIO importing.");
                            } catch (Exception ex) {
                                if (file.isDirectory()) {
                                    Tonga.catchError(ex, "Folder importing is not supported.");
                                } else {
                                    Tonga.catchError(ex, "Image file can not be imported.");
                                }
                                failures++;
                                if (ex instanceof FormatException || ex instanceof IllegalStateException) {
                                    formatissue = true;
                                    Tonga.log.warn("Unsupported format");
                                }
                                Tonga.loader().appendToNext();
                            }
                        }
                        layers = image.layerList.size();
                        if (layers > 0) {
                            picList.add(image);
                        }
                    } catch (Exception ex) {
                        Tonga.catchError(ex, "Image file can not be imported.");
                        failures++;
                        if (ex instanceof FormatException || ex instanceof IllegalStateException) {
                            formatissue = true;
                            Tonga.log.warn("Unsupported format");
                        }
                        Tonga.loader().appendToNext();
                    }
                    if (failures == files.size()) {
                        Tonga.refreshChanges(files.get(0), "<font color=\"red\">Image importing failed.</font>" + (formatissue ? " Unsupported file format." : ""));
                    } else {
                        Tonga.refreshChanges(files.get(0), "Imported "
                                + (layers > 0 ? "a new image with " + layers + " layers" + (stacks > 0 ? " and " : "") : "")
                                + (stacks > 0 ? stacks + " stack image(s)" : "")
                                + (failures > 0 ? " but " + failures + " file" + (failures > 1 ? "s" : "") + " failed to be imported." : "."));
                    }
                });
                Tonga.bootThread(thread, "Importer", false, true);
            }*/
        }
    }

    public static Object[] askFormat(File file) {
        long size = 0;
        try {
            size = (int) Files.size(file.toPath());
        } catch (IOException ex) {
        }
        final int s = (int) size;
        int sq = (int) Math.sqrt(s / 4);
        String inputText = "<html><body><p style='width: 250px;'>" + "The format of the file " + file.getName() + " could not be identified. Please input the width and the format of this image manually.<br><br>";
        JLabel labelw = new JLabel();
        labelw.setText("Width: ");
        JLabel labelh = new JLabel();
        labelh.setText(" Height: ");
        JLabel labelt = new JLabel();
        labelt.setText("?");
        JTextField inputw = new JTextField();
        inputw.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputw.setPreferredSize(new Dimension(80, 26));
        JPanel dimPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dimPanel.add(labelw);
        dimPanel.add(inputw);
        dimPanel.add(labelh);
        dimPanel.add(labelt);
        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String[] formats = {"ARGB", "RGBA", "BGRA", "RGB", "16GRAY", "8GRAY"};
        JComboBox format = new JComboBox(formats);
        format.setSelectedIndex(2);
        format.addActionListener((ActionEvent e) -> {
            inputw.selectAll();
        });
        inputw.addCaretListener((CaretEvent e) -> {
            try {
                int w = Integer.parseInt(inputw.getText());
                int t = 0;
                switch (format.getSelectedIndex()) {
                    case 0:
                    case 1:
                    case 2:
                        t = s / 4;
                        break;
                    case 3:
                        t = s / 3;
                        break;
                    case 4:
                        t = s / 2;
                        break;
                    case 5:
                        t = s;
                        break;
                }
                int h = (t / w) + (t % w > 0 ? 1 : 0);
                labelt.setText(Integer.toString(h));
            } catch (NumberFormatException ex) {
                labelt.setText("?");
            }
        });
        inputw.setText(Integer.toString(sq));
        inputw.selectAll();
        JLabel formatl = new JLabel();
        formatl.setText("Format: ");
        formatPanel.add(formatl);
        formatPanel.add(format);
        Object[] p = {inputText, dimPanel, formatPanel};
        Object[] butt = {"Import", "Cancel"};
        int r = JOptionPane.showOptionDialog(mainFrame, p, "Raw importer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, butt, butt[0]);
        boolean proceed = r == 0;
        int w = 0;
        int formatsel = format.getSelectedIndex();
        if (proceed) {
            if (inputw.getText().isBlank()) {
                Tonga.setStatus("Please input the width.");
                proceed = false;
            } else {
                try {
                    w = Integer.parseInt(inputw.getText());
                    if (w <= 0) {
                        Tonga.setStatus("The dimensions must be greater than 0.");
                        proceed = false;
                    }
                } catch (NumberFormatException ex) {
                    Tonga.setStatus(inputw.getText() + " is not a valid number.");
                    proceed = false;
                }
            }
        }
        return new Object[]{proceed, w, formatsel};
    }

    public static Object[] askMultichannel() {
        String inputText = "<html><body><p style='width: 300px;'>" + "Please input the number of channels in this dataset. "
                + "E.g. if you have 8 images and each of them has a DAPI channel and a GFP channel, the number of channels will be 2. "
                + "Make sure that every image has this many channels. In this example, you would have 16 separate image files in total."
                + "<br><br>The number of channels:</p></body></html>";
        String radioText = "<html><body><br><p style='width: 300px;'>" + "Please select the order of files. "
                + "<font color=\"#7878f0\">Image order</font> means \"img1_ch1, img1_ch2, img1_ch3, img2_ch1, img2_ch2, img2_ch3\" type of order and "
                + "<font color=\"#7878f0\">channel order</font> means \"ch1_img1, ch1_img2, ch1_img3, ch2_img1, ch2_img2, ch2_img3\" type of order.</p><br></body></html>";
        JTextField input = new JTextField();
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(200, 30));
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        JRadioButton channel = new JRadioButton();
        JRadioButton image = new JRadioButton();
        ButtonGroup group = new ButtonGroup();
        channel.setText("Channel order");
        image.setText("Image order");
        image.setSelected(true);
        group.add(channel);
        group.add(image);
        radioPanel.add(image);
        radioPanel.add(channel);
        inputPanel.add(input);
        Object[] p = {inputText, inputPanel, radioText, radioPanel};
        Object[] butt = {"Import", "Cancel"};
        int r = JOptionPane.showOptionDialog(mainFrame, p, "Multichannel importer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, butt, butt[0]);
        boolean proceed = r == 0;
        int number = 0;
        if (proceed) {
            if (input.getText().isBlank()) {
                Tonga.setStatus("Please input the number of channels.");
                proceed = false;
            } else {
                try {
                    number = Integer.parseInt(input.getText());
                    if (number <= 0) {
                        Tonga.setStatus("The number of channels must be more than 0.");
                        proceed = false;
                    }
                } catch (NumberFormatException ex) {
                    Tonga.setStatus(input.getText() + " is not a valid number.");
                    proceed = false;
                }
            }
        }
        return new Object[]{proceed, number, channel.isSelected()};
    }

    public static MappedImage getImageFromFile(String file) throws FileNotFoundException, ServiceException, FormatException, IOException {
        return getImageFromFile(new File(file));
    }

    public static ImageData getImageDataFromFile(String file) throws FileNotFoundException, ServiceException, FormatException, IOException {
        return new ImageData(getImageFromFile(new File(file)));
    }

    public static MappedImage getImageFromFile(File file) throws FileNotFoundException, ServiceException, FormatException, IOException {
        if (file.exists()) {
            try {
                try {
                    MappedImage n;
                    n = new MappedImage(file);
                    return n;
                } catch (Exception ex) {
                    Tonga.log.debug("Unable to directly import {}", file.toString());
                    Tonga.log.debug("Will try the Bio-Formats importer instead");
                    return StackImporter.openFile(file)[0].layerList.get(0).layerImage;
                }
            } catch (IOException | ServiceException | FormatException ex) {
                throw ex;
            }
        } else {
            throw new FileNotFoundException();
        }
    }

    public static void exportLayer(boolean all) {
        Thread thread = all
                ? new Thread(() -> {
                    Tonga.loader().loaderProgress(0, picList.size());
                    int layer = Tonga.getLayerIndex();
                    if (picList.stream().mapToInt((i) -> i.layerList.size()).min().getAsInt() == Tonga.getLayerList().size()
                            && picList.stream().map((i) -> i.layerList.get(layer).layerName).distinct().count() == 1) {
                        picList.forEach((p) -> {
                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }
                            exportImage(p, layer);
                            Tonga.loader().loaderProgress(picList.indexOf(p) + 1, picList.size());
                        });
                        Tonga.setStatus("Selected layers of all images were exported into PNG format");
                    } else {
                        Tonga.setStatus("All of the images must have the same layer structure");
                    }
                }) : new Thread(() -> {
                    Tonga.loader().loaderProgress(0, 1);
                    if (exportImage(picList.get(Tonga.getImageIndex()), Tonga.getLayerIndex())) {
                        Tonga.loader().loaderProgress(1, 1);
                        Tonga.setStatus("Selected layer of the image exported into PNG format");
                    }
                });
        Tonga.bootThread(thread, "Exporter", false, true);
    }

    public static void exportLayers(boolean all) {
        Thread thread = all
                ? new Thread(() -> {
                    Tonga.loader().loaderProgress(0, picList.size());
                    picList.forEach((p) -> {
                        p.layerList.forEach((l) -> {
                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }
                            exportImage(p, p.layerList.indexOf(l));
                            Tonga.loader().loaderProgress((int) ((picList.indexOf(p)
                                    + (p.layerList.indexOf(l) + 1) / (double) p.layerList.size()) * 100), picList.size() * 100);
                        });
                        Tonga.loader().loaderProgress(picList.indexOf(p) + 1, picList.size());
                    });
                    Tonga.setStatus("All layers of all images were exported into PNG format");
                }) : new Thread(() -> {
                    Tonga.loader().loaderProgress(0, picList.size());
                    TongaImage p = Tonga.getImage();
                    p.layerList.forEach((l) -> {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        exportImage(p, p.layerList.indexOf(l));
                        Tonga.loader().loaderProgress(p.layerList.indexOf(l) + 1, p.layerList.size());
                    });
                    Tonga.loader().loaderProgress(picList.size(), picList.size());
                    Tonga.setStatus("All layers of the selected image were exported into PNG format");
                });
        Tonga.bootThread(thread, "Exporter", false, true);
    }

    public static void exportStack(boolean all) {
        Thread thread = new Thread(() -> {
            Tonga.loader().loaderProgress(0, picList.size());
            for (int i = all ? 0 : Tonga.getImageIndex(); i <= (all ? picList.size() - 1 : Tonga.getImageIndex()); i++) {
                TongaImage m = Tonga.picList.get(i);
                boolean stackmem = m.stack;
                m.stack = true;
                TongaLayer[] layersToRender = Tonga.imageLayersFromIndexList(i);
                ImageData renderedImage = TongaRender.renderImage(Tonga.layersAs8BitImageDataArray(layersToRender));
                exportImage(m, renderedImage);
                Tonga.loader().loaderProgress(i + 1, picList.size());
                m.stack = stackmem;
            }
            Tonga.loader().loaderProgress(picList.size(), picList.size());
            Tonga.setStatus("Stack " + (all ? "images" : "image") + " of " + (all ? "all of the images were" : "the current image was") + " exported into PNG format");
        });
        Tonga.bootThread(thread, "Exporter", false, true);
    }

    private static boolean exportImage(TongaImage p, ImageData i) {
        String name = p.imageName + "_[Stack]";
        return exportImage(i.toStreamedImage(), name);
    }

    private static boolean exportImage(TongaImage p, int layer) {
        String name = p.imageName + "_" + p.layerList.get(layer).layerName;
        return exportImage(p.layerList.get(layer).layerImage, name);
    }

    private static boolean exportImage(BufferedImage i, String name) {
        boolean ok = new Exporter() {
            @Override
            void write() throws IOException {
                if (i.getClass() == MappedImage.class) {
                    MappedImage ci = (MappedImage) i;
                    ImageIO.write(ci.bits
                            == 16 ? ci.get8BitCopy() : i, "png", file);
                } else {
                    ImageIO.write(i, "png", file);
                }
            }
        }.exportFile(name, "png");
        return ok;
    }

    public static File exportTable(boolean temp) {
        Tonga.frame().resultHash();
        String fname = "results" + new SimpleDateFormat("ddMMyyyHHmmss").format(new Date());
        File nfile = new File(temp
                ? (Tonga.getTempPath() + fname + ".tsv")
                : Tonga.formatPath(Tonga.frame().filePathField.getText() + "\\" + fname + ".tsv"));
        Thread thread = new Thread(() -> {
            if (!temp) {
                boolean ok = new Exporter() {
                    @Override
                    void write() throws IOException {
                        IO.toTSVfile(Tonga.frame().resultTable, file);
                    }
                }.exportFile(fname, "tsv");
                if (ok) {
                    Tonga.setStatus("Table exported into TSV format");
                    Tonga.log.info("Table exported into TSV format.");
                }
            } else {
                IO.toTSVfile(Tonga.frame().resultTable, nfile);
                launchExcel(nfile);
                Tonga.setStatus("Table exported and fired into Excel");
                Tonga.log.info("Table exported and fired into Excel.");
            }
        });
        Tonga.bootThread(thread, "Exporter", true, true);
        return nfile;
    }

    public static void launchExcel(File file) {
        try {
            switch (Tonga.currentOS()) {
                case WINDOWS:
                    Runtime.getRuntime().exec("cmd /c start excel \"" + file.getAbsolutePath() + "\"");
                    break;
                case MAC:
                    new ProcessBuilder("open", "-a", "Microsoft Excel", file.getAbsolutePath()).start();
                    //exec("open -a \"Microsoft Excel\" \"" + file.getAbsolutePath() + "\"");
                    //exec("open /Applications/Microsoft Excel.app");
                    break;
                case UNKNOWN:
                    Tonga.catchError(new UnsupportedOperationException(), "No excel launcher for this OS.");
                    break;
            }
        } catch (IOException ex) {
            Tonga.catchError(ex, "Excel can not be started.");
        }
    }

    public abstract static class binaryWriter {

        protected abstract void write(DataOutputStream out) throws IOException;

        public boolean save(File f, String desc) {
            try {
                try (DataOutputStream out = new DataOutputStream(new FileOutputStream(f))) {
                    write(out);
                    out.flush();
                }
            } catch (IOException ex) {
                Tonga.catchError(ex, "The " + desc + " could not be saved");
                return false;
            }
            return true;
        }
    }

    public abstract static class binaryReader {

        protected abstract void read(DataInputStream in) throws IOException;

        public boolean load(File f, String desc) {
            if (f.exists()) {
                try {
                    try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
                        read(in);
                    }
                } catch (IOException ex) {
                    Tonga.catchError(ex, "The dialog config file could not be read");
                    return false;
                }
            } else {
                Tonga.log.info("The {} does not exist", desc);
            }
            return true;
        }
    }

    public static void openLogs() {
        File file = new File(Tonga.getAppDataPath() + "tonga.log");
        try {
            switch (Tonga.currentOS()) {
                case WINDOWS:
                    Runtime.getRuntime().exec("notepad \"" + file.getAbsolutePath() + "\"");
                    break;
                case MAC:
                    new ProcessBuilder("open", "-a", "TextEdit", file.getAbsolutePath()).start();
                    break;
                case UNKNOWN:
                    Desktop.getDesktop().open(file);
                    break;
            }
        } catch (IOException ex) {
            Tonga.catchError(ex, "Logs can not be opened.");
        }
    }

    public static void launchURL(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (URISyntaxException | IOException ex) {
            Tonga.catchError(ex, "URI error.");
        }
    }

    public static void waitForJFXRunLater() {
        try {
            Semaphore semaphore = new Semaphore(0);
            Platform.runLater(() -> semaphore.release());
            semaphore.acquire();
        } catch (InterruptedException ex) {
            Tonga.catchError(ex, "You should never see this.");
        }
    }

    private static int getLayerNumber(TongaImage[] images) {
        int counter = 0;
        for (TongaImage image : images) {
            counter += image.layerList.size();
        }
        return counter;
    }

    static String fileName(String name) {
        int pp = name.lastIndexOf(".");
        if (pp >= name.length() - 4 && pp > -1) {
            return name.substring(0, pp);
        } else {
            return name;
        }
    }

    static String legalName(String n) {
        n = n.replaceAll("[^a-zA-Z0-9]", "");
        if (n.isEmpty()) {
            n = "null";
        }
        return n;
    }
}
