package mainPackage;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import javafx.scene.image.Image;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import mainPackage.utils.HISTO;

public class Histogram {

    private static Color histoCol;
    private static JPanel histoPanel;
    private static JLabel histoLabel;
    private static JPanel histoImg;
    private static Integer imageHash = -1;
    private static int[] currentHisto;
    private static long lastStamp;
    private static int pwidth = 0;

    protected static void boot() {
        histoPanel = Tonga.frame().histogramPanel;
        histoLabel = Tonga.frame().histoLabel;
        histoImg = Tonga.frame().histoImg;
        histoCol = histoImg.getBackground();
        Tonga.log.info("Histograms initialized succesfully");
    }

    public static void update() {
        if (pwidth > histoPanel.getWidth()) {
            clearHistogram(Color.white);
        }
        if (Settings.settingBatchProcessing()) {
            clearHistogram(histoCol);
        } else {
            updateHistogram();
        }
    }

    private static void clearHistogram(Color col) {
        SwingUtilities.invokeLater(() -> {
            histoImg.setBackground(col);
            histoLabel.setIcon(null);
        });
    }

    private static void updateHistogram() {
        Thread thread = new Thread(() -> {
            long stamp = System.currentTimeMillis();
            try {
                pwidth = histoPanel.getWidth();
                if (Tonga.getLayerIndex() > -1
                        && TongaRender.renderImages != null && TongaRender.renderImages.length > 0) {
                    Image imgSource = TongaRender.renderImages[Tonga.getLayerIndex()];
                    int imgHash = imgSource.hashCode();
                    int[] renderHisto = currentHisto;
                    if (imageHash != imgHash) {
                        renderHisto = HISTO.getHistogram(imgSource);
                        currentHisto = renderHisto;
                        imageHash = imgHash;
                        Tonga.log.trace("Calculate a histogram for {} by {}", imgHash, stamp);
                    }
                    if (renderHisto != null) {
                        Tonga.log.trace("Render a histogram for {} by {}", imgHash, stamp);
                        BufferedImage currentImage = renderHistogram(renderHisto, histoLabel.getWidth(), histoLabel.getHeight(), (int) (imgSource.getHeight() * imgSource.getWidth()));
                        if (stamp > lastStamp) {
                            lastStamp = stamp;
                            SwingUtilities.invokeLater(() -> {
                                histoLabel.setIcon(new ImageIcon(currentImage));
                            });
                            Tonga.log.trace("Set a histogram for {} by {}", imgHash, stamp);
                        } else {
                            Tonga.log.trace("Discard a histogram for {} by {}", imgHash, stamp);
                        }
                    }
                } else {
                    Tonga.log.trace("No image by {}", stamp);
                    clearHistogram(histoCol);
                }
            } catch (NullPointerException | IndexOutOfBoundsException ex) {
                Tonga.catchError(ex, "Histogram rendering error.");
                clearHistogram(histoCol);
            }
        });
        thread.setName("Histogram");
        thread.start();
    }

    private static BufferedImage renderHistogram(int[] histo, int width, int height, int sourcepixels) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        double widthRelation = width / 256.;
        double heightRelation = height / Math.sqrt(sourcepixels);
        byte[] px = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        int pixels = width * height;
        int pos = 0;
        while (pos < pixels) {
            int x = pos % width, y = height - (pos / width), xp = (int) Math.floor(x / widthRelation);
            px[pos++] = (byte) ((Math.sqrt(histo[(int) (x / widthRelation)]) * heightRelation < y)
                    ? (xp >= Tonga.frame().histoRange.getValue()
                    && xp <= Tonga.frame().histoRange.getUpperValue()) ? 0xFF : 0xF0 : 0x0);
        }
        return img;
    }
}
