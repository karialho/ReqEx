package fi.improveit.req_ex;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.swing.JFileChooser.DIRECTORIES_ONLY;

public class ExportPanel extends JPanel implements ActionListener, ItemListener {

    private final static int MAX_ROWS = 10000; // max number of requirements, if none specified
    private final JComboBox<String> projectSelector;
    private final JComboBox<String> baselineSelector;
    private final JComboBox<String> typeSelector;
    private final JCheckBox allTypes;
    private final JFileChooser fc;
    private final JTextField tf;
    private final JTextField maxRowCount;
    private final JLabel reqIFOptionsLabel;
    private final JRadioButton excel;
    private final JRadioButton reqIF;
    private final JRadioButton reqIFZ;
    private final JRadioButton XHTML;
    private final JRadioButton skipImages;
    private final JRadioButton plaintext;

    private CaliberSession cs;

    private static final Logger logger = LoggerFactory.getLogger(ExportPanel.class.getName());

    ExportPanel() {
        this.setLayout(new GridBagLayout());

        JLabel jLabel1 = new JLabel("Project: ");
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_END;
        this.add(jLabel1, c);

        projectSelector = new JComboBox<>();
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 3, 3, 3);
        this.add(projectSelector, c);

        JLabel blLabel = new JLabel("Baseline: ");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_END;
        this.add(blLabel, c);

        baselineSelector = new JComboBox<>();
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 3, 3, 3);
        this.add(baselineSelector, c);


        JLabel jLabel2 = new JLabel("Requirement type: ");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_END;
        this.add(jLabel2, c);

        typeSelector = new JComboBox<>();
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 3, 3, 3);
        this.add(typeSelector, c);

        allTypes = new JCheckBox("All types");
        allTypes.addItemListener(this);
        c = new GridBagConstraints();
        c.gridx = 4;
        c.gridy = 2;
        c.insets = new Insets(3, 3, 3, 3);
        this.add(allTypes, c);

        JLabel jLabel3 = new JLabel("Export file directory: ");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_END;
        this.add(jLabel3, c);

        tf = new JTextField(20);
        tf.setText(Defaults.get("export_file_dir"));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 3, 3, 3);
        this.add(tf, c);

        fc = new JFileChooser();
        fc.setFileSelectionMode(DIRECTORIES_ONLY);
        JButton open = new JButton("Open...", Util.createImageIcon("/images/Open16.gif"));
        open.addActionListener(this);
        c = new GridBagConstraints();
        c.gridx = 4;
        c.gridy = 3;
        c.insets = new Insets(3, 3, 3, 3);
        this.add(open, c);

        JLabel jLabel4 = new JLabel("Max row count: ");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_END;
        this.add(jLabel4, c);

        maxRowCount = new JTextField(20);
        maxRowCount.setText(Defaults.get("max_row_count"));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 3, 3, 3);
        this.add(maxRowCount, c);

        JLabel jLabel5 = new JLabel("Export file: ");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 5;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_END;
        this.add(jLabel5, c);

        excel = new JRadioButton("Excel");
        excel.setSelected(true);
        excel.addItemListener(this);

        reqIF = new JRadioButton("ReqIF");
        reqIF.setSelected(false);
        reqIF.addItemListener(this);

        reqIFZ = new JRadioButton("ReqIFZ");
        reqIF.setSelected(false);
        reqIFZ.addItemListener(this);

        ButtonGroup group = new ButtonGroup();
        group.add(excel);
        group.add(reqIF);
        group.add(reqIFZ);

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new GridLayout(1, 3));
        filePanel.add(excel);
        filePanel.add(reqIF);
        filePanel.add(reqIFZ);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 5;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(3, 3, 3, 3);
        this.add(filePanel, c);

        reqIFOptionsLabel = new JLabel("ReqIF options: ");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 6;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_END;
        this.add(reqIFOptionsLabel, c);
        reqIFOptionsLabel.setEnabled(false);

        XHTML = new JRadioButton("XHTML");
        XHTML.setSelected(true);
        XHTML.setEnabled(false);

        skipImages = new JRadioButton("Skip images");
        skipImages.setSelected(false);
        skipImages.setEnabled(false);

        plaintext = new JRadioButton("Plaintext");
        plaintext.setSelected(false);
        plaintext.setEnabled(false);

        ButtonGroup reqIFGroup = new ButtonGroup();
        reqIFGroup.add(XHTML);
        reqIFGroup.add(skipImages);
        reqIFGroup.add(plaintext);

        JPanel reqIFPanel = new JPanel();
        reqIFPanel.setLayout(new GridLayout(1, 3));
        reqIFPanel.add(XHTML);
        reqIFPanel.add(skipImages);
        reqIFPanel.add(plaintext);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 6;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(3, 3, 3, 3);
        this.add(reqIFPanel, c);
    }

    public void init(CaliberSession cs) {
        try {
            this.cs = cs;
            setProjectList(cs.getProjectNames());
        } catch (CaliberException e) {
            logger.info("Exception: {}", e.getMessage());
        }
    }

    private void setProjectList(String[] plist) {
        projectSelector.removeAllItems();
        Arrays.sort(plist);
        for (String p : plist) {
            projectSelector.addItem(p);
        }
        try {
            cs.setProject(getProject());
            // Add baselines
            for (String b : cs.getBaselineNames()) {
                baselineSelector.addItem(b);
            }
            baselineSelector.addItemListener(this);
            // Add requirement types
            for (String t : cs.getTypeNames()) {
                typeSelector.addItem(t);
            }
        } catch (CaliberException ex) {
            logger.warn("Exception: {}", ex.getMessage());
        }
        projectSelector.addItemListener(this);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        try {
            Object item = e.getItem();
            ItemSelectable itemSelector = e.getItemSelectable();
            logger.info("itemStateChanged, item: " + item.toString());
            if (item == excel) {
                reqIFOptionsLabel.setEnabled(false);
                XHTML.setEnabled(false);
                skipImages.setEnabled(false);
                plaintext.setEnabled(false);
            }
            else if (item == reqIF || item == reqIFZ) {
                reqIFOptionsLabel.setEnabled(true);
                XHTML.setEnabled(true);
                skipImages.setEnabled(true);
                plaintext.setEnabled(true);
            }
            else if (item == allTypes) {
                updateTypes();
            }
            else if (itemSelector == projectSelector) {
                // Project was changed
                cs.setProject(getProject());
                baselineSelector.removeItemListener(this);
                baselineSelector.removeAllItems();
                for (String b : cs.getBaselineNames()) {
                    baselineSelector.addItem(b);
                }
                cs.setBaseline(getBaseline());
                baselineSelector.addItemListener(this);
                updateTypes();
            } else if (itemSelector == baselineSelector) {
                // Baseline was changed
                cs.setBaseline(getBaseline());
                updateTypes();
            }
        } catch (CaliberException ex) {
            logger.info("Exception: {}", ex.getMessage());
        }

    }

    private void updateTypes() {
        try {
            typeSelector.removeAllItems();
            if (allTypes.isSelected())
                return;
            String[] types = cs.getTypeNames();
            Arrays.sort(types);
            for (String t : types) {
                typeSelector.addItem(t);
            }
        } catch (CaliberException ex) {
            logger.info("Exception: {}", ex.getMessage());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            logger.info("Opening directory: {}", f.getName());
            tf.setText(f.getAbsolutePath());
        }
    }

    public String getProject() {
        Object o = projectSelector.getSelectedItem();
        if (o == null)
            return "";
        else
            return o.toString();
    }

    public String getBaseline() {
        Object o = baselineSelector.getSelectedItem();
        if (o == null)
            return "";
        else
            return o.toString();
    }

    public String getReqType() {
        if (allTypes.isSelected()) {
            return null;
        } else {
            return (String) typeSelector.getSelectedItem();
        }
    }

    public String getDirectory() {
        Defaults.set("export_file_dir", tf.getText());
        return tf.getText();
    }

    public int getMaxRowCount() {
        if (maxRowCount.getText().length() == 0) {
            return MAX_ROWS;
        }
        else {
            Defaults.set("max_row_count", maxRowCount.getText());
            return Integer.parseInt(maxRowCount.getText());
        }
    }

    public Boolean isExcel() {
        return excel.isSelected();
    }

    public Boolean isReqIF() { return reqIF.isSelected(); }
    public Boolean isReqIFZ() { return reqIFZ.isSelected(); }

    public Boolean isPlaintext() { return plaintext.isSelected(); }
    public Boolean isSkipImages() { return skipImages.isSelected(); }
}
