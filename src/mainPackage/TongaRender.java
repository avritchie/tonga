package mainPackage;

import mainPackage.utils.IMG;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import loci.formats.FormatTools;
import loci.formats.gui.AWTImageTools;
import static mainPackage.Tonga.picList;
import mainPackage.utils.COL;
import mainPackage.utils.HISTO;
import mainPackage.utils.RGB;

public class TongaRender {

    static Image[] renderImages;
    static int[] renderHashes;
    static boolean zoomFreeze;
    static int mainPHash = 0, zoomPHash = 0;
    public static JFXPanel mainPanel, zoomPanel;
    private static GraphicsContext mainDraw, zoomDraw;
    static double zoomFactor = 2, mainFactor = 1;
    static int[] overlayIndices;
    static ImagePattern diamonds, stripes;
    static int zoomx = 0, zoomy = 0;
    static double posx = 0, posy = 0;
    static int mousx = 0, mousy = 0;
    static int imgx = 0, imgy = 0;
    static int[] imageDimensions;
    static Thread bgRenderThread;

    static void boot() {
        mainPanel = Tonga.frame().panelBig;
        zoomPanel = Tonga.frame().panelSmall;
        overlayIndices = new int[]{0};
        diamonds = new ImagePattern(drawDiamonds(), 0, 0, 20, 20, false);
        stripes = new ImagePattern(drawStripes(), 0, 0, 20, 20, false);
        initZoomPanel();
        initMainPanel();
        redraw();
    }

    public static void redraw() {
        Platform.runLater(() -> {
            TongaImage ti = Tonga.getImage();
            int[] ix = Tonga.getLayerIndexes();
            boolean nulls = ti == null || ix.length == 0;
            if (!picList.isEmpty() && nulls) {
                return;
            }
            imageDimensions = new int[]{mainPanel.getWidth(), mainPanel.getHeight()};
            if (Settings.settingBatchProcessing()) {
                setCanvas();
                fillAndDraw();
            } else {
                imageDimensions = !nulls
                        ? getMaxDim(ti, ix)
                        : imageDimensions;
                setCanvas();
                setDragBounds();
                setZoomPosition();
                fillAndDraw();
            }
        });
    }

    private static void setCanvas() {
        int mainWidth = Math.min(mainPanel.getWidth(),
                (int) (imageDimensions[0] * (picList.isEmpty() ? 1 : mainFactor)));
        int mainHeight = Math.min(mainPanel.getHeight(),
                (int) (imageDimensions[1] * (picList.isEmpty() ? 1 : mainFactor)));
        mainDraw.getCanvas().setWidth(mainWidth);
        mainDraw.getCanvas().setHeight(mainHeight);
        int zoomWidth = zoomPanel.getWidth();
        int zoomHeight = zoomPanel.getHeight();
        zoomDraw.getCanvas().setWidth(zoomWidth);
        zoomDraw.getCanvas().setHeight(zoomHeight);
    }

    private static void setDragBounds() {
        int xlimit = Math.max(0, (int) (imageDimensions[0] * mainFactor - mainPanel.getWidth()));
        int ylimit = Math.max(0, (int) (imageDimensions[1] * mainFactor - mainPanel.getHeight()));
        posx = Math.min(Math.max(0, posx), xlimit / mainFactor);
        posy = Math.min(Math.max(0, posy), ylimit / mainFactor);
    }

    private static void setZoomPosition() {
        int zoomWidth = zoomPanel.getWidth(), zoomHeight = zoomPanel.getHeight();
        int mx, my;
        try {
            Point mp = mainPanel.getMousePosition();
            mx = (int) (mp.x / mainFactor);
            my = (int) (mp.y / mainFactor);
        } catch (NullPointerException ex) {
            mx = 0;
            my = 0;
        }
        if (!zoomFreeze) {
            zoomx = Math.min(Math.max((int) posx + mx - (int) (zoomWidth / 2 / zoomFactor), 0), imageDimensions[0] - (int) (zoomWidth / zoomFactor));
            zoomy = Math.min(Math.max((int) posy + my - (int) (zoomHeight / 2 / zoomFactor), 0), imageDimensions[1] - (int) (zoomHeight / zoomFactor));
        }
    }

    private static void fillAndDraw() {
        int zoomWidth = (int) zoomDraw.getCanvas().getWidth(), zoomHeight = (int) zoomDraw.getCanvas().getHeight();
        int mainWidth = (int) mainDraw.getCanvas().getWidth(), mainHeight = (int) mainDraw.getCanvas().getHeight();
        double zoomLocalFactor = picList.isEmpty() ? 1 : zoomFactor;
        double mainLocalFactor = picList.isEmpty() ? 1 : mainFactor;
        Paint fill = Settings.settingBatchProcessing() ? stripes : Settings.settingsAlphaBackground() ? COL.awt2FX(Settings.settingAlphaBackgroundColor()) : diamonds;
        int newMainPHash = (int) (posx * mainWidth - posy * mainHeight - mainLocalFactor * 100);
        if (mainPHash != newMainPHash) {
            mainPHash = newMainPHash;
            mainDraw.setFill(fill);
            mainDraw.fillRect(0, 0,
                    Math.min(mainWidth, imageDimensions[0] * mainLocalFactor),
                    Math.min(mainHeight, imageDimensions[1] * mainLocalFactor));
            renderGraphics(mainDraw, (int) posx, (int) posy,
                    (int) Math.ceil(mainWidth / mainLocalFactor), (int) Math.ceil(mainHeight / mainLocalFactor),
                    0, 0, mainWidth, mainHeight);
        }
        int newZoomPHash = (int) (zoomx * zoomWidth - zoomy * zoomHeight - zoomLocalFactor * 100);
        if (zoomPHash != newZoomPHash) {
            zoomPHash = newZoomPHash;
            zoomDraw.setFill(fill);
            zoomDraw.fillRect(0, 0, zoomWidth, zoomHeight);
            int izw = (int) (imageDimensions[0] * zoomLocalFactor), izh = (int) (imageDimensions[1] * zoomLocalFactor);
            boolean he = izw < zoomWidth, ve = izh < zoomHeight;
            int zbx = he ? (zoomWidth - izw) / 2 : 0, zby = ve ? (zoomHeight - izh) / 2 : 0;
            int zwx = he ? izw : zoomWidth, zwy = ve ? izh : zoomHeight;
            int zrx = he ? 0 : zoomx, zry = ve ? 0 : zoomy;
            int zux = he ? imageDimensions[0] : (int) (zoomWidth / zoomLocalFactor);
            int zuy = ve ? imageDimensions[1] : (int) (zoomHeight / zoomLocalFactor);
            renderGraphics(zoomDraw, zrx, zry, zux, zuy, zbx, zby, zwx, zwy);
        }
    }

    private static void initZoomPanel() {
        Canvas canvas2 = new Canvas(0, 0);
        zoomDraw = canvas2.getGraphicsContext2D();
        initPanel(zoomDraw, zoomPanel);
    }

    private static void initMainPanel() {
        Canvas canvas2 = new Canvas(0, 0);
        mainDraw = canvas2.getGraphicsContext2D();
        initPanel(mainDraw, mainPanel);
        Tonga.frame().updateZoomLabel(imgx, imgy, mainFactor, zoomFactor);
        mainPanel.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent me) {
                int mx = me.getX(), my = me.getY();
                posx = posx + ((mousx - mx) / mainFactor);
                posy = posy + ((mousy - my) / mainFactor);
                mousx = mx;
                mousy = my;
                redraw();
            }

            @Override
            public void mouseMoved(MouseEvent me) {
                if (Tonga.thereIsImage()) {
                    int mx = me.getX(), my = me.getY();
                    //double mmx = (double) mx / mainPanel.getWidth() * (mainPanel.getWidth() + mainFactor);
                    //double mmy = (double) my / mainPanel.getHeight() * (mainPanel.getHeight() + mainFactor);
                    if (mx <= imageDimensions[0] * mainFactor && my <= imageDimensions[1] * mainFactor) {
                        imgx = (int) (posx + imageDimensions[0] * ((double) mx / (int) (imageDimensions[0] * mainFactor)));
                        imgy = (int) (posy + imageDimensions[1] * ((double) my / (int) (imageDimensions[1] * mainFactor)));
                    }
                    Tonga.frame().updateZoomLabel(imgx, imgy, mainFactor, zoomFactor);
                    redraw();
                }
            }
        });
        mainPanel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    resetZooms();
                }
                zoomFreeze = !zoomFreeze;
                redraw();
            }

            @Override
            public void mousePressed(MouseEvent me) {
                mousx = me.getX();
                mousy = me.getY();
            }

            @Override
            public void mouseReleased(MouseEvent me) {
            }

            @Override
            public void mouseEntered(MouseEvent me) {
            }

            @Override
            public void mouseExited(MouseEvent me) {
            }
        });
        mainPanel.addMouseWheelListener((MouseWheelEvent mwe) -> {
            if (Tonga.getImageIndex() >= 0) {
                if (Key.keyCtrl) {
                    mainFactor = Math.min(Math.max(mainFactor - (mwe.getWheelRotation() * (1 / (10 / mainFactor))), 0.1), 8);
                } else if (!zoomFreeze) {
                    zoomFactor = Math.min(Math.max(zoomFactor - (mwe.getWheelRotation() * (1 / (10 / zoomFactor))), 1), 16);
                }
                redraw();
                Tonga.frame().updateZoomLabel(imgx, imgy, mainFactor, zoomFactor);
            }
        });
    }

    private static void initPanel(GraphicsContext gc, JFXPanel panel) {
        if (Tonga.javaFxVersion >= 12) {
            gc.setImageSmoothing(false);
        }
        Group root2 = new Group();
        root2.getChildren().add(gc.getCanvas());
        Scene scene2 = new Scene(root2);
        scene2.setFill(Color.GRAY);
        panel.setScene(scene2);
    }

    private static void resetZooms() {
        zoomFactor = 2;
        mainFactor = 1;
        redraw();
        Tonga.frame().updateZoomLabel(imgx, imgy, mainFactor, zoomFactor);
    }

    private static Image drawDiamonds() {
        WritableImage img = new WritableImage(20, 20);
        PixelWriter pxls = img.getPixelWriter();
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                if (i > 9) {
                    if (j > 9) {
                        pxls.setColor(i, j, Color.WHITE);
                    } else {
                        pxls.setColor(i, j, Color.WHITESMOKE);
                    }
                } else {
                    if (j > 9) {
                        pxls.setColor(i, j, Color.WHITESMOKE);
                    } else {
                        pxls.setColor(i, j, Color.WHITE);
                    }
                }
            }
        }
        return img;
    }

    private static Image drawStripes() {
        WritableImage img = new WritableImage(20, 20);
        PixelWriter pxls = img.getPixelWriter();
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                if ((i + j) % 5 == 0) {
                    pxls.setColor(i, j, Color.WHITE);
                } else {
                    pxls.setColor(i, j, Color.LIGHTGRAY);
                }
            }
        }
        return img;
    }

    public static void updateRenders() {
        updateRenders(Tonga.getImage());
    }

    static void updateRenders(TongaImage img) {
        if (Settings.settingBatchProcessing()) {
            renderImages = null;
            renderHashes = null;
            return;
        }
        ArrayList<TongaLayer> list = img.layerList;
        Image[] rims = new Image[list.size()];
        int[] rhas = new int[list.size()];
        int[] prior = priorityImages(img);
        HashMap<Integer, CachedImage> rtsk = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            rhas[i] = list.get(i).layerImage.hashCode();
            if (renderHashes == null || i >= renderHashes.length || rhas[i] != renderHashes[i]) {
                rims[i] = null;
                if (prior[i] == 1) {
                    rims[i] = list.get(i).layerImage.getFXImage();
                } else {
                    rtsk.put(i, list.get(i).layerImage);
                }
            } else {
                rims[i] = renderImages[i];
            }
        }
        renderImages = rims;
        renderHashes = rhas;
        invokeBackgroundRendering(rtsk);
    }

    private static void invokeBackgroundRendering(HashMap<Integer, CachedImage> thingsToRender) {
        //background threaded rendering (for images not currently active)
        if (bgRenderThread != null && bgRenderThread.isAlive()) {
            bgRenderThread.interrupt();
        }
        bgRenderThread = new Thread(() -> {
            thingsToRender.entrySet().forEach(imgs -> {
                if (Thread.interrupted()) {
                    //cancel the work
                    return;
                }
                if (renderImages[imgs.getKey()] == null) {
                    renderImages[imgs.getKey()] = imgs.getValue().getFXImage();
                }
            });
        });
        bgRenderThread.setName("Background Renderer");
        bgRenderThread.start();
    }

    private static int[] priorityImages(TongaImage img) {
        // layers which are selected currently (get a list of them to render them first)
        int[] prior = new int[img.layerList.size()];
        if (img.stack) {
            prior = img.layerList.stream().mapToInt(l -> l.isGhost ? 0 : 1).toArray();
        } else {
            for (int i = 0; i < img.activeLayers.length; i++) {
                prior[img.activeLayers[i]] = 1;
            }
        }
        return prior;
    }

    static void resetHash() {
        zoomPHash = 0;
        mainPHash = 0;
    }

    static short[] sliceProjection(BufferedImage[] slcs, int bits) {
        int w = slcs[0].getWidth();
        int h = slcs[0].getHeight();
        int pc = w * h;
        short[] buf = new short[pc];
        short[][][] ss = new short[slcs.length][0][0];
        for (int i = 0; i < slcs.length; i++) {
            ss[i] = AWTImageTools.getShorts(slcs[i]);
        }
        for (int p = 0; p < pc; p++) {
            long v = 0;
            for (int i = 0; i < slcs.length; i++) {
                v += ss[i][0][p] & bits;
            }
            buf[p] = (short) (v / slcs.length);
        }
        return buf;
    }

    static int[] multiplyBits(int[] i, double f) {
        for (int p = 0; p < i.length; p++) {
            i[p] = i[p] * (int) f;
        }
        return i;
    }

    public static int[] getMaxDim() {
        return getMaxDim(Tonga.getImage(), Tonga.getLayerIndexes());
    }

    public static int[] getMaxDim(TongaImage image) {
        return getMaxDim(image, Tonga.getLayerIndexes());
    }

    public static int[] getMaxDim(TongaImage image, int[] indx) {
        List<TongaLayer> pictList = image.layerList;
        TongaImage cimage = Tonga.getImage();
        boolean isthis = cimage == image || cimage == null;
        int lidx = isthis ? indx[0] : 0;
        int width = image.layerList.get(lidx).width;
        try {
            width = (image.stack || !isthis) ? pictList.stream().filter(i -> !i.isGhost).mapToInt(i -> i.width).min().getAsInt() : indx.length > 1 ? Arrays.stream(indx).mapToObj(i -> pictList.get(i)).mapToInt(i -> i.width).min().getAsInt() : width;
        } catch (NoSuchElementException ex) {
            //use the default
        }
        int height = image.layerList.get(lidx).height;
        try {
            height = (image.stack || !isthis) ? pictList.stream().filter(i -> !i.isGhost).mapToInt(i -> i.height).min().getAsInt() : indx.length > 1 ? Arrays.stream(indx).mapToObj(i -> pictList.get(i)).mapToInt(i -> i.height).min().getAsInt() : height;
        } catch (NoSuchElementException ex) {
        }
        return new int[]{width, height};
    }

    public static int[] getMaxDim(Image[] image) {
        int width = Arrays.stream(image).mapToInt(i -> (int) i.getWidth()).min().getAsInt();
        int height = Arrays.stream(image).mapToInt(i -> (int) i.getHeight()).min().getAsInt();
        return new int[]{width, height};
    }

    public static int[] shortToARGB(short[] bb, int colour, int max, int min) {
        int bf = colour & 255;
        int gf = colour >> 8 & 255;
        int rf = colour >> 16 & 255;
        int[] bytes = new int[bb.length];
        int rem = bb.length;
        int s;
        int r;
        int g;
        int b;
        double v;
        double l = max - min == 0 ? 1 : max - min;
        for (int i = 0; i < rem; i++) {
            s = bb[i] & 65535;
            v = min > s ? 0 : (s - min) / l;
            v = v > 1 ? 1 : v;
            b = (int) (v * bf);
            g = (int) (v * gf);
            r = (int) (v * rf);
            bytes[i] = -16777216 | r << 16 | g << 8 | b;
        }
        return bytes;
    }

    static void setDisplayRange(short[] channel, CachedImage image) {
        int[] hist = HISTO.getHistogram(channel);
        int[] p = HISTO.getMinMaxAdapt(hist, Settings.settingAutoscaleAggressive() ? 0.1 : 0);
        image.min = p[0];
        image.max = p[1];
    }

    static void setDisplayRange(int maxChannels, TongaImage[] returnableImages) {
        int[] max = new int[maxChannels];
        int[] min = new int[maxChannels];
        for (int i = 0; i < maxChannels; i++) {
            max[i] = 0;
            min[i] = 65535;
        }
        for (TongaImage ti : returnableImages) {
            for (int j = 0; j < ti.layerList.size(); j++) {
                CachedImage ci = ti.layerList.get(j).layerImage;
                if (ci.bits == 16) {
                    int[] hist = HISTO.getHistogram(ci.getShorts());
                    int[] b = HISTO.getMinMaxAdapt(hist, Settings.settingAutoscaleAggressive() ? 0.1 : 0);
                    min[j] = Math.min(min[j], b[0]);
                    max[j] = Math.max(max[j], b[1]);
                }
            }
        }
        for (int i = 0; i < maxChannels; i++) {
            System.out.println("Common scaling for channel " + i + " will be between " + min[i] + " and " + max[i]);
        }
        for (TongaImage ti : returnableImages) {
            for (int j = 0; j < ti.layerList.size(); j++) {
                CachedImage ci = ti.layerList.get(j).layerImage;
                if (ci.bits == 16) {
                    ci.min = min[j];
                    ci.max = max[j];
                }
            }
        }
    }

    public static CachedImage imageToCache(Image img) {
        return new CachedImage(SwingFXUtils.fromFXImage(img, null));
    }

    public static void renderGraphics(GraphicsContext cont, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
        renderGraphics(cont, Tonga.getImage(), Tonga.getLayerIndexes(), sx, sy, sw, sh, dx, dy, dw, dh);
    }

    public static void renderGraphics(GraphicsContext cont, TongaImage image) {
        int[] ix = Tonga.getLayerIndexes();
        int[] dim = getMaxDim(image, ix);
        renderGraphics(cont, image, ix, 0, 0, dim[0], dim[1], 0, 0, dim[0], dim[1]);
    }

    private static void renderGraphics(GraphicsContext cont, TongaImage image, int[] indices, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
        if (image != null && TongaRender.renderImages != null) {
            cont.setGlobalBlendMode(BlendMode.SRC_OVER);
            boolean stack = image.stack;
            int[] iter = stack ? IntStream.range(0, image.layerList.size()).toArray() : indices;
            Arrays.stream(iter).forEach(i -> {
                TongaLayer layer = image.layerList.get(i);
                if (!stack || !layer.isGhost) {
                    if (TongaRender.renderImages[i] == null) {
                        TongaRender.renderImages[i] = layer.layerImage.getFXImage();
                        System.out.println("This should not happen.");
                    }
                    double alpha = layer.isGhost ? 1.0 / indices.length : 1.0;
                    cont.setGlobalAlpha(alpha);
                    cont.drawImage(TongaRender.renderImages[i], sx, sy, sw, sh, dx, dy, dw, dh);
                    cont.setGlobalAlpha(1.0);
                }
                if (stack && !layer.isGhost) {
                    cont.setGlobalBlendMode(Settings.settingBlendMode());
                }
            });
        }
    }

    public static ImageData renderAsStack(ImageData[] layers) {
        return new ImageData(blendImages(IMG.datasToImages(layers), Settings.settingBlendMode()));
    }

    public static ImageData renderAsStack(ImageData img1, ImageData img2) {
        return renderAsStack(new ImageData[]{img1, img2});
    }

    public static WritableImage renderWithMode(Image img1, Image img2, BlendMode mode) {
        return blendImages(new Image[]{img1, img2}, mode);
    }

    public static ImageData renderWithMode(ImageData img1, ImageData img2, BlendMode mode) {
        return renderWithMode(new ImageData[]{img1, img2}, mode);
    }

    public static ImageData renderWithMode(ImageData[] layers, BlendMode mode) {
        Image[] images = Arrays.stream(layers).map(l -> l.toFXImage()).toArray(Image[]::new);
        return new ImageData(blendImages(images, mode));
    }

    static BufferedImage bitTobit8Color(int[] i, int slices, int bitdivider, int maxvalue, ome.xml.model.primitives.Color c, int w, int h) {
        int cc = c.getRed() << 16 | c.getGreen() << 8 | c.getBlue() | 255 << 24;
        System.out.println("There are " + slices + " slices and the divider is " + bitdivider + ", maxvalue " + maxvalue);
        int div = slices * bitdivider;
        for (int p = 0; p < i.length; p++) {
            i[p] = RGB.argbColored(Math.min(maxvalue * slices, i[p]) / div, cc);
        }
        BufferedImage nb = AWTImageTools.blankImage(w, h, 4, FormatTools.INT8);
        nb.setRGB(0, 0, w, h, i, 0, w);
        return nb;
    }

    static int[] scaleBits(int[] i, int s, double f) {
        for (int p = 0; p < i.length; p++) {
            i[p] = (int) ((i[p] - s) * f);
        }
        return i;
    }

    private static WritableImage blendImages(Image[] layers, BlendMode mode) {
        int[] dim = getMaxDim(layers);
        int width = dim[0];
        int height = dim[1];
        Canvas canvas = new Canvas(width, height);
        GraphicsContext cont = canvas.getGraphicsContext2D();
        cont.setGlobalBlendMode(BlendMode.SRC_OVER);
        Arrays.stream(layers).forEach(layer -> {
            cont.drawImage(layer, 0, 0, width, height, 0, 0, width, height);
            cont.setGlobalBlendMode(mode);
        });
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage[] wi = new WritableImage[1];
        Platform.runLater(() -> wi[0] = canvas.snapshot(params, null));
        IO.waitForRunLater();
        return wi[0];
    }
}
