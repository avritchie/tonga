package mainPackage;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class TongaImage {

    public String imageName;
    public ArrayList<TongaLayer> layerList;
    public int[] activeLayers;
    public boolean stack;

    public TongaImage(File file, String name) throws Exception {
        this(file);
        layerList.add(new TongaLayer(IO.getImageFromFile(file), name));
    }

    public TongaImage(String file, String name) throws Exception {
        this(new File(file));
        layerList.add(new TongaLayer(file, name));
    }

    public TongaImage() {
        this("");
    }

    public TongaImage(File file) {
        this(IO.fileName(file.getName()));
    }

    private TongaImage(String name) {
        imageName = name;
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
}
