package mainPackage;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;
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
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javax.swing.JPanel;
import loci.formats.gui.AWTImageTools;
import static mainPackage.Tonga.picList;
import mainPackage.utils.COL;
import mainPackage.utils.HISTO;
import mainPackage.utils.RGB;

public class TongaRender {

    static Image[] renderImages;
    static int[] renderHashes;
    static ImageData[] renderCopies;
    static Image renderImage;
    static boolean zoomFreeze;
    static Semaphore redrawSem;
    static int mainPHash = 0, zoomPHash = 0;
    public static JFXPanel mainPanel, zoomPanel;
    private static JPanel actionPanel;
    public static GraphicsContext mainDraw, zoomDraw, mainAnno, zoomAnno;
    static double zoomFactor = 2, mainFactor = 1;
    static ImagePattern diamonds, stripes;
    static Paint fill;
    static int zoomx = 0, zoomy = 0;
    static double posx = 0, posy = 0;
    static int mousx = 0, mousy = 0;
    public static int pointx = 0, pointy = 0;
    static int imgx = 0, imgy = 0;
    static int imgint = 0;
    static int[] zoomC = new int[8];
    static int[] mainC = new int[8];
    static String imgval = "";
    static int[] imageDimensions;
    static double[] dragDimensions;
    static Thread bgRenderThread;

    static void boot() {
        mainPanel = Tonga.frame().panelBig;
        zoomPanel = Tonga.frame().panelSmall;
        actionPanel = Tonga.frame().actionPanel;
        redrawSem = new Semaphore(2);
        diamonds = new ImagePattern(drawDiamonds(), 0, 0, 20, 20, false);
        stripes = new ImagePattern(drawStripes(), 0, 0, 20, 20, false);
        Splash.append("Pattern", 3);
        initZoomPanel();
        Splash.append("Zoom panel", 18);
        initMainPanel();
        Splash.append("Main panel", 9);
        redraw();
        Tonga.log.info("Renderer initialized successfully");
    }

    public static void redraw() {
        if (redrawSem.tryAcquire()) {
            Platform.runLater(() -> {
                TongaImage ti = Tonga.getImage();
                int[] ix = Tonga.getLayerIndexes();
                boolean nulls = ti == null || ix.length == 0;
                boolean bm = Settings.settingBatchProcessing();
                if (picList.isEmpty() || !nulls) {
                    imageDimensions = !bm && !nulls
                            ? getMaxDim(ti, ix)
                            : new int[]{mainPanel.getWidth(), mainPanel.getHeight()};
                    setCanvas();
                    if (!Settings.settingBatchProcessing()) {
                        setDragBounds();
                        setZoomPosition();
                    }
                    try {
                        fillAndDraw();
                    } catch (Exception ex) {
                        Tonga.catchError(ex, "Unable to render the canvas.");
                    }
                }
                redrawSem.release();
            });
        }
    }

    private static void setCanvas() {
        int mainWidth, mainHeight;
        if (Settings.settingBatchProcessing()) {
            mainWidth = mainPanel.getWidth();
            mainHeight = mainPanel.getHeight();
        } else {
            mainWidth = Math.min(mainPanel.getWidth(),
                    (int) (imageDimensions[0] * (picList.isEmpty() ? 1 : mainFactor)));
            mainHeight = Math.min(mainPanel.getHeight(),
                    (int) (imageDimensions[1] * (picList.isEmpty() ? 1 : mainFactor)));
        }
        mainDraw.getCanvas().setWidth(mainWidth);
        mainDraw.getCanvas().setHeight(mainHeight);
        mainAnno.getCanvas().setWidth(mainWidth);
        mainAnno.getCanvas().setHeight(mainHeight);
        int zoomWidth = zoomPanel.getWidth();
        int zoomHeight = zoomPanel.getHeight();
        zoomDraw.getCanvas().setWidth(zoomWidth);
        zoomDraw.getCanvas().setHeight(zoomHeight);
        zoomAnno.getCanvas().setWidth(zoomWidth);
        zoomAnno.getCanvas().setHeight(zoomHeight);
    }

    private static void setDragBounds() {
        double xlimit = Math.max(0, (imageDimensions[0] * mainFactor - mainPanel.getWidth()));
        double ylimit = Math.max(0, (imageDimensions[1] * mainFactor - mainPanel.getHeight()));
        dragDimensions = new double[]{xlimit, ylimit};
        calculatePanelPosition(posx, posy);
    }

    private static void setZoomPosition() {
        Point mp = actionPanel.getMousePosition();
        if (mp != null && !zoomFreeze) {
            int mx = (int) (mp.x / mainFactor);
            int my = (int) (mp.y / mainFactor);
            int zoomWidth = zoomPanel.getWidth(), zoomHeight = zoomPanel.getHeight();
            zoomx = Math.min(Math.max((int) posx + mx - (int) (zoomWidth / 2 / zoomFactor), 0), imageDimensions[0] - (int) (zoomWidth / zoomFactor));
            zoomy = Math.min(Math.max((int) posy + my - (int) (zoomHeight / 2 / zoomFactor), 0), imageDimensions[1] - (int) (zoomHeight / zoomFactor));
        }
    }

    private static void updateMainCoordinates() {
        double mainLocalFactor = picList.isEmpty() ? 1 : mainFactor;
        int mainWidth = Math.min((int) mainDraw.getCanvas().getWidth(), (int) (imageDimensions[0] * mainLocalFactor));
        int mainHeight = Math.min((int) mainDraw.getCanvas().getHeight(), (int) (imageDimensions[1] * mainLocalFactor));
        int miw = (int) Math.round(mainWidth / mainLocalFactor);
        int mih = (int) Math.round(mainHeight / mainLocalFactor);
        //image x start, image y start, image x end, image y end, canvas x start, canvas y start, canvas x end, canvas y end
        mainC = new int[]{(int) posx, (int) posy, miw, mih, 0, 0, mainWidth, mainHeight};
    }

    private static void updateZoomCoordinates() {
        double zoomLocalFactor = picList.isEmpty() ? 1 : zoomFactor;
        int zoomWidth = (int) zoomDraw.getCanvas().getWidth();
        int zoomHeight = (int) zoomDraw.getCanvas().getHeight();
        int izw = (int) (imageDimensions[0] * zoomLocalFactor), izh = (int) (imageDimensions[1] * zoomLocalFactor);
        boolean he = izw < zoomWidth, ve = izh < zoomHeight;
        int zbx = he ? (zoomWidth - izw) / 2 : 0, zby = ve ? (zoomHeight - izh) / 2 : 0;
        int zwx = he ? izw : zoomWidth, zwy = ve ? izh : zoomHeight;
        int zrx = he ? 0 : zoomx, zry = ve ? 0 : zoomy;
        int zux = he ? imageDimensions[0] : (int) (zoomWidth / zoomLocalFactor);
        int zuy = ve ? imageDimensions[1] : (int) (zoomHeight / zoomLocalFactor);
        //image x start, image y start, image x end, image y end, canvas x start, canvas y start, canvas x end, canvas y end
        zoomC = new int[]{zrx, zry, zux, zuy, zbx, zby, zwx, zwy};
    }

    private static void fillAndDraw() {
        updateMainCoordinates();
        updateZoomCoordinates();
        fill = Settings.settingBatchProcessing() ? stripes : Settings.settingsAlphaBackground() ? COL.awt2FX(Settings.settingAlphaBackgroundColor()) : diamonds;
        int newMainPHash = (int) (posx * mainC[6] - posy * mainC[7] - mainFactor * 100);
        int newZoomPHash = (int) (zoomx * zoomC[6] - zoomy * zoomC[7] - zoomFactor * 100);
        int newAnnoHash = TongaAnnotator.getHash();
        boolean mainAnnoRender = false;
        boolean zoomAnnoRender = false;
        if (mainPHash != newMainPHash) {
            mainPHash = newMainPHash;
            fillGraphics(mainDraw, mainC[6], mainC[7]);
            renderGraphics(mainDraw, mainC[0], mainC[1], mainC[2], mainC[3], mainC[4], mainC[5], mainC[6], mainC[7]);
            mainAnnoRender = true;
        }
        if (zoomPHash != newZoomPHash) {
            zoomPHash = newZoomPHash;
            fillGraphics(zoomDraw, (int) zoomDraw.getCanvas().getWidth(), (int) zoomDraw.getCanvas().getHeight());
            renderGraphics(zoomDraw, zoomC[0], zoomC[1], zoomC[2], zoomC[3], zoomC[4], zoomC[5], zoomC[6], zoomC[7]);
            zoomAnnoRender = true;
        }
        if (annoHash != newAnnoHash || TongaAnnotator.annotating() || TongaAnnotator.selection()) { //|| TongaAnnotate.annotationPending() || 
            annoHash = newAnnoHash;
            mainAnnoRender = true;
            zoomAnnoRender = true;
            TongaAnnotator.deselect();
        }
        if (mainAnnoRender) {
            clearGraphics(mainAnno, mainC[6], mainC[7]);
            renderAnnotations(mainAnno, mainC[0], mainC[1], mainC[2], mainC[3], mainC[4], mainC[5], mainC[6], mainC[7]);
        }
        if (zoomAnnoRender) {
            clearGraphics(zoomAnno, (int) zoomDraw.getCanvas().getWidth(), (int) zoomDraw.getCanvas().getHeight());
            renderAnnotations(zoomAnno, zoomC[0], zoomC[1], zoomC[2], zoomC[3], zoomC[4], zoomC[5], zoomC[6], zoomC[7]);
        }
    }

    private static void fillGraphics(GraphicsContext cont, int w, int h) {
        cont.setGlobalBlendMode(BlendMode.SRC_OVER);
        cont.setFill(fill);
        cont.fillRect(0, 0, w, h);
    }

    private static void clearGraphics(GraphicsContext cont, int w, int h) {
        cont.clearRect(0, 0, w, h);
    }

    private static void initZoomPanel() {
        Canvas canvas2 = new Canvas(0, 0);
        zoomDraw = canvas2.getGraphicsContext2D();
        Canvas canvas2a = new Canvas(0, 0);
        zoomAnno = canvas2a.getGraphicsContext2D();
        initPanel(zoomDraw, zoomAnno, zoomPanel);
    }

    private static void initMainPanel() {
        Canvas canvas2 = new Canvas(0, 0);
        mainDraw = canvas2.getGraphicsContext2D();
        Canvas canvas2a = new Canvas(0, 0);
        mainAnno = canvas2a.getGraphicsContext2D();
        initPanel(mainDraw, mainAnno, mainPanel);
        updateLabel();
        mainPanel.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent me) {
                if (!annotationControl()) {
                    int mx = me.getX(), my = me.getY();
                    calculatePanelPosition(posx + ((mousx - mx) / mainFactor), posy + ((mousy - my) / mainFactor));
                    mousx = mx;
                    mousy = my;
                } else {
                    TongaAnnotator.mouseEvent(me, MouseEvent.MOUSE_DRAGGED, imgx, imgy);
                }
                mouseMoved(me);
            }

            @Override
            public void mouseMoved(MouseEvent me) {
                if (Tonga.thereIsImage()) {
                    int mx = me.getX(), my = me.getY();
                    moupx = mx;
                    moupy = my;
                    if (mx <= imageDimensions[0] * mainFactor && my <= imageDimensions[1] * mainFactor) {
                        calculatePixelPosition(mx, my);
                        getPixelColourIntensity(imgx, imgy);
                        if (Tonga.frame().annotationTabOpen()) {
                            Tonga.getImage().annotations.annotationCollision();
                        }
                    }
                    updateLabel();
                    redraw();
                }
            }
        });
        mainPanel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (!annotationControl()) {
                    if (me.getClickCount() == 2) {
                        resetZooms();
                    }
                    zoomFreeze = !zoomFreeze;
                } else {
                    TongaAnnotator.mouseEvent(me, MouseEvent.MOUSE_CLICKED, imgx, imgy);
                }
                redraw();
            }

            @Override
            public void mousePressed(MouseEvent me) {
                if (!annotationControl()) {
                    mousx = me.getX();
                    mousy = me.getY();
                }
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                if (annotationControl()) {
                    TongaAnnotator.mouseEvent(me, MouseEvent.MOUSE_RELEASED, imgx, imgy);
                }
            }

            @Override
            public void mouseEntered(MouseEvent me) {
            }

            @Override
            public void mouseExited(MouseEvent me) {
            }
        });
        mainPanel.addMouseWheelListener((MouseWheelEvent mwe) -> {
            if (!annotationControl()) {
                if (Tonga.getImageIndex() >= 0) {
                    if (Key.keyCtrl) {
                        mainFactor = Math.min(Math.max(mainFactor - (mwe.getWheelRotation() * (1 / (10 / mainFactor))), 0.1), 8);
                        int deltaX = imgx, deltaY = imgy;
                        int mx = mwe.getX(), my = mwe.getY();
                        updateMainCoordinates();
                        calculatePixelPosition(mx, my);
                        posx += deltaX - imgx;
                        posy += deltaY - imgy;
                        setDragBounds();
                        updateMainCoordinates();
                        calculatePixelPosition(mx, my);
                    } else if (!zoomFreeze) {
                        zoomFactor = Math.min(Math.max(zoomFactor - (mwe.getWheelRotation() * (1 / (10 / zoomFactor))), 1), 16);
                    }
                    redraw();
                    updateLabel();
                }
            } else {
                JSpinner js = Tonga.frame().annotRadius;
                int ss = (int) js.getValue();
                int ch = -mwe.getWheelRotation();
                ss += ch * (Key.keyCtrl ? 10 : 1);
                TongaAnnotator.setAnnotationSize(ss);
                js.setValue(ss);
                redraw();
            }
        });
    }

    private static boolean annotationControl() {
        return (TongaAnnotator.annotating() && Tonga.frame().annotationTabOpen() && TongaAnnotator.isVisible() && !Key.keyShift);
    }

    private static void initPanel(GraphicsContext gc, GraphicsContext ac, JFXPanel panel) {
        if (Tonga.javaFxVersion >= 12) {
            gc.setImageSmoothing(false);
        }
        Pane root2 = new Pane(gc.getCanvas(), ac.getCanvas());
        Scene scene2 = new Scene(root2);
        scene2.setFill(Color.GRAY);
        panel.setScene(scene2);
    }

    private static void calculatePixelPosition(int mx, int my) {
        //imgx = Math.min(imageDimensions[0] - 1, (int) (posx + imageDimensions[0] * ((double) mx / (int) (imageDimensions[0] * mainFactor))));
        //imgy = Math.min(imageDimensions[1] - 1, (int) (posy + imageDimensions[1] * ((double) my / (int) (imageDimensions[1] * mainFactor))));
        imgx = Math.min(imageDimensions[0] - 1, (int) (mx / (mainC[6] / (double) mainC[2]) + mainC[0]));
        imgy = Math.min(imageDimensions[1] - 1, (int) (my / (mainC[7] / (double) mainC[3]) + mainC[1]));
    }

    private static void getPixelColourIntensity(int xx, int yy) {
        if (Tonga.thereIsVisibleImage() && xx >= 0 && yy >= 0) {
            PixelReader renderReader = getCurrentRender().getPixelReader();
            int val = renderReader.getArgb(xx, yy);
            imgint = RGB.brightness(val);
            imgval = String.format("%08X", val);
        } else {
            imgint = 0;
            imgval = String.format("--------");
        }
    }

    private static void calculatePanelPosition(double x, double y) {
        posx = Math.min(Math.max(0, x), dragDimensions[0] / mainFactor);
        posy = Math.min(Math.max(0, y), dragDimensions[1] / mainFactor);
    }

    private static void resetZooms() {
        zoomFactor = 2;
        mainFactor = 1;
        redraw();
        updateLabel();
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

    public static void copyFromCache() {
        if (Settings.settingBatchProcessing()) {
            renderImage = null;
            renderImages = null;
            renderCopies = null;
            renderHashes = null;
            return;
        }
        TongaImage img = Tonga.getImage();
        ArrayList<TongaLayer> list = img.layerList;
        int[] rhas = new int[list.size()];
        int[] prior = Tonga.imageAsBinarySelectedLayerArray(img);
        boolean hw = Settings.settingHWAcceleration();
        HashMap<Integer, Object> rtsk = new HashMap<>();
        Image[] rimsi = new Image[list.size()];
        ImageData[] rimsid = new ImageData[list.size()];
        for (int i = 0; i < list.size(); i++) {
            rhas[i] = list.get(i).layerImage.hashCode();
            boolean ev = renderHashes == null || i >= renderHashes.length || rhas[i] != renderHashes[i];
            if (hw) {
                if (ev) {
                    if (prior[i] == 1) {
                        rimsi[i] = list.get(i).layerImage.getFXImage();
                    } else {
                        rtsk.put(i, list.get(i).layerImage);
                    }
                } else {
                    rimsi[i] = renderImages[i];
                }
            } else {
                if (ev) {
                    if (prior[i] == 1) {
                        rimsid[i] = new ImageData(list.get(i));
                    } else {
                        rtsk.put(i, list.get(i));
                    }
                } else {
                    rimsid[i] = renderCopies[i];
                }
            }
        }
        renderImages = rimsi;
        renderCopies = rimsid;
        renderHashes = rhas;
        invokeBackgroundRendering(rtsk);
        if (!hw) {
            setRenderImage();
        }
    }

    public static void setRenderImage() {
        if (Tonga.thereIsVisibleImage()) {
            TongaLayer[] layersToRender;
            if (Tonga.getImage().stack) {
                layersToRender = Tonga.imageLayersFromStack();
            } else {
                layersToRender = Tonga.imageLayersFromIndexList();
            }
            renderImage = renderImage(Tonga.layersAs8BitImageDataArray(layersToRender)).toFXImage();
        } else {
            renderImage = null;
        }
    }

    public static ImageData renderImage(ImageData[] layersarray) {
        if (layersarray.length == 1) {
            return layersarray[0].copy();
        } else if (Tonga.getImage().stack) {
            return Blender.renderBlend(layersarray, Blender.modeBridge(Settings.settingBlendMode()));
        } else {
            return Blender.renderOverlay(layersarray);
        }
    }

    private static void invokeBackgroundRendering(HashMap<Integer, Object> thingsToRender) {
        //background threaded rendering (for images not currently active)
        if (bgRenderThread != null && bgRenderThread.isAlive()) {
            bgRenderThread.interrupt();
        }
        bgRenderThread = new Thread(() -> {
            thingsToRender.entrySet().forEach(imgs -> {
                if (Thread.currentThread().isInterrupted()) {
                    //cancel the work
                    return;
                }
                if (Settings.settingHWAcceleration()) {
                    if (renderImages[imgs.getKey()] == null) {
                        renderImages[imgs.getKey()] = ((MappedImage) imgs.getValue()).getFXImage();
                    }
                } else {
                    if (renderCopies[imgs.getKey()] == null) {
                        renderCopies[imgs.getKey()] = new ImageData((TongaLayer) imgs.getValue());
                    }
                }
            });
        });
        bgRenderThread.setName("Background Renderer");
        bgRenderThread.start();
    }

    static void resetHash() {
        zoomPHash = 0;
        mainPHash = 0;
        annoHash = 0;
    }

    static <T> BufferedImage sliceProjection(BufferedImage[] slcs, int maxval) {
        BufferedImage ref = slcs[0];
        int w = ref.getWidth();
        int h = ref.getHeight();
        int pc = w * h;
        byte[][] bBuf = null; //[channel][pixel]
        short[][] sBuf = null; //[channel][pixel]
        byte[][][] bb; //[slice][channel][pixel]
        short[][][] ss; //[slice][channel][pixel]
        if (maxval > 255) {
            sBuf = new short[1][pc];
            ss = new short[slcs.length][0][0];
            for (int i = 0; i < slcs.length; i++) {
                ss[i] = AWTImageTools.getShorts(slcs[i]);
            }
            for (int c = 0; c < 1; c++) {
                for (int p = 0; p < pc; p++) {
                    long v = 0;
                    for (int i = 0; i < slcs.length; i++) {
                        v += ss[i][c][p] & maxval;
                    }
                    sBuf[c][p] = (short) (v / slcs.length);
                }
            }
        } else {
            bBuf = new byte[1][pc];
            bb = new byte[slcs.length][0][0];
            for (int i = 0; i < slcs.length; i++) {
                bb[i] = AWTImageTools.getBytes(slcs[i]);
            }
            for (int c = 0; c < 1; c++) {
                for (int p = 0; p < pc; p++) {
                    long v = 0;
                    for (int i = 0; i < slcs.length; i++) {
                        v += bb[i][c][p] & maxval;
                    }
                    bBuf[c][p] = (byte) (v / slcs.length);
                }
            }
        }
        BufferedImage bufImg = rebuffer(ref, maxval > 255 ? sBuf[0] : bBuf[0], maxval > 255);
        return bufImg;
    }

    static BufferedImage rebuffer(BufferedImage reference, Object buffer, boolean bits) {
        //set a new buffer for an image while keeping the type and colours untouched
        int w = reference.getWidth();
        int h = reference.getHeight();
        int type = reference.getType();
        ColorModel color = reference.getColorModel();
        BufferedImage bi;
        switch (type) {
            case BufferedImage.TYPE_BYTE_INDEXED: {
                bi = new BufferedImage(w, h, type, (IndexColorModel) color);
                break;
            }
            case BufferedImage.TYPE_CUSTOM: {
                bi = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
                break;
            }
            default: {
                bi = new BufferedImage(w, h, type);
                break;
            }
        }
        DataBuffer rdb = bi.getRaster().getDataBuffer();
        if (bits) {
            short[] sBuf = (short[]) buffer;
            System.arraycopy(sBuf, 0, ((DataBufferUShort) rdb).getData(), 0, sBuf.length);
        } else {
            byte[] bBuf = (byte[]) buffer;
            System.arraycopy(bBuf, 0, ((DataBufferByte) rdb).getData(), 0, bBuf.length);
        }
        return bi;
    }

    static short[][] separate16bit(BufferedImage buf) {
        return AWTImageTools.getShorts(buf);
    }

    static short[] make16bit(BufferedImage buf) {
        return AWTImageTools.getShorts(buf)[0];
    }

    static int[] multiplyBits(int[] i, double f) {
        for (int p = 0; p < i.length; p++) {
            i[p] = i[p] * (int) f;
        }
        return i;
    }

    public static boolean allSameSize(ImageData[] image) {
        boolean wi = Arrays.stream(image).mapToInt(i -> (int) i.width).distinct().count() == 1;
        boolean he = Arrays.stream(image).mapToInt(i -> (int) i.height).distinct().count() == 1;
        return wi && he;
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

    public static int[] getMaxDim(ImageData[] image) {
        int width = Arrays.stream(image).mapToInt(i -> (int) i.width).min().getAsInt();
        int height = Arrays.stream(image).mapToInt(i -> (int) i.height).min().getAsInt();
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

    static void setDisplayRange(short[] channel, MappedImage image) {
        int[] hist = HISTO.getHistogram(channel);
        int[] p = HISTO.getImportScaled(hist);
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
                MappedImage ci = ti.layerList.get(j).layerImage;
                if (ci.bits == 16) {
                    int[] hist = HISTO.getHistogram(ci.getShorts());
                    int[] b = HISTO.getImportScaled(hist);
                    min[j] = Math.min(min[j], b[0]);
                    max[j] = Math.max(max[j], b[1]);
                }
            }
        }
        for (int i = 0; i < maxChannels; i++) {
            Tonga.log.debug("Common scaling for channel {} will be between {} and {}", i, min[i], max[i]);
        }
        for (TongaImage ti : returnableImages) {
            for (int j = 0; j < ti.layerList.size(); j++) {
                MappedImage ci = ti.layerList.get(j).layerImage;
                if (ci.bits == 16) {
                    ci.min = min[j];
                    ci.max = max[j];
                }
            }
        }
    }

    public static MappedImage imageToCache(Image img) {
        return new MappedImage(SwingFXUtils.fromFXImage(img, null));
    }

    public static void renderAnnotations(GraphicsContext cont, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
        TongaAnnotator.renderAnnotations(cont, sx, sy, sw, sh, dx, dy, dw, dh);
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
        if (image != null) {
            if (Settings.settingHWAcceleration()) {
                if (TongaRender.renderImages != null) {
                    cont.setGlobalBlendMode(BlendMode.SRC_OVER);
                    boolean stack = image.stack;
                    int[] iter = stack ? IntStream.range(0, image.layerList.size()).toArray() : indices;
                    Arrays.stream(iter).forEach(i -> {
                        TongaLayer layer = image.layerList.get(i);
                        if (!stack || !layer.isGhost) {
                            if (TongaRender.renderImages[i] == null) {
                                TongaRender.renderImages[i] = layer.layerImage.getFXImage();
                                Tonga.log.warn("Render concurrency violation. This should not happen.");
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
                } else {
                    Tonga.log.warn("Requested a render for an image but the render images were null");
                }
            } else {
                cont.drawImage(renderImage, sx, sy, sw, sh, dx, dy, dw, dh);
            }
        } else {
            //there is nothing to render, so render only background
        }
    }

    @Deprecated
    public static WritableImage blendImages(Image[] layers, BlendMode mode) {
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
        return snapshot(canvas, Color.TRANSPARENT);
    }

    public static WritableImage snapshot(Canvas canvas, Color bg) {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(bg);
        WritableImage[] wi = new WritableImage[1];
        Platform.runLater(() -> wi[0] = canvas.snapshot(params, null));
        IO.waitForJFXRunLater();
        return wi[0];
    }

    static void forceRenderRefresh() {
        renderHashes = null;
    }

    private static void updateLabel() {
        Tonga.frame().updateZoomLabel(imgx, imgy, imgint, imgval, mainFactor, zoomFactor);
    }

    static double getDisplayScaling() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform().getScaleX();
    }

    static Image getCurrentRender() {
        boolean hw = Settings.settingHWAcceleration();
        if (Tonga.thereIsVisibleImage()) {
            if (hw && TongaRender.renderImages != null && TongaRender.renderImages.length > Tonga.selectedLayerIndex()) {
                return TongaRender.renderImages[Tonga.selectedLayerIndex()];
            } else if (!hw) {
                return TongaRender.renderImage;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
