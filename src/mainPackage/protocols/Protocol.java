package mainPackage.protocols;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import mainPackage.utils.COL;
import mainPackage.CachedImage;
import mainPackage.ImageData;
import mainPackage.PanelCreator;
import mainPackage.PanelCreator.ControlReference;
import mainPackage.PanelCreator.ControlType;
import mainPackage.PanelCreator.PanelControl;
import mainPackage.PanelParams;
import mainPackage.Settings;
import mainPackage.Threader;
import mainPackage.Tonga;
import mainPackage.TongaImage;
import mainPackage.TongaLayer;
import mainPackage.counters.Counter;
import mainPackage.counters.TableData;

public abstract class Protocol {

    ArrayList<JComboBox<String>> layerCombos;
    private boolean doNotChangeButtonColor;
    public ControlReference[] parameters;
    public PanelCreator panelCreator;
    public PanelParams param;
    public TableData results;

    public Protocol() {
        layerCombos = new ArrayList<>();
        parameters = getParameters();
        param = new PanelParams(parameters);
    }

    protected abstract ControlReference[] getParameters();

    protected abstract Processor getProcessor();

    public abstract String getName();

    public void loadComponents() {
        panelCreator = new PanelCreator(parameters);
        setLayerSelectors();
        setBGColourSelectors();
    }

    public void getParams() {
        param.getFilterParameters(panelCreator);
    }

    public static Protocol load(Supplier<Protocol> method) {
        return method.get();
    }

    private void execute(int imageId, Processor processor) {
        TongaImage sourceImage = Tonga.getImageList().get(imageId);
        TongaLayer[] sourceLayers = getLayers(param.layer, sourceImage.layerList);
        ImageData[] processed = processor.internalProcessing(sourceImage, sourceLayers);
        injectAsLayers(processed, imageId);
        Runtime.getRuntime().gc();
    }

    public void runProtocol(boolean all) {
        results = null;
        //calculate the images to process
        boolean multithreading = Settings.settingsMultithreading();
        int iters = all ? Tonga.getImageList().size() : 1;
        int startid = all ? 0 : Tonga.frame().imagesList.getSelectedIndex();
        //init processors for every image + general processor vals
        Processor[] processors = new Processor[iters];
        for (int imageId = startid; imageId < iters + startid; imageId++) {
            processors[all ? imageId : 0] = getProcessor();
        }
        int procIterations = processors[0].iterations;
        int procOutputImages = processors[0].outputImageNumber;
        //set target value for the progress bar
        Tonga.loader().setIterations(procIterations * iters);
        List<Integer> procImgs = new ArrayList<>();
        //perform the processing for every image
        for (int imageId = startid; imageId < iters + startid; imageId++) {
            if (multithreading) {
                procImgs.add(imageId);
            } else {
                execute(imageId, processors[all ? imageId : 0]);
            }
        }
        //run multithreaded
        if (multithreading) {
            new Threader() {
                @Override
                public void action(int imageId) {
                    execute(imageId, processors[all ? imageId : 0]);
                }
            }.runThreaded(procImgs, getName());
        }
        //max progress after finishing
        Tonga.loader().maxProgress();
        //publish the datas
        results = collectData(processors);
        if (results != null) {
            Counter.publish(results);
        }
        /*
        if (data != null) {
            Counter.publish(data);
        } else if (!datas.isEmpty()) {
            Counter.publish(datas);
        }
        //reset for the next round
        datas = new ArrayList<>();
        data = null;*/
        //refresh components
        if (procOutputImages > 0) {
            Tonga.publishLayerList();
        }
    }

    public final ImageData[] runSilent(TongaImage imageSource, ImageData[] layerSources, Object... parameters) {
        param.setFilterParameters(this.parameters, parameters);
        Processor processor = getProcessor();
        TongaLayer[] layerDataSources = ImageData.convertToLayers(layerSources);
        ImageData[] processed = processor.internalProcessing(imageSource, layerDataSources);
        results = collectData(processor);
        return processed;
    }

    public final ImageData[] runSilent(TongaImage source, ImageData layerSource, Object... parameters) {
        return runSilent(source, new ImageData[]{layerSource}, parameters);
    }

    public final ImageData[] runSilent(TongaImage source, Image layerSource, Object... parameters) {
        return runSilent(source, new ImageData[]{new ImageData(layerSource)}, parameters);
    }

    private TongaLayer[] getLayers(int[] layerAccess, ArrayList<TongaLayer> layers) {
        TongaLayer[] images = new TongaLayer[layerAccess.length];
        for (int i = 0; i < layerAccess.length; i++) {
            images[i] = layers.get(layerAccess[i]);
        }
        return images;
    }

    private void injectAsLayers(ImageData[] processed, int imageId) {
        if (Settings.settingBatchProcessing()) {
            for (ImageData image : processed) {
                String file = Tonga.getLayerList(imageId).get(0).path + "-out-" + image.name.replaceAll("/", "-") + ".png";
                try {
                    ImageIO.write(image.toImage(), "png", new File(file));
                } catch (IOException ex) {
                    Tonga.catchError(ex, "Unable to write the file " + file);
                }
                Tonga.injectNewLayer(new TongaLayer(file, image.name), imageId);
            }
        } else {
            for (ImageData image : processed) {
                Tonga.injectNewLayer(image.toImage(), image.name, imageId);
            }
        }
    }

    public final void updateComponents() {
        layerCombos.forEach(c -> {
            int i = c.getSelectedIndex();
            updateComboLayerList(c);
            if (i >= 0 && i < c.getItemCount()) {
                c.setSelectedIndex(i);
            } else {
                c.setSelectedIndex(c.getItemCount() - 1);
            }
        });
    }

    private void setLayerSelectors() {
        int howMany = (int) Arrays.stream(parameters).filter(p -> p.type == ControlType.LAYER).count();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].type == ControlType.LAYER) {
                JComboBox<String> combo = (JComboBox) panelCreator.getControls().get(i).comp;
                layerCombos.add(combo);
                updateComboLayerList(combo);
                if (howMany == 1) {
                    combo.setSelectedIndex(Tonga.getLayerIndex());
                } else {
                    combo.setSelectedIndex(i < combo.getItemCount() ? i : 0);
                }
            }
        }
    }

    private void setBGColourSelectors() {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].type == ControlType.COLOUR) {
                PanelControl pc = panelCreator.getControls().get(i);
                if (pc.interaction != null) {
                    PanelControl sc = panelCreator.getControls().get(pc.interaction[0]);
                    JButton button = (JButton) pc.comp;
                    JComboBox<String> combo = (JComboBox) sc.comp;
                    if (sc.type == ControlType.LAYER) {
                        doNotChangeButtonColor = false;
                        button.addActionListener((ActionEvent evt) -> {
                            doNotChangeButtonColor = true;
                        });
                        combo.addActionListener((ActionEvent evt) -> {
                            if (!doNotChangeButtonColor) {
                                updateColorButtonColour(button, combo);
                            }
                        });
                        updateColorButtonColour(button, combo);
                    } else {
                        Tonga.log.warn("The interaction parameter supplied to COLOUR must be of type LAYER");
                    }
                }
            }
        }
    }

    /*
    public final void assignLayerCombo(int controlindex, int select) {
        if (Tonga.thereIsImage()) {
            JComboBox<String> combo = (JComboBox) panelCreator.getControls().get(controlindex).comp;
            layerCombos.add(combo);
            updateComboLayerList(combo);
            if (select < combo.getItemCount()) {
                combo.setSelectedIndex(select);
            } else {
                combo.setSelectedIndex(0);
            }
        }
    }
    public void setColorControlByLayer(int buttonindex, int comboindex) {
        JButton button = (JButton) panelCreator.getControls().get(buttonindex).comp;
        JComboBox<String> combo = (JComboBox) panelCreator.getControls().get(comboindex).comp;
        doNotChangeButtonColor = false;
        button.addActionListener((ActionEvent evt) -> {
            doNotChangeButtonColor = true;
        });
        combo.addActionListener((ActionEvent evt) -> {
            if (!doNotChangeButtonColor) {
                updateColorButtonColour(button, combo);
            }
        });
        updateColorButtonColour(button, combo);
    }*/
    private static void updateColorButtonColour(JButton button, JComboBox combo) {
        Color c;
        if (Tonga.thereIsImage()) {
            CachedImage img = Tonga.getLayerList(Tonga.getImageIndex()).get(combo.getSelectedIndex()).layerImage;
            c = COL.layerCornerColour(img);
        } else {
            c = Color.BLACK;
        }
        button.setBackground(c);
    }

    private static void updateComboLayerList(JComboBox<String> combo) {
        ArrayList<TongaLayer> layers = Tonga.getLayerList();
        int lsize = layers == null ? 0 : layers.size();
        TongaLayer[] list = new TongaLayer[lsize];
        for (int j = 0; j < lsize; j++) {
            list[j] = layers.get(j);
        }
        combo.setModel(new DefaultComboBoxModel(list));
    }

    public boolean checkEqualSize() {
        if (param.layer.length < 2) {
            return true;
        } else {
            int fw = Tonga.getLayerList().get(param.layer[0]).width;
            int fh = Tonga.getLayerList().get(param.layer[0]).height;
            for (int i = 1; i < param.layer.length; i++) {
                if (fw != Tonga.getLayerList().get(param.layer[i]).width
                        || fh != Tonga.getLayerList().get(param.layer[i]).height) {
                    return false;
                }
            }
        }
        return true;
    }

    private static TableData collectData(Processor[] processors) {
        TableData finalData;
        Processor proc = processors[0];
        if (!proc.datas.isEmpty()) {
            finalData = new TableData(proc.datas.get(0).columns);
            for (Processor processor : processors) {
                processor.datas.forEach(d -> {
                    for (int j = 0; j < d.rowCount(); j++) {
                        finalData.newRow(d.rows.get(j));
                    }
                });
            }
        } else if (proc.data != null) {
            finalData = new TableData(proc.data.columns);
            for (Processor processor : processors) {
                for (int j = 0; j < processor.data.rowCount(); j++) {
                    finalData.newRow(processor.data.rows.get(j));
                }
            }
        } else {
            return null;
        }
        return finalData;
    }

    private TableData collectData(Processor processor) {
        return collectData(new Processor[]{processor});
    }
}
