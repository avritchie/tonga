package mainPackage.protocols;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import mainPackage.ImageData;
import mainPackage.MappingManager;
import mainPackage.PanelControl;
import mainPackage.PanelCreator;
import mainPackage.PanelCreator.ControlReference;
import mainPackage.PanelParams;
import mainPackage.PanelCreator.ControlType;
import mainPackage.PanelUtils;
import mainPackage.Settings;
import mainPackage.Threader;
import mainPackage.Tonga;
import mainPackage.TongaImage;
import mainPackage.TongaLayer;
import mainPackage.counters.Counter;
import mainPackage.counters.TableData;

public abstract class Protocol {

    private boolean doNotChangeButtonColor;
    private boolean fullOutput;
    public ControlReference[] parameters;
    public PanelCreator panelCreator;
    public PanelParams param;
    public TableData results;

    public Protocol() {
        parameters = getParameters();
        param = new PanelParams(parameters);
    }

    protected abstract ControlReference[] getParameters();

    protected abstract Processor getProcessor();

    public abstract String getName();

    public void loadComponents() {
        panelCreator = new PanelCreator(parameters);
        PanelUtils.updateComponents(panelCreator);
        setLayerSelectors();
        setBGColourSelectors();
    }

    public void getParams() {
        param.getFilterParameters(panelCreator);
    }

    protected boolean fullOutput() {
        return Tonga.debug() || fullOutput;
    }

    public static Protocol load(Supplier<Protocol> method) {
        return method.get();
    }

    private void execute(int imageId, Processor[] processors, List<TableData>[] procdatas, int pid) {
        Processor processor = processors[pid];
        ImageData[] processed = processor.internalProcessing();
        injectAsLayers(processed, imageId);
        procdatas[pid] = processor.datas;
        processors[pid] = null;
        MappingManager.trash();
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
            TongaImage sourceImage = Tonga.getImageList().get(imageId);
            TongaLayer[] sourceLayers = getLayers(param.layer, sourceImage);
            processors[all ? imageId : 0] = bootProcessor(sourceImage, sourceLayers);
        }
        int procIterations = Arrays.stream(processors).mapToInt(p -> p.iterations).sum();
        int procOutputImages = processors[0].outputImageNumber;
        //init data storage for result outputs
        List<TableData>[] procdatas = new List[iters];
        //set target value for the progress bar
        Tonga.loader().setIterations(procIterations);
        List<Integer> procImgs = new ArrayList<>();
        //perform the processing for every image
        for (int imageId = startid; imageId < iters + startid; imageId++) {
            if (multithreading) {
                procImgs.add(imageId);
            } else {
                execute(imageId, processors, procdatas, all ? imageId : 0);
            }
        }
        //run multithreaded
        if (multithreading) {
            new Threader() {
                @Override
                public void action(int imageId) {
                    execute(imageId, processors, procdatas, all ? imageId : 0);
                }

                @Override
                public boolean evaluate(int i, int j) {
                    return true;
                }
            }.runThreaded(procImgs, getName());
        }
        //max progress after finishing
        Tonga.loader().maxProgress();
        //publish the datas
        if (!Tonga.loader().hasAborted()) {
            results = collectData(procdatas);
            if (results != null) {
                Counter.setUnit(results);
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
    }

    private Processor bootProcessor(TongaImage sourceImage, Object[] sourceLayers) {
        Processor processor = getProcessor();
        processor.setSources(sourceImage, sourceLayers);
        processor.localParameters(param);
        processor.internalParameters(param);
        processor.methodLoad();
        return processor;
    }

    public final void runSilentTo(TongaImage imageSource, ImageData[] layerSources, ImageData imageDest, Object... parameters) {
        ImageData[] ids = runSilent(imageSource, layerSources, parameters);
        imageDest.pixels32 = ids[0].pixels32;
        imageDest.pixels16 = ids[0].pixels16;
        imageDest.setAttributes(ids[0]);
    }

    public final ImageData[] runSilentFull(TongaImage imageSource, ImageData[] layerSources, Object... parameters) {
        fullOutput = true;
        return runSilent(imageSource, layerSources, parameters);
    }

    public final ImageData[] runSilentFull(TongaImage source, ImageData layerSource, Object... parameters) {
        return runSilentFull(source, new ImageData[]{layerSource}, parameters);
    }

    public final ImageData[] runSilent(TongaImage imageSource, ImageData[] layerSources, Object... parameters) {
        param.setFilterParameters(this.parameters, parameters);
        Processor processor = bootProcessor(imageSource, layerSources);
        ImageData[] processed = processor.internalProcessing();
        results = collectData(processor);
        return processed;
    }

    public final ImageData[] runSilent(TongaImage source, ImageData layerSource, Object... parameters) {
        return runSilent(source, new ImageData[]{layerSource}, parameters);
    }

    public final ImageData[] runSilent(TongaImage source, Image layerSource, Object... parameters) {
        return runSilent(source, new ImageData[]{new ImageData(layerSource)}, parameters);
    }

    private TongaLayer[] getLayers(int[] layerAccess, TongaImage image) {
        TongaLayer[] images = new TongaLayer[layerAccess.length];
        for (int i = 0; i < layerAccess.length; i++) {
            images[i] = image.getLayer(layerAccess[i]);
        }
        return images;
    }

    private void injectAsLayers(ImageData[] processed, int imageId) {
        if (!Thread.currentThread().isInterrupted()) {
            if (Settings.settingBatchProcessing()) {
                for (ImageData image : processed) {
                    String file = Tonga.getLayerList(imageId).get(0).path + "-out-" + image.name.replaceAll("/", "-") + ".png";
                    try {
                        ImageIO.write(image.toStreamedImage(), "png", new File(file));
                    } catch (IOException ex) {
                        Tonga.catchError(ex, "Unable to write the file " + file);
                    }
                    Tonga.injectNewLayer(new TongaLayer(file, image.name), imageId);
                }
            } else {
                for (ImageData image : processed) {
                    Tonga.injectNewLayer(image.toCachedImage(), image.name, imageId);
                }
            }
        }
    }

    private void setLayerSelectors() {
        int howMany = (int) Arrays.stream(parameters).filter(p -> p.type == ControlType.LAYER).count();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].type == ControlType.LAYER) {
                JComboBox<String> combo = (JComboBox) panelCreator.getControls().get(i).comp;
                PanelUtils.updateComboLayerList(combo);
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
                                PanelUtils.updateColorButtonColour(button, combo);
                            }
                        });
                        PanelUtils.updateColorButtonColour(button, combo);
                    } else {
                        Tonga.log.warn("The interaction parameter supplied to COLOUR must be of type LAYER");
                    }
                }
            }
        }
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

    private static TableData collectData(List<TableData>[] procdatas) {
        TableData finalData;
        List<TableData> tdatl = procdatas[0];
        if (!tdatl.isEmpty()) {
            finalData = new TableData(tdatl.get(0).columns, tdatl.get(0).descriptions);
            for (List<TableData> tdat : procdatas) {
                tdat.forEach(d -> {
                    for (int j = 0; j < d.rowCount(); j++) {
                        finalData.newRow(d.rows.get(j));
                    }
                });
            }
        } else {
            return null;
        }
        return finalData;
    }

    private TableData collectData(Processor processor) {
        return collectData(new List[]{processor.datas});
    }
}
