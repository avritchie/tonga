package mainPackage.protocols;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;
import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import mainPackage.Tonga;
import mainPackage.filters.Filters;
import mainPackage.morphology.ROI;
import mainPackage.morphology.ROISet;
import mainPackage.morphology.ImageTracer;
import mainPackage.utils.GEO;

public class _EstimateNucleusSize extends Protocol {

    @Override
    public String getName() {
        return "Nucleus size";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Image with nucleis is at which layer")};
    }

    @Override
    protected Processor getProcessor() {
        return new ProcessorFast(0, new String[]{"Nucleus Size"}, 7) {

            ImageData layer, layer2;

            @Override
            protected void pixelProcessor() {
                initTableData(new String[]{"Image", "Ø", "Size"});
                //guess the size of the cells
                boolean finished = false;
                int nucleusSize = -1;
                double mp = 1.0;
                int trs = 0;
                while (!finished) {
                    Tonga.log.trace("An attempt to recognize nucleus size");
                    layer = Filters.dog().runSingle(sourceLayer[0], (int) (3 * mp), (int) (60 * mp));
                    layer2 = Filters.thresholdBright().runSingle(layer, 5);
                    layer = Filters.thresholdBright().runSingle(layer, 2);
                    ROISet set = new ImageTracer(layer2, COL.BLACK).trace();
                    set = set.getPositionFilteredSet(layer, COL.BLACK, false);
                    trs++;
                    if (mp < 0.3 || mp > 4 || trs > 5) {
                        Tonga.log.trace("We could not find the size. Stuck in the loop.");
                        finished = true;
                    } else if ((set.statsForTotalSize().getStdDev() / set.statsForTotalSize().getMedian())
                            / (set.totalAreaSize() / set.statsForTotalSize().getStdDev()) > 1) {
                        mp *= 0.67;
                        Tonga.loader().stopAppending();
                        Tonga.log.trace("Too big - going smaller");
                    } else if ((set.avgSize() / (double) set.totalAreaSize()) < 0.02 && set.statsForTotalSize().getMMRatio() / set.statsForTotalSize().getMedian() > 1) {
                        mp *= 1.33;
                        Tonga.loader().stopAppending();
                        Tonga.log.trace("Too small - going bigger");
                    } else {
                        Tonga.log.trace("Current filtering is fine");
                        ArrayList<ROI> rcs = new ArrayList<>();
                        //only consider round objects - try to identify clear "normal" nuclei
                        double circ = 0.95;
                        while (rcs.isEmpty() && circ >= 0.8) {
                            for (int i = 0; i < set.list.size(); i++) {
                                ROI nn = set.list.get(i);
                                if (nn.getCircularity() > circ) {
                                    rcs.add(nn);
                                }
                            }
                            if (rcs.isEmpty()) {
                                Tonga.log.trace("There are no circular objects with circ={}", circ);
                                circ -= 0.05;
                            }
                        }
                        if (!rcs.isEmpty()) {
                            //original size before filtering
                            int brc = rcs.size();
                            //filter outliers by size (too big or too small from the avg)
                            double avg = rcs.stream().mapToInt(r -> r.getSize()).average().getAsDouble();
                            Iterator<ROI> it = rcs.iterator();
                            while (it.hasNext()) {
                                ROI rr = it.next();
                                if ((avg / rr.getSize()) > 2 || (rr.getSize() / avg) > 2) {
                                    it.remove();
                                }
                            }
                            //if there is something left
                            if (!rcs.isEmpty()) {
                                //and something has been removed -> need to recalculate
                                if (brc != rcs.size()) {
                                    avg = rcs.stream().mapToInt(r -> r.getSize()).average().getAsDouble();
                                }
                                //smallest and largest remaining objects
                                int min = rcs.stream().mapToInt(r -> r.getSize()).min().getAsInt();
                                int max = rcs.stream().mapToInt(r -> r.getSize()).max().getAsInt();
                                Tonga.log.trace("Objects: n={}, avg={}, min={}, max={}", rcs.size(), avg, min, max);
                                int estimate = (int) rcs.stream().mapToDouble(r -> (r.getWidth() + r.getHeight()) / 2).average().getAsDouble();
                                Tonga.log.trace("Diameter estimate: {}", estimate);
                                if ((rcs.size() > 4 && avg / min < 5 && max / avg < 5) || (rcs.size() > 2 && avg / min < 2 && max / avg < 2)) {
                                    nucleusSize = estimate;
                                    Tonga.log.trace("Normal rounds");
                                    finished = true;
                                } else if (rcs.size() > 4) {
                                    avg = rcs.stream().mapToInt(r -> r.getSize()).average().getAsDouble();
                                    int mnlim = (int) (avg / (double) 3);
                                    int mxlim = (int) avg * 3;
                                    try {
                                        nucleusSize = (int) rcs.stream()
                                                .filter(r -> r.getSize() > mnlim && r.getSize() > mxlim)
                                                .mapToDouble(r -> (r.getWidth() + r.getHeight()) / 2)
                                                .average().getAsDouble();
                                        Tonga.log.trace("Outliers removed");
                                        finished = true;
                                    } catch (Exception ex) {
                                        ROI r = rcs.get(rcs.size() / 2);
                                        nucleusSize = (r.getWidth() + r.getHeight()) / 2;
                                        Tonga.log.trace("Large distribution");
                                        finished = true;
                                    }
                                } else if (!rcs.isEmpty()) {
                                    ROI r = rcs.get(rcs.size() / 2);
                                    nucleusSize = (r.getWidth() + r.getHeight()) / 2;
                                    Tonga.log.trace("Small amount");
                                    finished = true;
                                }
                            }
                            if (!finished) {
                                Tonga.log.trace("We could not find the size. No objects. Approximating from image size.");
                                nucleusSize = (int) (sourceWidth[0] / 10.);
                                finished = true;
                            }
                        } else {
                            Tonga.log.trace("We could not find the size. There are no circular objects. Approximating from image size.");
                            nucleusSize = (int) (sourceWidth[0] / 10.);
                            finished = true;
                        }
                    }
                    Tonga.log.trace("Blending parameter: {}", mp);
                }
                Tonga.loader().continueAppending();
                Object[] newRow = data.newRow(sourceImage.imageName);
                newRow[1] = (Integer) nucleusSize;
                newRow[2] = (Integer) GEO.circleArea(nucleusSize);
                Tonga.log.debug("Estimated nucleus diameter is {} pixels", nucleusSize);
            }
        };
    }
}
