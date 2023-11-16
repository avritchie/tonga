package mainPackage;

import java.awt.Color;
import java.util.Objects;
import mainPackage.utils.COL;

public class TongaColor {

    private int value;
    private javafx.scene.paint.Color jfxcolor;
    private Color awtcolor;
    private String name;

    public TongaColor(int value) {
        this.value = value;
        this.jfxcolor = COL.ARGBintToColor(value);
        this.awtcolor = COL.FX2awt(jfxcolor);
        this.name = COL.colorName(value);
    }

    public int getColor() {
        return value;
    }

    public javafx.scene.paint.Color getColorJFX() {
        return jfxcolor;
    }

    public Color getColorAWT() {
        return awtcolor;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + this.value;
        hash = 79 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TongaColor other = (TongaColor) obj;
        if (this.value != other.value) {
            return false;
        }
        return Objects.equals(this.name, other.name);
    }

}
