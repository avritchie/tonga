/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mainPackage.morphology;

import mainPackage.ImageData;
import mainPackage.Tonga;

public class Cell extends ROI {

    // alla olevat arvot ylikirjoitetaan tarvittaessa/käytettäessä
    private int cellEstimate;

    public Cell(ROI roi) {
        super(roi.originalImage, roi.area);
    }

    public Cell(ImageData id, Area a) {
        super(id, a);
    }

    public int getCellEstimate() {
        if (cellEstimate == 0) {
            Tonga.log.warn("Cell estimations must be invoked from the set");
        }
        return cellEstimate;
    }

    protected void setCellEstimate(int estimate) {
        cellEstimate = estimate;
    }

}
