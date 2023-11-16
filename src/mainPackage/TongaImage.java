package mainPackage;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import ome.units.quantity.Length;

public class TongaImage {

    public String imageName;
    public Length imageScaling;
    private ArrayList<TongaLayer> layerList;
    private Supplier<Stream<TongaLayer>> streamSupplier;
    public TongaAnnotations annotations;
    public int[] activeLayers;
    public boolean stack;

    public TongaImage(MappedImage file, String iname, String lname) throws Exception {
        this(IO.fileName(iname), file.scale);
        addLayer(new TongaLayer(file, lname));
    }

    public TongaImage(File file, String name) throws Exception {
        this(file);
        addLayer(new TongaLayer(IO.getImageFromFile(file), name));
    }

    public TongaImage(String file, String name) throws Exception {
        this(new File(file));
        addLayer(new TongaLayer(file, name));
    }

    public TongaImage(File file) {
        this(IO.fileName(file.getName()), (Length) null);
    }

    public TongaImage(String name, Length scale) {
        imageName = name;
        imageScaling = scale;
        layerList = new ArrayList<>();
        streamSupplier = () -> layerList.stream();
        activeLayers = new int[]{0};
        stack = false;
        annotations = new TongaAnnotations();
    }

    public final void addLayer(TongaLayer tongaLayer) {
        layerList.add(tongaLayer);
    }

    public final void addLayer(int position, TongaLayer tongaLayer) {
        layerList.add(position, tongaLayer);
    }

    public final TongaLayer getLayer(int index) {
        return layerList.get(index);
    }

    public final void removeLayer(int index) {
        layerList.remove(index);
    }

    public final int layerCount() {
        return layerList.size();
    }

    public final boolean noLayers() {
        return layerList.isEmpty();
    }

    public final int getLayerIndex(TongaLayer layer) {
        return layerList.indexOf(layer);
    }

    public final Stream<TongaLayer> getLayerStream() {
        return streamSupplier.get();
    }

    public ArrayList<TongaLayer> getLayerList() {
        return layerList;
    }

    protected String description() {
        StringBuilder desc = new StringBuilder();
        desc.append(imageName).append("  |  ");
        desc.append(layerCount()).append(" layers");
        if (imageScaling != null) {
            desc.append("  |  ").append(Math.round(imageScaling.value().doubleValue() * 10000) / 10000.).append(" ").append(imageScaling.unit().getSymbol()).append(" / px");
        }
        return desc.toString();
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
}
