/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mainPackage.counters;

import java.util.ArrayList;
import java.util.HashMap;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.Tonga;
import mainPackage.TongaImage;
import mainPackage.TongaLayer;
import mainPackage.utils.IMG;
import ome.units.quantity.Length;

/**
 *
 * @author aritchie
 */
public class Plot {

    static ImageData id;
    static TableData td;

    public static void xy() {
        Thread thread = new Thread(() -> {
            input(256, 256);
            if (td != null) {
                process();
                output();
            } else {
                Tonga.setStatus("No results.");
            }
        });
        Tonga.loader().allocateLoader(thread, "Plot", false, true);
    }

    private static void input(int width, int height) {
        short[] vals = new short[width * height];
        IMG.fillArray(vals, width, height, (short) 0xFFFF);
        id = new ImageData(vals, width, height, "Plot");
        td = Tonga.frame().resultTable.getData();
    }

    private static void process() {
        HashMap<String, ArrayList<int[]>> map = new HashMap<>();
        for (int r = 0; r < td.rowCount(); r++) {
            String key = ((String) td.getVal(r, 0)).substring(0, 4);
            if (!map.containsKey(key)) {
                map.put(key, new ArrayList<>());
            }
            ArrayList<int[]> al = map.get(key);
            int red = td.getInteger(r, 3);
            int green = td.getInteger(r, 4);
            al.add(new int[]{red, green});
        }
        double[] vals = new double[id.pixels16.length];
        map.entrySet().forEach((es) -> {
            ArrayList<int[]> value = es.getValue();
            int length = value.size();
            for (int b = 0; b < length; b++) {
                int[] rg = value.get(b);
                vals[(id.width - 1) * id.height - rg[0] * id.width + rg[1]] += 1. / length;
            }
        });
        Iterate.pixels(id, (int pos) -> {
            double ov = vals[pos];
            double pv = Math.pow(ov / map.size(), 0.333);
            id.pixels16[pos] = (short) (65535 - Math.ceil(pv * 65535));
        });
    }

    private static void output() {
        TongaLayer tl = new TongaLayer(id);
        TongaImage ti = new TongaImage("Plot", (Length) null);
        ti.addLayer(tl);
        Tonga.injectNewImage(ti);
        Tonga.relistElements("Finished!");
        Tonga.selectImage();
        Tonga.loader().resetProgress();
    }
}
