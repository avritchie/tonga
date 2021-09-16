package mainPackage.filters;

import javafx.scene.effect.BlendMode;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.COMBO;
import mainPackage.Settings;
import mainPackage.TongaRender;
import static mainPackage.filters.Filter.noParams;

public class FiltersRender {

    // never use this filter for protocols in any circumstances, use TongaRender.blend() directly
    public static FilterStack renderStack() {
        return new FilterStack("Stack", noParams) {

            @Override
            protected ImageData processor(ImageData[] layersarray) {
                return TongaRender.blend(layersarray, Settings.settingBlendMode());
            }
        };
    }

    // dont use this filter for protocols, use TongaRender.blend() directly
    public static FilterStack blendStack() {
        return new FilterStack("Blended", new ControlReference[]{
            new ControlReference(COMBO, new String[]{"Abstract", "Subtract", "Extract", "Multiply"}, "Rendering method")}) {
            @Override
            protected ImageData processor(ImageData[] layersarray) {

                BlendMode blend = null;
                switch (param.combo[0]) {
                    case 0:
                        blend = BlendMode.ADD;
                        processName = "Addition";
                        break;
                    case 1:
                        blend = BlendMode.DIFFERENCE;
                        processName = "Subtraction";
                        break;
                    case 2:
                        blend = BlendMode.EXCLUSION;
                        processName = "Extraction";
                        break;
                    case 3:
                        blend = BlendMode.MULTIPLY;
                        processName = "Multiplication";
                        break;
                }
                return TongaRender.blend(layersarray, blend);
            }
        };
    }
}
