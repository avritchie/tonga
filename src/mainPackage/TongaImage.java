package mainPackage;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import ome.units.quantity.Length;

public class TongaImage {

    public String imageName;
    public Length imageScaling;
    public ArrayList<TongaLayer> layerList;
    public TongaAnnotations annotations;
    public int[] activeLayers;
    public boolean stack;

    public TongaImage(MappedImage file, String iname, String lname) throws Exception {
        this(IO.fileName(iname), (Length) null);
        layerList.add(new TongaLayer(file, lname));
    }
    
    public TongaImage(File file, String name) throws Exception {
        this(file);
        layerList.add(new TongaLayer(IO.getImageFromFile(file), name));
    }

    public TongaImage(String file, String name) throws Exception {
        this(new File(file));
        layerList.add(new TongaLayer(file, name));
    }

    public TongaImage(File file) {
        this(IO.fileName(file.getName()), (Length) null);
    }

    public TongaImage(String name, Length scale) {
        imageName = name;
        imageScaling = scale;
        layerList = new ArrayList<>();
        activeLayers = new int[]{0};
        stack = false;
    }

    @Override
    public String toString() {
        return imageName;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.imageName);
        hash = 97 * hash + Objects.hashCode(this.layerList);
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
        final TongaImage other = (TongaImage) obj;
        if (!Objects.equals(this.imageName, other.imageName)) {
            return false;
        }
        return Objects.equals(this.layerList, other.layerList);
    }

    protected String description() {
        StringBuilder desc = new StringBuilder();
        desc.append(imageName).append("  |  ");
        desc.append(layerList.size()).append(" layers");
        if (imageScaling != null) {
            desc.append("  |  ").append(Math.round(imageScaling.value().doubleValue() * 10000) / 10000.).append(" ").append(imageScaling.unit().getSymbol()).append(" / px");
        }
        return desc.toString();
    }
}
