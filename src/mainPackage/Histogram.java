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

    private static Integer imageHash = null;
    private static BufferedImage currentImage = null;
    private static Color bgcol = Tonga.frame().histoImg.getBackground();
    private static int[] histo = null;
    private static int pwidth = 0;
    private static long lastStamp;

    public static void update() {
        JPanel histoPanel = Tonga.frame().histogramPanel;
        JLabel histoLabel = Tonga.frame().histoLabel;
        JPanel histoImg = Tonga.frame().histoImg;
        if (pwidth > histoPanel.getWidth()) {
            histoImg.setBackground(Color.white);
            histoLabel.setIcon(null);
        }
        if (Settings.settingBatchProcessing()) {
            histoImg.setBackground(bgcol);
            histoLabel.setIcon(null);
            return;
        }
        Thread thread = new Thread(() -> {
            long stamp = System.currentTimeMillis();
            lastStamp = stamp;
            try {
                pwidth = histoPanel.getWidth();
                Image imgSource = TongaRender.renderImages[Tonga.getLayerIndex()];
                int imgHash = imgSource.hashCode();
                if (imageHash == null || histo == null || imageHash != imgHash) {
                    imageHash = imgHash;
                    histo = HISTO.getHistogram(imgSource);
                }
                SwingUtilities.invokeLater(() -> {
                    int width = histoLabel.getWidth();
                    int height = histoLabel.getHeight();
                    currentImage = renderHistogram(histo, width, height, (int) (imgSource.getHeight() * imgSource.getWidth()));
                    if (stamp == lastStamp) {
                        histoLabel.setIcon(new ImageIcon(currentImage));
                    }
                });
            } catch (NullPointerException | IndexOutOfBoundsException ex) {
                histoImg.setBackground(bgcol);
                histoLabel.setIcon(null);
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
