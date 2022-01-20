package mainPackage.protocols;

import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import static mainPackage.PanelCreator.ControlType.TOGGLE;
import mainPackage.utils.RGB;

/**
 *
 * @author aritchie
 */
public class BreakImageRGB extends Protocol {

    @Override
    public String getName() {
        return "Separate RGBA Channels";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "The source layer"),
            new ControlReference(TOGGLE, "Red", 1),
            new ControlReference(TOGGLE, "Green", 1),
            new ControlReference(TOGGLE, "Blue", 1),
            new ControlReference(TOGGLE, "Alpha", 0),
            new ControlReference(TOGGLE, "Output as brightness", 0)};
    }

    @Override
    protected Processor getProcessor() {
        boolean redT = param.toggle[0];
        boolean greenT = param.toggle[1];
        boolean blueT = param.toggle[2];
        boolean alphaT = param.toggle[3];
        boolean lumiT = param.toggle[4];
        int outPut = 0 + (alphaT ? 1 : 0) + (redT ? 1 : 0) + (greenT ? 1 : 0) + (blueT ? 1 : 0);
        String[] outNam = new String[outPut];
        int i = 0;
        if (redT) {
            outNam[i] = "Channel RED";
            i++;
        }
        if (greenT) {
            outNam[i] = "Channel GREEN";
            i++;
        }
        if (blueT) {
            outNam[i] = "Channel BLUE";
            i++;
        }
        if (alphaT) {
            outNam[i] = "Channel ALPHA";
            i++;
        }

        return new ProcessorFast(outPut, outNam) {

            @Override
            protected void methodCore(int pos
            ) {
                int id = 0, tv = 0;
                if (redT) {
                    tv = inImage[0].pixels32[pos] & 0xFFFF0000;
                    outImage[id].pixels32[pos] = lumiT ? RGB.argb(RGB.brightness(tv)) : tv;
                    id++;
                }
                if (greenT) {
                    tv = inImage[0].pixels32[pos] & 0xFF00FF00;
                    outImage[id].pixels32[pos] = lumiT ? RGB.argb(RGB.brightness(tv)) : tv;
                    id++;
                }
                if (blueT) {
                    tv = inImage[0].pixels32[pos] & 0xFF0000FF;
                    outImage[id].pixels32[pos] = lumiT ? RGB.argb(RGB.brightness(tv)) : tv;
                    id++;
                }
                if (alphaT) {
                    outImage[id].pixels32[pos] = RGB.argb(inImage[0].pixels32[pos] >> 24 & 0xFF);
                    id++;
                }
            }
        };
    }
}
