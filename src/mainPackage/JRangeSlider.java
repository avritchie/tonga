package mainPackage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.basic.BasicSliderUI;
import static javax.swing.plaf.basic.BasicSliderUI.NEGATIVE_SCROLL;
import static javax.swing.plaf.basic.BasicSliderUI.POSITIVE_SCROLL;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthSliderUI;

public class JRangeSlider extends JSlider {

    public JRangeSlider() {
        initSlider();
    }

    public JRangeSlider(int min, int max) {
        super(min, max);
        initSlider();
    }

    private void initSlider() {
        setOrientation(HORIZONTAL);
    }

    @Override
    public void updateUI() {
        setUI(new RangeSliderUI(this));
        updateLabelUIs();
    }

    @Override
    public int getValue() {
        return super.getValue();
    }

    @Override
    public void setValue(int value) {
        int oldValue = getValue();
        if (oldValue == value) {
            return;
        }

        // Compute new value and extent to maintain upper value.
        int oldExtent = getExtent();
        int newValue = Math.min(Math.max(getMinimum(), value), oldValue + oldExtent);
        int newExtent = oldExtent + oldValue - newValue;

        // Set new value and extent, and fire a single change event.
        getModel().setRangeProperties(newValue, newExtent, getMinimum(),
                getMaximum(), getValueIsAdjusting());
    }

    public int getUpperValue() {
        return getValue() + getExtent();
    }

    public void setUpperValue(int value) {
        // Compute new extent.
        int lowerValue = getValue();
        int newExtent = Math.min(Math.max(0, value - lowerValue), getMaximum() - lowerValue);

        // Set extent to set upper value.
        setExtent(newExtent);
    }

    class RangeSliderUI extends SynthSliderUI {

        private Rectangle upperThumbRect;
        private boolean upperThumbSelected;
        private boolean upperHover;
        private boolean lowerHover;
        private transient boolean lowerDragging;
        private transient boolean upperDragging;

        /**
         * Constructs a RangeSliderUI for the specified slider component.
         *
         * @param b RangeSlider
         */
        public RangeSliderUI(JRangeSlider b) {
            super(b);
            SliderUI a = ((SliderUI) UIManager.getUI(b));
        }

        public int getLowerX() {
            return thumbRect.x + (thumbRect.width / 2);
        }

        public int getUpperX() {
            return upperThumbRect.x + (upperThumbRect.width / 2);
        }

        /**
         * Installs this UI delegate on the specified component.
         */
        @Override
        public void installUI(JComponent c) {
            upperThumbRect = new Rectangle();
            super.installUI(c);
        }

        /**
         * Creates a listener to handle track events in the specified slider.
         */
        @Override
        protected BasicSliderUI.TrackListener createTrackListener(JSlider slider) {
            return new RangeTrackListener();
        }

        /**
         * Creates a listener to handle change events in the specified slider.
         */
        @Override
        protected ChangeListener createChangeListener(JSlider slider) {
            return new ChangeHandler();
        }

        @Override
        protected void calculateThumbSize() {
            super.calculateThumbSize();
            Dimension size = getThumbSize();
            upperThumbRect.setSize(size.width, size.height);
        }

        /**
         * Updates the locations for both thumbs.
         */
        @Override
        protected void calculateThumbLocation() {
            // Call superclass method for lower thumb location.
            super.calculateThumbLocation();

            // Adjust upper value to snap to ticks if necessary.
            if (slider.getSnapToTicks()) {
                int upperValue = slider.getValue() + slider.getExtent();
                int snappedValue = upperValue;
                int majorTickSpacing = slider.getMajorTickSpacing();
                int minorTickSpacing = slider.getMinorTickSpacing();
                int tickSpacing = 0;

                if (minorTickSpacing > 0) {
                    tickSpacing = minorTickSpacing;
                } else if (majorTickSpacing > 0) {
                    tickSpacing = majorTickSpacing;
                }

                if (tickSpacing != 0) {
                    // If it's not on a tick, change the value
                    if ((upperValue - slider.getMinimum()) % tickSpacing != 0) {
                        float temp = (float) (upperValue - slider.getMinimum()) / (float) tickSpacing;
                        int whichTick = Math.round(temp);
                        snappedValue = slider.getMinimum() + (whichTick * tickSpacing);
                    }

                    if (snappedValue != upperValue) {
                        slider.setExtent(snappedValue - slider.getValue());
                    }
                }
            }

            // Calculate upper thumb location.  The thumb is centered over its 
            // value on the track.
            if (slider.getOrientation() == JSlider.HORIZONTAL) {
                int upperPosition = xPositionForValue(slider.getValue() + slider.getExtent());
                upperThumbRect.x = upperPosition - (upperThumbRect.width / 2);
                upperThumbRect.y = trackRect.y;

            } else {
                int upperPosition = yPositionForValue(slider.getValue() + slider.getExtent());
                upperThumbRect.x = trackRect.x;
                upperThumbRect.y = upperPosition - (upperThumbRect.height / 2);
            }
        }

        /**
         * Paints the slider. The selected thumb is always painted on top of the
         * other thumb.
         */
        @Override
        public void paint(SynthContext context, Graphics g) {
            super.paint(context, g);
            Rectangle lowerKnobBounds = thumbRect;
            thumbRect = upperThumbRect;
            super.paint(context, g);
            thumbRect = lowerKnobBounds;
        }

        @Override
        protected void paintThumb(SynthContext context, Graphics g, Rectangle thumbBounds) {
            // 1 = off non-focus
            // 2 = hover non-focus
            // 4 = press non-focus
            // 257 = off focus
            // 258 = hover focus
            // 260 = press focus
            int vv = context.getComponentState();
            if ((lowerDragging && thumbBounds != upperThumbRect) || (upperDragging && thumbBounds == upperThumbRect)) {
                vv += 2;
            }
            if ((lowerHover && thumbBounds != upperThumbRect) || (upperHover && thumbBounds == upperThumbRect)) {
                vv += 1;
            }
            super.paintThumb(new SynthContext(context.getComponent(), context.getRegion(), context.getStyle(), vv), g, thumbBounds);
        }

        @Override
        protected void paintTrack(SynthContext context, Graphics g,
                Rectangle trackBounds) {
            if (thumbRect != upperThumbRect) {
                super.paintTrack(context, g, trackBounds);
                g.setColor(new Color(10, 10, 10, 50));
                int lowerX = thumbRect.x + (thumbRect.width / 2);
                int upperX = upperThumbRect.x + (upperThumbRect.width / 2);
                for (int i = 0; i < 5; i++) {
                    g.drawLine(lowerX, thumbRect.y + 6 + i, upperX, thumbRect.y + 6 + i);
                }
            }
        }

        /**
         * Sets the location of the upper thumb, and repaints the slider. This
         * is called when the upper thumb is dragged to repaint the slider. The
         * <code>setThumbLocation()</code> method performs the same task for the
         * lower thumb.
         */
        private void setUpperThumbLocation(int x, int y) {
            Rectangle upperUnionRect = new Rectangle();
            upperUnionRect.setBounds(upperThumbRect);

            upperThumbRect.setLocation(x, y);

            SwingUtilities.computeUnion(upperThumbRect.x, upperThumbRect.y, upperThumbRect.width, upperThumbRect.height, upperUnionRect);
            slider.repaint(upperUnionRect.x, upperUnionRect.y, upperUnionRect.width, upperUnionRect.height);
        }

        /**
         * Moves the selected thumb in the specified direction by a block
         * increment. This method is called when the user presses the Page Up or
         * Down keys.
         */
        public void scrollByBlock(int direction) {
            synchronized (slider) {
                int blockIncrement = (slider.getMaximum() - slider.getMinimum()) / 10;
                if (blockIncrement <= 0 && slider.getMaximum() > slider.getMinimum()) {
                    blockIncrement = 1;
                }
                int delta = blockIncrement * ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);

                if (upperThumbSelected) {
                    int oldValue = ((JRangeSlider) slider).getUpperValue();
                    ((JRangeSlider) slider).setUpperValue(oldValue + delta);
                } else {
                    int oldValue = slider.getValue();
                    slider.setValue(oldValue + delta);
                }
            }
        }

        /**
         * Moves the selected thumb in the specified direction by a unit
         * increment. This method is called when the user presses one of the
         * arrow keys.
         */
        public void scrollByUnit(int direction) {
            synchronized (slider) {
                int delta = 1 * ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);

                if (upperThumbSelected) {
                    int oldValue = ((JRangeSlider) slider).getUpperValue();
                    ((JRangeSlider) slider).setUpperValue(oldValue + delta);
                } else {
                    int oldValue = slider.getValue();
                    slider.setValue(oldValue + delta);
                }
            }
        }

        /**
         * Listener to handle model change events. This calculates the thumb
         * locations and repaints the slider if the value change is not caused
         * by dragging a thumb.
         */
        public class ChangeHandler implements ChangeListener {

            public void stateChanged(ChangeEvent arg0) {
                if (!lowerDragging && !upperDragging) {
                    calculateThumbLocation();
                    slider.repaint();
                }
            }
        }

        /**
         * Listener to handle mouse movements in the slider track.
         */
        public class RangeTrackListener extends BasicSliderUI.TrackListener {

            @Override
            public void mousePressed(MouseEvent e) {
                if (!slider.isEnabled()) {
                    return;
                }
                currentMouseX = e.getX();
                currentMouseY = e.getY();
                if (slider.isRequestFocusEnabled()) {
                    slider.requestFocus();
                }
                updateActive(e);
                boolean lowerPressed = false;
                boolean upperPressed = false;
                if (upperThumbSelected || slider.getMinimum() == slider.getValue()) {
                    if (upperThumbRect.contains(currentMouseX, currentMouseY)) {
                        upperPressed = true;
                    } else if (thumbRect.contains(currentMouseX, currentMouseY)) {
                        lowerPressed = true;
                    }
                } else {
                    if (thumbRect.contains(currentMouseX, currentMouseY)) {
                        lowerPressed = true;
                    } else if (upperThumbRect.contains(currentMouseX, currentMouseY)) {
                        upperPressed = true;
                    }
                }
                if (lowerPressed) {
                    switch (slider.getOrientation()) {
                        case JSlider.VERTICAL:
                            offset = currentMouseY - thumbRect.y;
                            break;
                        case JSlider.HORIZONTAL:
                            offset = currentMouseX - thumbRect.x;
                            break;
                    }
                    upperThumbSelected = false;
                    lowerDragging = true;
                    return;
                }
                lowerDragging = false;
                if (upperPressed) {
                    switch (slider.getOrientation()) {
                        case JSlider.VERTICAL:
                            offset = currentMouseY - upperThumbRect.y;
                            break;
                        case JSlider.HORIZONTAL:
                            offset = currentMouseX - upperThumbRect.x;
                            break;
                    }
                    upperThumbSelected = true;
                    upperDragging = true;
                    return;
                }
                upperDragging = false;
                if (!lowerPressed && !upperPressed) {
                    upperThumbSelected = Math.abs(currentMouseX - thumbRect.x) > Math.abs(currentMouseX - upperThumbRect.x);
                    boolean dir = upperThumbSelected ? ((currentMouseX - upperThumbRect.x) > 0) : ((currentMouseX - thumbRect.x) > 0);
                    scrollByBlock(dir ? 1 : 0);
                    scrollTimer.stop();
                    scrollListener.setDirection(dir ? 1 : 0);
                    scrollTimer.start();
                }
            }

            @Override
            public boolean shouldScroll(int direction) {
                if (upperThumbSelected) {
                    return direction == 1 ? currentMouseX > upperThumbRect.x : currentMouseX < upperThumbRect.x;
                } else {
                    return direction == 1 ? currentMouseX > thumbRect.x : currentMouseX < thumbRect.x;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lowerDragging = false;
                upperDragging = false;
                updateActive(e);
                slider.setValueIsAdjusting(false);
                scrollTimer.stop();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateActive(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateActive(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!slider.isEnabled()) {
                    return;
                }
                currentMouseX = e.getX();
                currentMouseY = e.getY();
                if (lowerDragging) {
                    slider.setValueIsAdjusting(true);
                    moveLowerThumb();

                } else if (upperDragging) {
                    slider.setValueIsAdjusting(true);
                    moveUpperThumb();
                }
            }

            /**
             * Moves the location of the lower thumb, and sets its corresponding
             * value in the slider.
             */
            private void moveLowerThumb() {
                int thumbMiddle = 0;

                switch (slider.getOrientation()) {
                    case JSlider.VERTICAL:
                        int halfThumbHeight = thumbRect.height / 2;
                        int thumbTop = currentMouseY - offset;
                        int trackTop = trackRect.y;
                        int trackBottom = trackRect.y + (trackRect.height - 1);
                        int vMax = yPositionForValue(slider.getValue() + slider.getExtent());

                        // Apply bounds to thumb position.
                        if (drawInverted()) {
                            trackBottom = vMax;
                        } else {
                            trackTop = vMax;
                        }
                        thumbTop = Math.max(thumbTop, trackTop - halfThumbHeight);
                        thumbTop = Math.min(thumbTop, trackBottom - halfThumbHeight);

                        setThumbLocation(thumbRect.x, thumbTop);

                        // Update slider value.
                        thumbMiddle = thumbTop + halfThumbHeight;
                        slider.setValue(valueForYPosition(thumbMiddle));
                        break;

                    case JSlider.HORIZONTAL:
                        int halfThumbWidth = thumbRect.width / 2;
                        int thumbLeft = currentMouseX - offset;
                        int trackLeft = trackRect.x;
                        int trackRight = trackRect.x + (trackRect.width - 1);
                        int hMax = xPositionForValue(slider.getValue() + slider.getExtent());

                        // Apply bounds to thumb position.
                        if (drawInverted()) {
                            trackLeft = hMax;
                        } else {
                            trackRight = hMax;
                        }
                        thumbLeft = Math.max(thumbLeft, trackLeft);
                        thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);

                        setThumbLocation(thumbLeft, thumbRect.y);

                        // Update slider value.
                        thumbMiddle = thumbLeft + halfThumbWidth;
                        slider.setValue(valueForXPosition(thumbMiddle));
                        break;

                    default:
                        return;
                }
            }

            /**
             * Moves the location of the upper thumb, and sets its corresponding
             * value in the slider.
             */
            private void moveUpperThumb() {
                int thumbMiddle = 0;

                switch (slider.getOrientation()) {
                    case JSlider.VERTICAL:
                        int halfThumbHeight = thumbRect.height / 2;
                        int thumbTop = currentMouseY - offset;
                        int trackTop = trackRect.y;
                        int trackBottom = trackRect.y + (trackRect.height - 1);
                        int vMin = yPositionForValue(slider.getValue());

                        // Apply bounds to thumb position.
                        if (drawInverted()) {
                            trackTop = vMin;
                        } else {
                            trackBottom = vMin;
                        }
                        thumbTop = Math.max(thumbTop, trackTop - halfThumbHeight);
                        thumbTop = Math.min(thumbTop, trackBottom - halfThumbHeight);

                        setUpperThumbLocation(thumbRect.x, thumbTop);

                        // Update slider extent.
                        thumbMiddle = thumbTop + halfThumbHeight;
                        slider.setExtent(valueForYPosition(thumbMiddle) - slider.getValue());
                        break;

                    case JSlider.HORIZONTAL:
                        int halfThumbWidth = thumbRect.width / 2;
                        int thumbLeft = currentMouseX - offset;
                        int trackLeft = trackRect.x;
                        int trackRight = trackRect.x + (trackRect.width - 1);
                        int hMin = xPositionForValue(slider.getValue());

                        // Apply bounds to thumb position.
                        if (drawInverted()) {
                            trackRight = hMin;
                        } else {
                            trackLeft = hMin;
                        }
                        thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
                        thumbLeft = Math.min(thumbLeft, trackRight - thumbRect.width + 2);

                        setUpperThumbLocation(thumbLeft, thumbRect.y);

                        // Update slider extent.
                        thumbMiddle = thumbLeft + halfThumbWidth;
                        slider.setExtent(valueForXPosition(thumbMiddle) - slider.getValue());
                        break;

                    default:
                        return;
                }
            }

            private void updateActive(MouseEvent e) {
                if (!slider.isEnabled()) {
                    return;
                }
                currentMouseX = e.getX();
                currentMouseY = e.getY();
                upperHover = upperDragging || upperThumbRect.contains(currentMouseX, currentMouseY);
                lowerHover = lowerDragging || thumbRect.contains(currentMouseX, currentMouseY);
                slider.repaint(thumbRect);
                slider.repaint(upperThumbRect);
            }
        }
    }

}
