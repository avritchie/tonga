package mainPackage;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.swing.SwingUtilities;
import static mainPackage.TongaAnnotator.update;
import mainPackage.counters.TableData;
import mainPackage.utils.GEO;

public class TongaAnnotations {

    private List<TongaAnnotation> annos;

    public TongaAnnotations() {
        annos = new ArrayList<>();
    }

    public TongaAnnotations(List<TongaAnnotation> anno) {
        annos = new ArrayList<>();
        annos.addAll(anno);
    }

    public List<TongaAnnotation> getAnnotations() {
        return annos;
    }

    public void setAnnotations(TongaAnnotations anno) {
        annos = anno.getAnnotations();
    }

    public boolean isEmpty() {
        return annos.isEmpty();
    }

    protected void removeAll() {
        annos = new ArrayList<>();
    }

    void addAll(List<TongaAnnotation> all) {
        annos.addAll(all);
    }

    void add(TongaAnnotation anno) {
        annos.add(anno);
    }

    protected void remove(TongaAnnotation o) {
        annos.remove(o);
    }

    protected void remove(int i) {
        annos.remove(i);
    }

    protected void remove(int[] i) {
        for (int a = i.length - 1; a >= 0; a--) {
            annos.remove(i[a]);
        }
    }

    protected void removeHovered() {
        UndoRedo.start();
        boolean modif = false;
        Map<TongaAnnotation, Double> annoDists = new HashMap<>();
        for (TongaAnnotation anno : annos) {
            if (anno.hover) {
                annoDists.put(anno, GEO.getDist(anno.points.get(0), new Point(TongaRender.mousx, TongaRender.mousy)));
                modif = true;
            }
        }
        Optional o = annoDists.entrySet().stream().sorted(Map.Entry.comparingByValue()).findFirst();
        if (!o.isEmpty()) {
            annos.remove(((Optional<Map.Entry<TongaAnnotation, Double>>) o).get().getKey());
        }
        if (modif) {
            update();
        }
        UndoRedo.end();
    }

    protected void annotationCollision() {
        int mousx = TongaRender.imgx;
        int mousy = TongaRender.imgy;
        synchronized (annos) {
            for (TongaAnnotation anno : annos) {
                if (anno.getShape() != null) {
                    boolean hv = anno.hover;
                    anno.hover = anno.getShape().contains(mousx, mousy);
                    if (hv != anno.hover) {
                        TongaAnnotator.select();
                    }
                }
            }
        }
        if (TongaAnnotator.selection()) {
            TongaRender.redraw();
        }
    }

    protected void tableAnnotations() {
        TongaAnnotationTable tat = Tonga.frame().annotationTable;
        Object[][] newRows = new Object[annos.size()][];
        for (int i = 0; i < annos.size(); i++) {
            TongaAnnotation anno = annos.get(i);
            newRows[i] = new Object[]{
                i + 1,
                anno.getType().name(),
                anno.points.get(0).x,
                anno.points.get(0).y,
                anno.getWidth(),
                anno.getHeight(),
                anno.getArea(),
                anno.getNodes(),
                anno.getLength(),
                anno.getAngle(),
                anno.getGroup(),
                anno.getColorObject()
            };
        }
        if (!tat.isData()) {
            TableData newData = tat.newData();
            newData.newRows(newRows);
            tat.overwriteData(newData);
        } else {
            SwingUtilities.invokeLater(() -> {
                tat.deleteAllRows();
                tat.addRows(newRows);
            });
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + this.annos.stream().mapToInt(a -> a.hashCode()).sum();
        return hash;
    }

    @Override
    public boolean equals(Object obj
    ) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TongaAnnotations other = (TongaAnnotations) obj;
        return Objects.equals(getAnnotations(), other.getAnnotations());
    }
}
