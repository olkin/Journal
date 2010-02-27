
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * TermPanel.java
 *
 * Created on Feb 21, 2010, 5:42:40 PM
 */
/**
 *
 * @author olkin
 */
public class TermPanel extends javax.swing.JPanel /*implements java.io.Serializable*/ {

    private static final BigDecimal HIGHEST_NOTE = BigDecimal.TEN;
    private static final BigDecimal DEFAULT_AVG_VALUE = new BigDecimal(0.0);
    private static final int DEFAULT_NUMBER_OF_TRIES = 20;
    private NotesField originalNotes;
    private NotesField improvedNotes;
    private NotesField originalTestNotes;
    private NotesField improvedTestNotes;
    private BigDecimal originalAvgTerm;
    private BigDecimal improvedAvgTerm;
    private boolean isTestEnabled;
    /**
     * Format of the average notes.
     * Used values are positive and less than 10, so ormat used in this code is cutting the ending of the number,
     * leaving 3 digits
     * E.g. formatted "2.657" is "2.65"; "2.1111111" is "2.11"
     */
    public final static MathContext avgFormat = new MathContext(3, RoundingMode.FLOOR);

    private class NotesField {

        private NotesField() {
            this.reset();
        }

        private NotesField(NotesField x) {
            this.reset(x);
        }
        private int sum;
        private int count;
        private String text;

        private void reset() {
            sum = 0;
            count = 0;
            text = "";
        }

        private void reset(NotesField notesField) {
            this.sum = notesField.sum;
            this.count = notesField.count;
            this.text = notesField.text;
        }

        private NotesField append(NotesField notesField){
            NotesField sumNotes = new NotesField();
            sumNotes.sum = this.sum + notesField.sum;
            sumNotes.count= this.count + notesField.count;
            sumNotes.text = this.text + notesField.text;
            return sumNotes;
        }
    };
    


    /** Creates new form TermPanel */
    public TermPanel() {
        initComponents();
        originalNotes = new NotesField();
        improvedNotes = new NotesField();
        originalTestNotes = new NotesField();
        improvedTestNotes = new NotesField();
        resetAllInfo();
        isTestEnabled = false;
        setTestBtn();
    }

    /** Recalculation related to changes in notesText field that
     */
    private void changeNote(JTextField notesText, NotesField notes) {
        String notesFromText = notesText.getText();
        notes.reset(parseNotes(notesFromText));
        notesText.setText(notes.text);
        updateAvg();
    }



    /** Algorithm to improve originals notes to have average as avgNeeded (in formatted form)*/
    private NotesField improveNotes(BigDecimal avgNeeded, NotesField originals) {
        // First check if improvement is  needed

        if (originals.count != 0 && 
                getAvgNote(originals).compareTo(avgNeeded) == 0)
        {
            return new NotesField();
        }

        int count = 1;  // number of notes to be addedv

        do {
            // find originals.sum limits
            BigDecimal tmpFixedValue1 = new BigDecimal(originals.count + count);
            BigDecimal tmpFixedValue2 = avgNeeded.multiply(tmpFixedValue1).subtract(new BigDecimal(originals.sum));
            BigDecimal tmpFixedValue3 = new BigDecimal(0.01*(originals.count + count));         // 0.01 = 10(-2) is bound to the format

            int lowerLimit = tmpFixedValue2.subtract(tmpFixedValue3).intValue();
            int upperLimit = tmpFixedValue2.add(tmpFixedValue3).intValue();

            for (int sum = lowerLimit; sum <= upperLimit; sum++) {
                // Avg of the added notes cannot be more than the highest note or less than the smallest note(1)
                if (sum / count > HIGHEST_NOTE.intValue() || sum / count < 1) {
                    continue;
                }

                BigDecimal avg = new BigDecimal ((double) (originals.sum + sum)).divide(tmpFixedValue1, avgFormat);
                if (avg.compareTo(avgNeeded) != 0) //equal strings
                {
                    continue;
                }


                // Otherwise we've found the value we need
                NotesField improvement = new NotesField();
                improvement.sum = sum;
                improvement.count = count;

                // It means that we've found how to make the improvement
                // Find how to make this improvement, It should be > Highest_note & < than lowest note
                int base_note = (improvement.sum) / (improvement.count);
                int it = 0;
                for (; it < improvement.count - improvement.sum % improvement.count; it++) {
                    improvement.text += base_note == 10? 0 :base_note;
                }

                for (; it < improvement.count; it++) {
                    if (base_note + 1 > HIGHEST_NOTE.intValue()){
                        // Impossible situation. Notes cannot be more than Highest note
                        improvement = null;
                        break;
                    }

                    // Add note to improvement text. If it's 10 replace by 1
                    improvement.text += (base_note + 1) == 10? 0 :base_note+1;
                }

                // try again if uimprovement wasn't reached
                if (improvement == null)
                    continue;
                else
                    return improvement;
            }

        } while (count++ <= DEFAULT_NUMBER_OF_TRIES);

        // Improvement is not found
        return null;
    }

    private void improveNotesAction(NotesField originals, NotesField improvals, JTextField improvedTextField) {
        BigDecimal avgNeeded = getInputAvgValue();
        if (avgNeeded.compareTo(DEFAULT_AVG_VALUE) != 0) {
            NotesField result = improveNotes(avgNeeded, originals);
            if (result != null) {
                improvedTextField.setText(result.text);
                changeNote(improvedTextField, improvals);
            } else {
                JOptionPane.showMessageDialog(null, "Cannot find the values to improve existing notes to specified average note " + avgNeeded);
            }
        }
    }

    private void resetOriginals() {
        // Now reset the rest
        originalNotesText.setText("");
        originalTestText.setText("");
        originalNotes.reset();
        originalTestNotes.reset();
        updateAvg();
        // don't want to change isTestEnabled flag
    }

    private void setTestBtn() {
        // Disable/Enable Info about the test
        originalTestText.setEnabled(isTestEnabled);
        improvedTestText.setEnabled(isTestEnabled);

        avgTestText.setEnabled(isTestEnabled);
        avgImprTestBtn.setEnabled(isTestEnabled);

        // Disable/Enable info about the term's results (if test is disabled the result here is the same as in usual notes)
        avgTermLabel.setEnabled(isTestEnabled);
        avgTermText.setEnabled(isTestEnabled);
        avgImprTermBtn.setEnabled(isTestEnabled);
        resetTestBtn.setEnabled(isTestEnabled);

        // Reset all test-related labels
        originalTestText.setText(isTestEnabled ? originalTestNotes.text : "No Test");
        improvedTestText.setText(isTestEnabled ? improvedTestNotes.text : "-");
        if (!isTestEnabled) {
            avgTestText.setText("-");
            avgImprTestBtn.setText("-");
            avgTermText.setText("-");
            avgImprTermBtn.setText("-");
        } else {
            updateAvg();
        }

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        notesLabel = new javax.swing.JLabel();
        originalNotesText = new javax.swing.JTextField();
        improvedNotesText = new javax.swing.JTextField();
        avgNotesText = new javax.swing.JTextField();
        improvedTestText = new javax.swing.JTextField();
        originalTestText = new javax.swing.JTextField();
        avgTestText = new javax.swing.JTextField();
        addRmvTestBtn = new javax.swing.JButton();
        avgTermLabel = new javax.swing.JLabel();
        avgTermText = new javax.swing.JTextField();
        originalNotesLabel = new javax.swing.JLabel();
        imprNotesLabel = new javax.swing.JLabel();
        avgLabel = new javax.swing.JLabel();
        avgImprLabel = new javax.swing.JLabel();
        resetTermBtn = new javax.swing.JButton();
        resetImprovementsBtn = new javax.swing.JButton();
        resetNotesBtn = new javax.swing.JButton();
        avgImprNotesBtn = new javax.swing.JButton();
        avgImprTestBtn = new javax.swing.JButton();
        avgImprTermBtn = new javax.swing.JButton();
        resetTestBtn = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Term", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP));
        setEnabled(false);

        notesLabel.setText("Notes:");

        originalNotesText.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        originalNotesText.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                originalNotesTextKeyReleased(evt);
            }
        });

        improvedNotesText.setForeground(new java.awt.Color(235, 16, 16));
        improvedNotesText.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        improvedNotesText.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                improvedNotesTextKeyReleased(evt);
            }
        });

        avgNotesText.setEditable(false);
        avgNotesText.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        avgNotesText.setText("0");
        avgNotesText.setFocusable(false);
        avgNotesText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                avgNotesTextActionPerformed(evt);
            }
        });

        improvedTestText.setForeground(new java.awt.Color(235, 16, 16));
        improvedTestText.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        improvedTestText.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                improvedTestTextKeyReleased(evt);
            }
        });

        originalTestText.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        originalTestText.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                originalTestTextKeyReleased(evt);
            }
        });

        avgTestText.setEditable(false);
        avgTestText.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        avgTestText.setText("0");
        avgTestText.setFocusable(false);

        addRmvTestBtn.setText("Test:");
        addRmvTestBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addRmvTestBtnActionPerformed(evt);
            }
        });

        avgTermLabel.setText("Avg:");

        avgTermText.setEditable(false);
        avgTermText.setText("0");
        avgTermText.setFocusable(false);

        originalNotesLabel.setText("Original:");

        imprNotesLabel.setText("Improved:");

        avgLabel.setText("Avg:");
        avgLabel.setFocusable(false);

        avgImprLabel.setText("Avg impr:");

        resetTermBtn.setText("Reset all");
        resetTermBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetTermBtnActionPerformed(evt);
            }
        });

        resetImprovementsBtn.setText("Reset Improvements");
        resetImprovementsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetImprovementsBtnActionPerformed(evt);
            }
        });

        resetNotesBtn.setText("Reset");
        resetNotesBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetNotesBtnActionPerformed(evt);
            }
        });

        avgImprNotesBtn.setForeground(new java.awt.Color(235, 16, 16));
        avgImprNotesBtn.setText("0");
        avgImprNotesBtn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        avgImprNotesBtn.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        avgImprNotesBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                avgImprNotesBtnActionPerformed(evt);
            }
        });

        avgImprTestBtn.setForeground(new java.awt.Color(235, 16, 16));
        avgImprTestBtn.setText("0");
        avgImprTestBtn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        avgImprTestBtn.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        avgImprTestBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                avgImprTestBtnActionPerformed(evt);
            }
        });

        avgImprTermBtn.setForeground(new java.awt.Color(235, 16, 16));
        avgImprTermBtn.setText("0");
        avgImprTermBtn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        avgImprTermBtn.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);

        resetTestBtn.setText("Reset");
        resetTestBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetTestBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(notesLabel)
                            .addComponent(addRmvTestBtn)
                            .addComponent(avgTermLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(originalNotesLabel)
                            .addComponent(originalNotesText, javax.swing.GroupLayout.PREFERRED_SIZE, 324, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(originalTestText, javax.swing.GroupLayout.PREFERRED_SIZE, 330, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(avgTermText, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(avgImprTermBtn)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(imprNotesLabel)
                                    .addComponent(improvedNotesText, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(improvedTestText, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(avgLabel)
                                    .addComponent(avgNotesText, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(avgTestText, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(avgImprTestBtn)
                                    .addComponent(avgImprNotesBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(resetTestBtn)
                                    .addComponent(resetNotesBtn)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(211, 211, 211)
                                .addComponent(avgImprLabel))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(resetImprovementsBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(resetTermBtn)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {avgImprNotesBtn, avgImprTermBtn, avgImprTestBtn, avgNotesText, avgTermText, avgTestText});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {originalNotesText, originalTestText});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {resetNotesBtn, resetTestBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(originalNotesLabel)
                    .addComponent(imprNotesLabel)
                    .addComponent(avgLabel)
                    .addComponent(avgImprLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(notesLabel)
                    .addComponent(originalNotesText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(improvedNotesText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(avgNotesText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(avgImprNotesBtn)
                    .addComponent(resetNotesBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(addRmvTestBtn)
                    .addComponent(originalTestText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(improvedTestText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(avgTestText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(avgImprTestBtn)
                    .addComponent(resetTestBtn))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(avgTermLabel)
                    .addComponent(avgTermText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(avgImprTermBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(resetImprovementsBtn)
                    .addComponent(resetTermBtn))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {originalNotesText, originalTestText});

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {avgImprNotesBtn, avgImprTermBtn, avgImprTestBtn, avgNotesText, avgTermText, avgTestText});

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {resetNotesBtn, resetTestBtn});

    }// </editor-fold>//GEN-END:initComponents

    public BigDecimal getImprovedAvgTerm() {
        return improvedAvgTerm;
    }

    public BigDecimal getOriginalAvgTerm() {
        return originalAvgTerm;
    }

    private void resetAllInfo() {
        resetImprovements();
        resetOriginals();

        // don't want to change isTestEnabled flag
    }

    private void resetImprovements() {
        improvedNotesText.setText("");
        improvedTestText.setText("");
        improvedNotes.reset();
        improvedTestNotes.reset();
        updateAvg();
    }

    private void updateAvg() {

        BigDecimal origAvgNote = getAvgNote (originalNotes);
        avgNotesText.setText(origAvgNote.toString());

        NotesField imprAndOrigNotes = new NotesField(improvedNotes.append(originalNotes));
        BigDecimal impAvgNote = getAvgNote(imprAndOrigNotes);
        avgImprNotesBtn.setText(impAvgNote.toString());

        if (isTestEnabled) {

            BigDecimal origAvgTest = getAvgNote(originalTestNotes);
            avgTestText.setText(origAvgTest.toString());

            NotesField imprAndOrigNotesTest = new NotesField(improvedTestNotes.append(originalTestNotes));
            BigDecimal impAvgTest = getAvgNote(imprAndOrigNotesTest);
            avgImprTestBtn.setText(impAvgTest.toString());

            // average between 2 average notes
            originalAvgTerm = getAvgNote(origAvgTest, origAvgNote);
            improvedAvgTerm = getAvgNote(impAvgTest , impAvgNote);

            avgTermText.setText(originalAvgTerm.toString());
            avgImprTermBtn.setText(improvedAvgTerm.toString());
        } else {
            originalAvgTerm = origAvgNote;
            improvedAvgTerm = impAvgNote;
        }
    }

        private BigDecimal getAvgNote(NotesField notesInfo){
        return (notesInfo.count != 0) ?
            new BigDecimal(notesInfo.sum).divide(new BigDecimal(notesInfo.count), avgFormat)   :
            DEFAULT_AVG_VALUE;
        }

        public static BigDecimal getAvgNote (BigDecimal note1, BigDecimal note2){
            return note1.add(note2).divide(new BigDecimal(2), avgFormat); 
        }

    private NotesField parseNotes(String originalNotes) {
        NotesField tmp = new NotesField();
        for (int i = 0; i < originalNotes.length(); i++) {
            if (originalNotes.charAt(i) >= '0' && originalNotes.charAt(i) <= '9') {


                if (originalNotes.charAt(i) == '0') {
                    // If 0 is not part of 10, count it as 10.
                    if (i == 0 || originalNotes.charAt(i - 1) != '1') {
                        tmp.sum += 10;
                        tmp.count++;
                    } else {
                        // Case when 0 is part of 10. we have already 1 in the sum, adding another 9
                        tmp.sum += 9;
                    }
                } else {
                    tmp.count++;
                    tmp.sum += originalNotes.charAt(i) - '0';
                }

                // Copy only digits
                tmp.text += originalNotes.charAt(i);
            }
        }
        return tmp;
    }

    private BigDecimal getInputAvgValue() {
        // Add dialog to know Avg note needed
        String response = JOptionPane.showInputDialog(null,
                "What average note you'd like to get?", "Enter average note you'd like to get:",
                JOptionPane.QUESTION_MESSAGE);

        // If "cancel" was pressed, ignore the action
        if (response == null) {
            return DEFAULT_AVG_VALUE;
        }

        // Check the value specified if it has correct format (of a double)
        BigDecimal avgNeeded = DEFAULT_AVG_VALUE;
        try {
            avgNeeded = new BigDecimal(response);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Value specified (" + response + ") cannot be accepted - wrong format ", "Please be careful", JOptionPane.ERROR_MESSAGE);
            return DEFAULT_AVG_VALUE;
        }

        // Check the value if it doesn't exceed max value and is not less than min
        if (avgNeeded.compareTo(BigDecimal.ONE) < 0 || avgNeeded.compareTo(HIGHEST_NOTE) > 0) {
            JOptionPane.showMessageDialog(null, "Value specified (" + response + ") cannot be accepted - wrong value ", "Next time please enter value from 1 to " + HIGHEST_NOTE, JOptionPane.ERROR_MESSAGE);
            return DEFAULT_AVG_VALUE;
        }

        return avgNeeded.round(avgFormat);

    }

    private void avgNotesTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_avgNotesTextActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_avgNotesTextActionPerformed

    private void addRmvTestBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRmvTestBtnActionPerformed
        // Change the flag
        isTestEnabled = !isTestEnabled;
        setTestBtn();
        updateAvg();
    }//GEN-LAST:event_addRmvTestBtnActionPerformed

    private void resetTermBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetTermBtnActionPerformed
        //TODO:: Add Are you sure dialog!
        resetAllInfo();
    }//GEN-LAST:event_resetTermBtnActionPerformed

    private void resetImprovementsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetImprovementsBtnActionPerformed
        resetImprovements();
    }//GEN-LAST:event_resetImprovementsBtnActionPerformed

    private void originalNotesTextKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_originalNotesTextKeyReleased
        changeNote(originalNotesText, originalNotes);
    }//GEN-LAST:event_originalNotesTextKeyReleased

    private void originalTestTextKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_originalTestTextKeyReleased
        changeNote(originalTestText, originalTestNotes);
    }//GEN-LAST:event_originalTestTextKeyReleased

    private void improvedNotesTextKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_improvedNotesTextKeyReleased
        changeNote(improvedNotesText, improvedNotes);
    }//GEN-LAST:event_improvedNotesTextKeyReleased

    private void improvedTestTextKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_improvedTestTextKeyReleased
        changeNote(improvedTestText, improvedTestNotes);
    }//GEN-LAST:event_improvedTestTextKeyReleased

    private void avgImprTestBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_avgImprTestBtnActionPerformed
        improveNotesAction(originalTestNotes, improvedTestNotes, improvedTestText);
    }//GEN-LAST:event_avgImprTestBtnActionPerformed

    private void resetNotesBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetNotesBtnActionPerformed
        originalNotesText.setText("");
        improvedNotesText.setText("");
        changeNote(originalNotesText, originalNotes);
        changeNote(improvedNotesText, improvedNotes);
    }//GEN-LAST:event_resetNotesBtnActionPerformed

    private void resetTestBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetTestBtnActionPerformed
        originalTestText.setText("");
        improvedTestText.setText("");
        changeNote(originalTestText, originalTestNotes);
        changeNote(improvedTestText, improvedTestNotes);
    }//GEN-LAST:event_resetTestBtnActionPerformed

    private void avgImprNotesBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_avgImprNotesBtnActionPerformed
        improveNotesAction(originalNotes, improvedNotes, improvedNotesText);
    }//GEN-LAST:event_avgImprNotesBtnActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addRmvTestBtn;
    private javax.swing.JLabel avgImprLabel;
    private javax.swing.JButton avgImprNotesBtn;
    private javax.swing.JButton avgImprTermBtn;
    private javax.swing.JButton avgImprTestBtn;
    private javax.swing.JLabel avgLabel;
    private javax.swing.JTextField avgNotesText;
    private javax.swing.JLabel avgTermLabel;
    private javax.swing.JTextField avgTermText;
    private javax.swing.JTextField avgTestText;
    private javax.swing.JLabel imprNotesLabel;
    private javax.swing.JTextField improvedNotesText;
    private javax.swing.JTextField improvedTestText;
    private javax.swing.JLabel notesLabel;
    private javax.swing.JLabel originalNotesLabel;
    private javax.swing.JTextField originalNotesText;
    private javax.swing.JTextField originalTestText;
    private javax.swing.JButton resetImprovementsBtn;
    private javax.swing.JButton resetNotesBtn;
    private javax.swing.JButton resetTermBtn;
    private javax.swing.JButton resetTestBtn;
    // End of variables declaration//GEN-END:variables
}
