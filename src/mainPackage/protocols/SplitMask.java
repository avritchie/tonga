package mainPackage.protocols;

import java.util.Arrays;
import java.util.HashMap;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.COLOUR;
import static mainPackage.PanelCreator.ControlType.LAYER;
import static mainPackage.PanelCreator.ControlType.SPINNER;
import static mainPackage.PanelCreator.ControlType.TOGGLE;

public class SplitMask extends Protocol {

    @Override
    public String getName() {
        return "Copy areas";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "The mask is which layer"),
            new ControlReference(SPINNER, "Number of colors", 3),
            new ControlReference(TOGGLE, "Fill the background with color", 0, new int[]{3, 1}),
            new ControlReference(COLOUR, "The mask is masked with which colour")};
    }

    @Override
    protected Processor getProcessor() {
        int colors = param.spinner[0];

        return new ProcessorFast(colors, "Split", 1) {
            int dindex;
            HashMap<Integer, Integer> cmap;

            @Override
            protected void methodInit() {
                dindex = 0;
                cmap = new HashMap<>();
                if (param.toggle[0]) {
                    for (int i = 0; i < colors; i++) {
                        Arrays.fill(outImage[i].pixels32, param.colorARGB[0]);
                    }
                }
            }

            @Override
            protected void methodCore(int pos) {
                int thisCol = inImage[0].pixels32[pos];
                if (!param.toggle[0] || thisCol != param.colorARGB[0]) {
                    Integer cindex = cmap.get(thisCol);
                    if (cindex == null) {
                        cindex = dindex;
                        if (dindex < colors - 1) {
                            cmap.put(thisCol, dindex);
                            dindex++;
                        }
                    }
                    outImage[cindex].pixels32[pos] = thisCol;
                }
            }
        };
    }
}
