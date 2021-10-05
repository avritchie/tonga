package mainPackage.filters;

import javafx.scene.effect.BlendMode;
import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.COMBO;
import mainPackage.TongaRender;
import static mainPackage.filters.Filter.noParams;
import mainPackage.utils.IMG;

public class FiltersRender {

    // never use this filter for protocols in any circumstances, use Blender.renderBlend() directly
    public static FilterStack blendStackCurrent() {
        return new FilterStack("Stack", noParams) {

            @Override
            protected ImageData processor(ImageData[] layersarray) {
                return TongaRender.renderImage(layersarray);
            }
        };
    }

    // dont use this filter for protocols, use Blender.renderBlend() directly
    public static FilterStack blendStack() {
        return new FilterStack("Blended", new ControlReference[]{
            new ControlReference(COMBO, new String[]{"Add", "Subtract", "Difference", "Multiply", "Maximum", "Minimum"}, "Rendering method")}) {
            ImageData temp;

            @Override
            protected ImageData processor(ImageData[] layersarray) {
                switch (param.combo[0]) {
                    case 0:
                        processName = "Addition";
                        return Blender.renderBlend(layersarray, Blend.ADD);
                    case 1:
                        processName = "Subtraction";
                        return Blender.renderBlend(layersarray, Blend.SUBTRACT);
                    case 2:
                        processName = "Difference";
                        return Blender.renderBlend(layersarray, Blend.DIFFERENCE);
                    case 3:
                        processName = "Multiplication";
                        return Blender.renderBlend(layersarray, Blend.MULTIPLY);
                    case 4:
                        processName = "Maximum";
                        return Blender.renderBlend(layersarray, Blend.MAXIMUM);
                    case 5:
                        processName = "Minimum";
                        return Blender.renderBlend(layersarray, Blend.MINIMUM);
                }
                return temp;
            }
        };
    }

    // dont use this filter for protocols, use TongaRender.blendImages() directly
    @Deprecated
    public static FilterStack blendStackJFX() {
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
                return new ImageData(TongaRender.blendImages(IMG.datasToImages(layersarray), blend));
            }
        };
    }
}
