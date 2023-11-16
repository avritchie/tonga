package mainPackage.protocols;

import MRXS.MRXSLevel;
import MRXS.MRXSSlide;
import java.io.File;
import java.nio.file.Files;
import mainPackage.ImageData;
import mainPackage.MappedImage;
import mainPackage.Mirax;
import mainPackage.PanelParams;
import mainPackage.Threader;
import mainPackage.Tonga;
import mainPackage.filters.Filters;
import mainPackage.utils.COL;
import mainPackage.utils.IMG;
import ome.units.quantity.Length;

public abstract class ProcessorMirax extends ProcessorFast {

    MRXSSlide miraxSlide;
    int miraxTileZoom; //tavoitezoom jolle prosessi ajetaan
    int miraxBlockWidth, miraxBlockHeight; //mikä on yhden tilen w ja h esikatselukuvassa
    int miraxPreviewLevel; //mitä leveliä esikatselukuva vastaa
    File miraxPath; //polku jossa mrxs on
    int miraxBlockRatio; //ratio tavoitekuvan ja esikatselukuvan välillä
    int miraxTilesX, miraxTilesY; // montako tilea kuva sisältää
    Object[][] miraxTileValues; //kvantitoidut palautusarvot jokaisesta tilesta
    ImageData miraxArea, miraxTile; //binarisoitu kartta esikatselukuvasta, yksittäisen tilen temp-kuva
    int tileIters;

    public ProcessorMirax(int outputs, String[] output, int iters, int tileiters) {
        super(outputs, output, iters);
        tileIters = tileiters;
    }

    public ProcessorMirax(int outputs, String output, int iters, int tileiters) {
        super(outputs, output, iters);
        tileIters = tileiters;
    }

    public ProcessorMirax(String output, int iters, int tileiters) {
        super(output, iters);
        tileIters = tileiters;
    }

    @Override
    protected void internalParameters(PanelParams pparam) {
        //mirax processor must have folder and zoom level selectors
        miraxTileZoom = pparam.combo[0];
        miraxPath = lparam.folder[0];
    }

    protected Object[] methodCore(int xpos, int ypos, ImageData tile) {
        // no processing needed
        return new Object[]{-1};
    }

    @Override
    protected void methodLoad() {
        try {
            miraxSlide = new MRXSSlide(Tonga.formatPath(miraxPath + "\\Slidedat.ini"));
            miraxPreviewLevel = Mirax.findLevel(miraxSlide, sourceWidth[0], sourceHeight[0]);
            String[] sinfo = miraxSlide.getInfo();
            for (String i : sinfo) {
                Tonga.log.info(i);
            }
            miraxBlockWidth = miraxSlide.tileWidth / (int) Math.pow(2, miraxPreviewLevel - miraxTileZoom);
            miraxBlockHeight = miraxSlide.tileHeight / (int) Math.pow(2, miraxPreviewLevel - miraxTileZoom);
            miraxBlockRatio = miraxSlide.tileWidth / miraxBlockWidth;
            miraxTilesX = (int) Math.ceil(sourceWidth[0] / (double) miraxBlockWidth);
            miraxTilesY = (int) Math.ceil(sourceHeight[0] / (double) miraxBlockHeight);
            miraxTileValues = new Object[miraxTilesX * miraxTilesY][];
            iterations = iterations + tileIters * miraxTilesX * miraxTilesY;
        } catch (Exception ex) {
            Tonga.catchError(ex, "Failed to preprocess the Mirax file.");
        }
    }

    @Override
    protected void processorInit() {
        super.processorInit();
        miraxArea = Filters.thresholdBright().runSingle(inImage[0], 1);
        IMG.fillArray(outImage[0].pixels32, inImage[0].width, inImage[0].height, COL.BLACK);
    }

    @Override
    protected void pixelProcessor() {
        MRXSLevel miraxLevel;
        double fsize = 0;
        try {
            //load mrxs level
            Tonga.log.info("Loading {} as a tileset from level {}", miraxSlide.slideName, miraxTileZoom);
            miraxLevel = new MRXSLevel(miraxSlide, miraxTileZoom, 0);
            String[] linfo = miraxLevel.getInfo();
            for (String i : linfo) {
                Tonga.log.info(i);
            }
            fsize = Math.round(Files.size(miraxLevel.dataFile.toPath()) / 100000.) / 10.;
            byte[] md = miraxLevel.readDataFile();
            //iterate tiles of the level
            new Threader() {
                @Override
                public void action(int tileid) {
                    if (Tonga.loader().getTask().isInterrupted()) {
                        return;
                    }
                    int yt = tileid / miraxTilesX;
                    int xt = tileid % miraxTilesX;
                    try {
                        byte[] tb = miraxLevel.readASingleTile(md, xt, yt);
                        if (tb == null) {
                            Tonga.log.info("Tile x={},y={} does not exist at level {}.", xt, yt, miraxTileZoom);
                        } else {
                            miraxTile = new ImageData(new MappedImage(tb));
                            miraxTileValues[tileid] = methodCore(xt, yt, miraxTile);
                            Tonga.log.info("Tile x={},y={} processed successfully.", xt, yt);
                        }
                    } catch (Exception ex) {
                        Tonga.catchError(ex, "Failed to read the Mirax tile x=" + xt + ",y=" + yt + " at level " + miraxTileZoom + ".");
                    }
                }

                @Override
                public boolean evaluate(int i, int j) {
                    if (!isEmpty(i, j)) {
                        return true;
                    } else {
                        Tonga.iteration(tileIters);
                        Tonga.loader().appendProgress((double) tileIters);
                        return false;
                    }
                }
            }.runMirax(miraxTilesX, miraxTilesY, md);
        } catch (OutOfMemoryError ex) {
            Tonga.catchError(ex, "Not enough memory to open " + miraxSlide.slideName + ", " + fsize + " MB needed but only "
                    + (Math.round(Runtime.getRuntime().maxMemory() / 100000.) / 10.) + " MB available.");
        } catch (Exception ex) {
            Tonga.catchError(ex, "Failed to process the Mirax level " + miraxTileZoom + ".");
        }
    }

    public boolean isEmpty(int xx, int yy) {
        for (int px = 0; px < miraxBlockWidth; px++) {
            for (int py = 0; py < miraxBlockHeight; py++) {
                if (xx * miraxBlockWidth + px < miraxArea.width && yy * miraxBlockHeight + py < miraxArea.height) {
                    int p = xx * miraxBlockWidth + px + ((yy * miraxBlockHeight + py) * miraxArea.width);
                    if (miraxArea.pixels32[p] != COL.BLACK) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Length miraxScaleUnit(Length length) {
        if (length == null) {
            return null;
        } else {
            return new Length(Math.pow(0.5, miraxPreviewLevel - miraxTileZoom) * length.value().doubleValue(), length.unit());
        }
    }
}
