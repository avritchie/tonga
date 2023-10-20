package mainPackage;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import javafx.scene.effect.BlendMode;

public class Settings {

    private static TongaFrame host;
    private static BlendMode blendmode;
    private static Autoscale autoscale;
    private static HashMap<Integer, Boolean> neverShow;

    public enum Autoscale {
        NONE, CHANNEL, IMAGE, FILE;
    }

    protected static void boot() {
        host = Tonga.frame();
        setBlendMode();
        setAutoscale();
        loadConfigFiles();
    }

    public static boolean getNeverShow(int hash) {
        if (neverShow.containsKey(hash)) {
            return true; //neverShow.get(hash);
        } else {
            return false;
        }
    }

    public static void setNeverShow(int hash, boolean selection) {
        neverShow.put(hash, selection);
    }

    public static BlendMode settingBlendMode() {
        return blendmode;
    }

    public static Autoscale settingAutoscaleType() {
        return autoscale;
    }

    public static boolean settingAutoscaleAggressive() {
        return host.boxSettingAutoscale1.isSelected();
    }

    public static boolean settingResultsAppend() {
        return host.boxSettingResultsAppend.isSelected();
    }

    public static boolean settingBatchProcessing() {
        return host.boxSettingBatch.isSelected();
    }

    public static boolean settingOpenAfterExport() {
        return host.boxSettingOpenAfter.isSelected();
    }

    public static boolean settingSubfolders() {
        return host.boxSettingSubfolder.isSelected();
    }

    public static boolean settingsAlphaBackground() {
        return host.boxSettingAlphaBG.isSelected();
    }

    public static Color settingAlphaBackgroundColor() {
        return host.layerBackColor.getBackground();
    }

    public static boolean settingsMultithreading() {
        return host.boxSettingMultiThreading.isSelected();
    }

    public static boolean settingHWAcceleration() {
        return host.boxSettingHWRendering.isSelected();
    }

    public static boolean settingMemoryMapping() {
        return host.boxSettingMMapping.isSelected();
    }

    public static boolean settingsOverrideSizeEstimate() {
        return Tonga.frame().jCheckBoxMenuItem1.isSelected();
    }

    static void setAutoscale() {
        switch (host.autoscaleCombo.getSelectedIndex()) {
            case 0:
                autoscale = Autoscale.NONE;
                break;
            case 1:
                autoscale = Autoscale.FILE;
                break;
            case 2:
                autoscale = Autoscale.CHANNEL;
                break;
            case 3:
                autoscale = Autoscale.IMAGE;
                break;
        }
    }

    static void setBlendMode() {
        BlendMode mode = BlendMode.ADD;
        switch (host.stackCombo.getSelectedIndex()) {
            case 0:
                mode = BlendMode.ADD;
                break;
            case 1:
                mode = BlendMode.DIFFERENCE;
                break;
            case 2:
                mode = BlendMode.MULTIPLY;
                break;
            case 3:
                mode = BlendMode.LIGHTEN;
                break;
            case 4:
                mode = BlendMode.DARKEN;
                break;
        }
        blendmode = mode;
    }

    private static void loadConfigFiles() {
        File file = new File(Tonga.getAppDataPath());
        file.mkdirs();
        File dconf = new File(file + "/dialog.conf");
        File sconf = new File(file + "/settings.conf");
        boolean fail = false;
        neverShow = new HashMap<>();
        fail = fail || new IO.binaryReader() {
            @Override
            public void read(DataInputStream in) throws IOException {
                while (in.available() >= 5) {
                    int key = in.readInt();
                    boolean value = in.readByte() != 0;
                    neverShow.put(key, value);
                }
            }
        }.load(dconf, "dialog config file");
        fail = fail || new IO.binaryReader() {
            @Override
            public void read(DataInputStream in) throws IOException {
                byte gs = in.readByte();
                host.boxSettingAutoscale1.setSelected(((gs) & 1) == 1);
                host.boxSettingResultsAppend.setSelected(((gs >> 1) & 1) == 1);
                host.boxSettingMMapping.setSelected(((gs >> 2) & 1) == 1);
                host.boxSettingOpenAfter.setSelected(((gs >> 3) & 1) == 1);
                host.boxSettingSubfolder.setSelected(((gs >> 4) & 1) == 1);
                host.boxSettingAlphaBG.setSelected(((gs >> 5) & 1) == 1);
                host.boxSettingMultiThreading.setSelected(((gs >> 6) & 1) == 1);
                host.boxSettingHWRendering.setSelected(((gs >> 7) & 1) == 1);
                host.layerBackColor.setBackground(new Color(in.readInt()));
                byte cs = in.readByte();
                host.autoscaleCombo.setSelectedIndex(((cs) & 3));
                host.stackCombo.setSelectedIndex(((cs >> 2) & 7));
            }
        }.load(sconf, "setting file");
        if (!fail) {
            Tonga.log.info("Configuration files loaded successfully");
        }
    }

    public static void saveConfigFiles() throws IOException {
        File file = new File(Tonga.getAppDataPath());
        file.mkdirs();
        File dconf = new File(file + "/dialog.conf");
        File sconf = new File(file + "/settings.conf");
        boolean fail = false;
        fail = fail || new IO.binaryWriter() {
            @Override
            public void write(DataOutputStream out) throws IOException {
                Iterator<Entry<Integer, Boolean>> esi = neverShow.entrySet().iterator();
                while (esi.hasNext()) {
                    Entry es = esi.next();
                    int key = (Integer) es.getKey();
                    boolean value = (Boolean) es.getValue();
                    out.writeInt(key);
                    out.writeByte(value ? 1 : 0);
                }
            }
        }.save(dconf, "dialog config file");
        fail = fail || new IO.binaryWriter() {
            @Override
            public void write(DataOutputStream out) throws IOException {
                byte gs = (byte) ((host.boxSettingAutoscale1.isSelected() ? 1 : 0)
                        | (host.boxSettingResultsAppend.isSelected() ? 1 : 0) << 1
                        | (host.boxSettingMMapping.isSelected() ? 1 : 0) << 2
                        | (host.boxSettingOpenAfter.isSelected() ? 1 : 0) << 3
                        | (host.boxSettingSubfolder.isSelected() ? 1 : 0) << 4
                        | (host.boxSettingAlphaBG.isSelected() ? 1 : 0) << 5
                        | (host.boxSettingMultiThreading.isSelected() ? 1 : 0) << 6
                        | (host.boxSettingHWRendering.isSelected() ? 1 : 0) << 7);
                out.writeByte(gs);
                out.writeInt(host.layerBackColor.getBackground().getRGB());
                byte cs = (byte) (host.autoscaleCombo.getSelectedIndex()
                        | host.stackCombo.getSelectedIndex() << 2);
                out.writeByte(cs);
            }
        }.save(sconf, "setting file");
        if (!fail) {
            Tonga.log.info("Configuration files saved successfully");
        }
    }
}
