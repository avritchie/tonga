/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mainPackage;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import mainPackage.protocols._NucleusCounterIntensity;
import mainPackage.protocols.Protocol;
import mainPackage.protocols._NucleusCounterDoubleIntensity;
import mainPackage.protocols._NucleusCounterNumber;
import mainPackage.protocols._NucleusCounterPositivity;
import mainPackage.protocols._NucleusCounterSurroundIntensity;
import mainPackage.protocols._ObjectCount;
import mainPackage.protocols._ObjectSegmentCount;

public class Wizard extends javax.swing.JFrame {

    int id;
    int ls;
    boolean[] a; // answers given, true if first, false if second
    boolean launchStatus; // has a protocol been launched yes/no
    WizardEntry[] entries;
    DefaultListModel listModel;

    public Wizard() {
        initComponents();
        createList();
        entries = new WizardEntry[99];
        entryGrid();
        reset();
    }

    private void reset() {
        a = new boolean[99];
        loadEntry(0);
        listModel.clear();
    }

    private void entryGrid() {
        entries[0] = new WizardEntry(0, "What do you want to do?", "Task", "Analyze new images", "Quantify pre-processed masks",
                "Analyze new microscopy images of tissues, cells etc.",
                "Quantify values from masks which have been created previously.");
        entries[1] = new WizardEntry(1, "What do you want to measure?", "Measure", "Areas", "Objects",
                "An area to measure, typically tissues.",
                "Individual objects to measure, typically cells or organoids.");
        entries[2] = new WizardEntry(2, "What kind of microscopy was used?", "Microscopy", "Fluorescence", "Brightfield",
                "Fluorescence microscopy. Dark background and fluorescent dyes.",
                "Brightfield microscopy. Light background and histochemical stains.");
        entries[3] = new WizardEntry(3, "What do you want to know?", "Calculate", "Area with a certain staining", "Just the tissue area",
                "The tissue has areas stained with a separate fluorescent dye. How much staining is there? How big part of the tissue has the stain?",
                "How much tissue there is in total, regardless of the stain? What is the tissue area?");
        entries[4] = new WizardEntry(4, "How do you want to quantify the stain?", "Staining", "Binary", "Intensity",
                "Mark areas as either positive or negative for the stain(s) and calculate the proportion of tissue which is positive.",
                "Calculate the total amount of staining in the area using the intensity.");
        entries[5] = new WizardEntry(5, "How many stainings do you want to quantify?", "Number of stains", "One", "Two",
                "Quantify only one stain. Calculate the values for this stain only.",
                "Calculate two stains which may overlap. Calculates the values for both stainings separately, as well as estimates the overlapping area positive for both stainings.");
        entries[6] = new WizardEntry(6, "What kind of staining is it?", "Stain type", "Chromogen", "Histochemistry",
                "The tissue has been stained with hematoxylin, and the staining to be quantified is marked by chromogen, such as DAB. Calculate the intensity of this chromogen.",
                "The tissue has been stained with basic histochemical stainings, such as hematoxylin & eosin. Separate the stainings and calculate areas.");
        entries[7] = new WizardEntry(7, "What do you want to know?", "Calculate", "Number and/or size", "Staining intensity/positivity",
                "How many objects there are, or what kind of properties, such as size or roundness, they have?",
                "How strong is the dye in the objects, or are some objects positive for a dye while others are not?");
        entries[8] = new WizardEntry(8, "What kind of microscopy was used?", "Microscopy", "Fluorescence", "Phase contrast",
                "Fluorescence microscopy. Dark background and fluorescent dyes.",
                "Phase contrast microscopy. Black and white images.");
        entries[9] = new WizardEntry(9, "Do the objects touch each other?", "Segmentation", "Yes", "No",
                "The objects touch and overlap each other. Perform segmenting to separate them. Otherwise two overlapping objects will be detected as one object.",
                "The objects do not overlap. Skip segmenting for improved performance as it is not necessary.");
        entries[10] = new WizardEntry(10, "Calculate the statistics for every image or for every object?", "Statistics", "Every image", "Every object",
                "Compare values between pictures. The value is calculated for every image."
                + "The values for every object are calculated first, and then basic statistics such as the average are calculated for the image. ",
                "Compare objects inside the picture. The value is calculated for every object recognized in the image separately. "
                + "For example, when you want to know the intensity for every individual cell in the picture.");
        entries[11] = new WizardEntry(11, "Are you interested in positivity or intensity?", "Staining", "Positivity", "Intensity",
                "Consider the individual objects to be either positive or negative. For example, dividing or not, alive or dead.",
                "Calculate the total intensity of a certain stain in the object.");
        entries[12] = new WizardEntry(12, "Objects themselves or the area around them?", "Staining area", "Objects themselves", "Object surroundings",
                "Objects themselves. The staining in question is inside the object, such as inside the nucleus.",
                "Object surroundings. Look around the object for staining. Choose a radius to use.");
        entries[13] = new WizardEntry(13, "In the whole object or separated by another stain?", "Staining area", "The whole object", "Marked by another stain",
                "Calculate the staining in the full area of the recognized object. Don't filter the signal in any way.",
                "Calculate the staining only in the area which is already marked by another staining. As an example, calculate the green staining only if also the red staining is present.");
        entries[14] = new WizardEntry(14, "Remove dead and dividing cells?", "Remove dead and dividing", "Yes", "No",
                "Attempt to recognize cells which are dead or dividing and remove them from the analysis.",
                "Do not attempt to recognize dead or dividing cells. Consider all the objects.");
        entries[15] = new WizardEntry(15, "Estimate the area or sum up the intensity?", "Surrounding area", "Estimate", "Sum up",
                "Estimate the area around the object. For example, cell appendages around the nucleus.",
                "Sum up all the intensity around the object.");
        entries[16] = new WizardEntry(16, "Only consider non-overlapping objects?", "Only non-overlapping", "Yes", "No",
                "If the areas around the objects are overlapping, exclude these objects from the analysis. "
                + "Ie. analyze only objects which do not have another objects very close by.",
                "Analyze all objects regardless of their distance to each other.");
        entries[17] = new WizardEntry(17, "What do you want to calculate?", "Question", "How many?", "How much?",
                "The number of areas in the image.",
                "The amount of color(s) in the image.");
        entries[18] = new WizardEntry(18, "In objects or in the total area?", "Calculate values", "Objects", "Total area",
                "First recognize separate areas in the image and then calculate the values in those areas.",
                "Calculate the values in the whole picture.");
        entries[19] = new WizardEntry(19, "Number of pixels or gradient of color?", "Calculate", "Number of pixels", "Gradient of color",
                "How much of a certain color there is? Calculate the number of pixels of that colour. Use when the area is marked by an even color.",
                "Gradient. Sum up the values.");
        entries[20] = new WizardEntry(20, "Binarize or sum the values up?", "Values", "Binarize", "Sum up",
                "Consider the area to be either positive or not.",
                "Sum up all the values.");
        entries[21] = new WizardEntry(21, "Apply illumination correction?", "Illumination correction", "Yes", "No",
                "Estimate the background lighting and attempt to make it even. Correcting uneven lighting can make object detection easier.",
                "Keep everything as it is.");
        entries[22] = new WizardEntry(22, "Remove only partially visible objects?", "Remove partially visible", "Yes", "No",
                "Do you want to remove the objects which are touching the edges and are only partially visible in the image? "
                + "If a part of the objects is outside the image, it can result in outliers and inaccurate results, especially for cell intensity measurements. "
                + "For things like counting cells it may have less importance.",
                "Do not remove anything, keep all the recognized objects regardless of where they are.");
        entries[23] = new WizardEntry(23, "Estimate and remove the background?", "Estimate background", "Yes", "No",
                "Attempt to recognize areas far from any objects and estimate the intensity of staining background noise. "
                + "Then, subtract this value from the intensity measurements. "
                + "You can see both the uncorrected and corrected value in the result table.",
                "Do not do any estimation or subtract anything. Show the values exactly as they are.");
    }

    private int getDest(boolean f) {
        // f = is first option?
        switch (id) {
            case 0:
                return f ? 1 : 17;
            case 1:
                return f ? 2 : 7;
            case 3:
                return f ? a[2] ? 4 : 6 : -1;
            case 5:
                return -1;
            case 6:
                return -1;
            case 7:
                return f ? 8 : 11;
            case 8:
                return f ? 9 : 21;
            case 9:
                return a[18] ? 19 : a[17] ? -1 : 22;
            case 10:
                return -1;
            case 11:
                return f ? 9 : 12;
            case 12:
                return a[18] ? f ? 9 : 19 : f ? 13 : 15;
            case 13:
                return 9;
            case 14:
                return 10;
            case 16:
                return -1;
            case 17:
                return f ? 9 : 18;
            case 18:
                return f ? 12 : 19;
            case 19:
                return f ? a[18] ? 10 : -1 : 20;
            case 20:
                return a[18] ? 10 : -1;
            case 21:
                return 9;
            case 22:
                return a[7] ? 14 : 23;
            case 23:
                return 14;
            default:
                return id + 1;
        }
    }

    private void selectProtocol() {
        launchStatus = false;
        if (a[0]) { //new images
            if (a[1]) { //tissue
                //TODO
            } else { //objects
                if (a[9]) { //segment
                    if (a[7]) { //numbersize
                        if (a[8]) { //fluorescence
                            launchProtocol(_NucleusCounterNumber::new, new Object[]{a[22], null, a[14], a[10]});
                        } else { //phase contrast
                            //TODO
                        }
                    } else { //stainings
                        if (a[11]) { //positivity
                            launchProtocol(_NucleusCounterPositivity::new, new Object[]{null, a[23], a[22], a[14], a[10]});
                        } else { //intensity
                            if (a[12]) { //object themselves
                                if (a[13]) { // whole/single stain
                                    launchProtocol(_NucleusCounterIntensity::new, new Object[]{a[23], a[22], a[14], a[10]});
                                } else { // separate/two stains
                                    launchProtocol(_NucleusCounterDoubleIntensity::new, new Object[]{a[23], a[22], a[14], a[10]});
                                }
                            } else { //object surroundings
                                launchProtocol(_NucleusCounterSurroundIntensity::new, new Object[]{a[22], a[14], null, a[10]});
                            }
                        }
                    }
                } else { //dont segment
                    if (a[7]) { //numbersize
                        if (a[8]) { //fluorescence
                            //NON SEGM launchProtocol(_NucleusCounterNumber::new, new Object[]{a[22], null, a[14],a[10]});
                        } else { //phase contrast
                            //TODO
                        }
                    } else { //stainings
                        if (a[11]) { //positivity
                            //NON SEGM launchProtocol(_NucleusCounterPositivity::new, new Object[]{null,a[23], a[22], a[14],a[10]});
                        } else { //intensity
                            if (a[12]) { //object themselves
                                if (a[13]) { // whole/single stain
                                    //NON SEGM launchProtocol(_NucleusCounterIntensity::new, new Object[]{a[23], a[22], a[14],a[10]});
                                } else { // separate/two stains
                                    //NON SEGMN launchProtocol(_NucleusCounterDoubleIntensity::new, new Object[]{a[23], a[22], a[14],a[10]});
                                }
                            } else { //object surroundings
                                //NON SEGM launchProtocol(_NucleusCounterSurroundIntensity::new, new Object[]{a[22], a[14],null,a[10]});
                            }
                        }
                    }
                }
            }
        } else { //masks
            if (a[17]) { //how many
                if (a[9]) { //touching
                    launchProtocol(_ObjectSegmentCount::new, new Object[]{null, null});
                } else { //not touching
                    launchProtocol(_ObjectCount::new, new Object[]{null});
                }
            } else { //how much
            }
        }
        if (!launchStatus) {
            Tonga.catchError(new UnsupportedOperationException(), "The protocol for this outcome is not available yet, we apologize.");
        }
        Tonga.frame().closeDialog(this);
    }

    private void launchProtocol(Supplier<Protocol> supp, Object[] params) {
        Tonga.frame().launchProtocol(supp, null);
        Protocol cp = Tonga.frame().currentProtocol;
        cp.param.setControlParameters(cp.panelCreator, params);
        launchStatus = true;
    }

    private void answerQuestion(boolean b) {
        int r = getDest(b);
        if (r != -1) {
            listModel.addElement(new WizardReply(id, b, entries[id].key + ": " + entries[id].opt[b ? 0 : 1]));
            a[id] = b;
            loadEntry(r);
        } else {
            selectProtocol();
        }
    }

    private Icon getIcon(int id, int i) {
        ImageIcon ic;
        try {
            ic = new ImageIcon(getClass().getResource("/resourcePackage/wizard/" + id + "_" + i + ".png"));
        } catch (Exception ex) {
            Tonga.log.warn("Missing an image for {}_{}", id, i);
            return new ImageIcon(getClass().getResource("/resourcePackage/wizard/rikki.png"));
        }
        return ic;
    }

    private void createList() {
        java.awt.Color selectColor = new java.awt.Color(57, 105, 138);
        DefaultListCellRenderer listRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(((WizardReply) value).text);
                if (isSelected) {
                    setBackground(selectColor);
                    setForeground(java.awt.Color.WHITE);
                } else {
                    setForeground(java.awt.Color.BLACK);
                    setBackground(java.awt.Color.WHITE);
                }
                return c;
            }
        };
        listModel = (DefaultListModel) selectList.getModel();
        selectList.setCellRenderer(listRenderer);
        selectList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList) evt.getSource();
                if (evt.getButton() == MouseEvent.BUTTON1) {
                    if (evt.getClickCount() == 1) {
                        ls = list.getSelectedIndex();
                    } else if (evt.getClickCount() == 2 && ls == list.getSelectedIndex()) {
                        Tonga.log.debug("Wizard reply: {}", ((WizardReply) list.getSelectedValue()).text);
                        revertTo(list.getSelectedIndex());
                    }
                }
            }
        });
    }

    private void revertTo(int ind) {
        int l = listModel.getSize(), i;
        WizardReply r = null;
        for (i = l - 1; i >= ind; i--) {
            r = (WizardReply) listModel.get(i);
            a[r.id] = false;
            listModel.remove(i);
        }
        loadEntry(r.id);
    }

    private class WizardEntry {

        int id;
        String[] opt;
        String[] desc;
        String quest;
        String key;

        WizardEntry(int id, String quest, String key, String opt1, String opt2, String desc1, String desc2) {
            this.id = id;
            this.key = key;
            this.opt = new String[]{opt1, opt2};
            this.desc = new String[]{desc1, desc2};
            this.quest = quest;
        }
    }

    private void loadEntry(int id) {
        WizardEntry we = entries[id];
        if (we == null) {
            Tonga.catchError(new IllegalArgumentException(), "Entry number " + id + " does not exist.");
        } else {
            this.id = id;
            firstSelection.setText(we.opt[0]);
            secondSelection.setText(we.opt[1]);
            firstImage.setIcon(getIcon(we.id, 0));
            secondImage.setIcon(getIcon(we.id, 1));
            question.setText(we.quest);
            firstDescription.setText("<html>" + we.desc[0] + "</html>");
            secondDescription.setText("<html>" + we.desc[1] + "</html>");

        }
    }

    private class WizardReply {

        int id;
        boolean reply;
        String text;

        public WizardReply(int id, boolean reply, String text) {
            this.id = id;
            this.reply = reply;
            this.text = text;
        }
    }

    @Override
    public void setVisible(boolean yes) {
        super.setVisible(yes);
        if (yes) {
            reset();
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        firstSelection = new javax.swing.JButton();
        secondSelection = new javax.swing.JButton();
        question = new javax.swing.JLabel();
        firstDescription = new javax.swing.JLabel();
        secondDescription = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        secondPanel = new javax.swing.JPanel();
        secondImage = new javax.swing.JLabel();
        firstPanel = new javax.swing.JPanel();
        firstImage = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        selectList = new javax.swing.JList<>();

        setTitle("Wizard");
        setResizable(false);
        setSize(new java.awt.Dimension(930, 424));
        setType(java.awt.Window.Type.POPUP);

        firstSelection.setText("Yes");
        firstSelection.setPreferredSize(new java.awt.Dimension(200, 30));
        firstSelection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                firstSelectionActionPerformed(evt);
            }
        });

        secondSelection.setText("Yes");
        secondSelection.setPreferredSize(new java.awt.Dimension(200, 30));
        secondSelection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secondSelectionActionPerformed(evt);
            }
        });

        question.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        question.setText("The quick brown fox jumps over the lazy dog");
        question.setToolTipText("You're a wizard, Harry");

        firstDescription.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        firstDescription.setText("<html>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed ut tempus tellus. Pellentesque eget nisl justo. Praesent erat neque, tempus vel posuere eget, aliquet at quam.</html>");
        firstDescription.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        firstDescription.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);

        secondDescription.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        secondDescription.setText("<html>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla tincidunt commodo est, eu ornare ipsum gravida sit amet. Integer ut porttitor mauris. Phasellus ac turpis eros.</html>");
        secondDescription.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        secondDescription.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);

        jLabel1.setText("<html><b>Selections so far</b><br>Double click a selection to go back...</html>");
        jLabel1.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        secondPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        secondImage.setBackground(new java.awt.Color(0, 0, 0));
        secondImage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout secondPanelLayout = new javax.swing.GroupLayout(secondPanel);
        secondPanel.setLayout(secondPanelLayout);
        secondPanelLayout.setHorizontalGroup(
            secondPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(secondImage, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        secondPanelLayout.setVerticalGroup(
            secondPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(secondImage, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        firstPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        firstImage.setBackground(new java.awt.Color(0, 0, 0));
        firstImage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        firstImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resourcePackage/wizard/0_0.png"))); // NOI18N

        javax.swing.GroupLayout firstPanelLayout = new javax.swing.GroupLayout(firstPanel);
        firstPanel.setLayout(firstPanelLayout);
        firstPanelLayout.setHorizontalGroup(
            firstPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(firstImage, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        firstPanelLayout.setVerticalGroup(
            firstPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(firstImage, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        selectList.setModel(new DefaultListModel());
        selectList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(selectList);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(firstDescription, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(firstPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(firstSelection, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(25, 25, 25)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(secondPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(secondDescription, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(secondSelection, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(question, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(25, 25, 25))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(question, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(secondPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(firstPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(20, 20, 20)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(firstDescription, javax.swing.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE)
                            .addComponent(secondDescription))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(firstSelection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(secondSelection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(25, 25, 25))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void firstSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_firstSelectionActionPerformed
        answerQuestion(true);
    }//GEN-LAST:event_firstSelectionActionPerformed

    private void secondSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_secondSelectionActionPerformed
        answerQuestion(false);
    }//GEN-LAST:event_secondSelectionActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel firstDescription;
    private javax.swing.JLabel firstImage;
    private javax.swing.JPanel firstPanel;
    private javax.swing.JButton firstSelection;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel question;
    private javax.swing.JLabel secondDescription;
    private javax.swing.JLabel secondImage;
    private javax.swing.JPanel secondPanel;
    private javax.swing.JButton secondSelection;
    private javax.swing.JList<String> selectList;
    // End of variables declaration//GEN-END:variables
}
