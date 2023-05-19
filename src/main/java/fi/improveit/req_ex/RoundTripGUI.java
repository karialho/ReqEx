/*
 * Copyright 2021. ImproveIt Oy
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package fi.improveit.req_ex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

public class RoundTripGUI implements ActionListener, PropertyChangeListener {

    final static String VERSION = "1.1";
    private final JButton prev;
    private final JButton next;
    private final JButton cancel;
    private final JButton about;
    private final JFrame frame;
    private final JPanel cards;
    protected CaliberSession cs;
    private final CaliberPanel cp;
    protected ProgressPanel progressPanel;
    protected ExportPanel ep;
    private final SummaryPanel sp;
    private int step = 0;
    private final JLabel stepLabel;
    private Export exp;

    private int maxRows;                // max rows to export per type

    static protected RoundTripGUI theGUI;

    private static final Logger logger = LoggerFactory.getLogger("RoundTripGUI");

    final static String[] PANEL_NAME =
            {"CaliberPanel", "ExportPanel", "ProgressPanel", "SummaryPanel"};

    final static String[] STEP_LABEL =
            {"Step 1: Log into CaliberRM server",
                    "Step 2: Select export options",
                    "Step 3: Exporting",
                    "Step 4: Review export results"};

    protected ExportTask exportTask = null;

    class ExportTask extends SwingWorker<Boolean, String> {

        /*
         * Worker task for export. Executed in background thread.
         */
        @Override
        public Boolean doInBackground() {
            publish("Starting the export process.");
            setProgress(0);
            try {
                exp.init();
                publish("Export file created.");
                setProgress(10);

                exp.exportDefinitions();
                publish("Definitions exported.");
                setProgress(20);
                exp.exportRequirements();    // will update progress within from 20-80
                publish("Requirements exported.");

                exp.exportTraces();
                publish("Traces exported.");
                setProgress(90);
                exp.exportSpecifications();

                setProgress(95);
                exp.finish();
                publish("Export finished.");

            } catch (ExportException ex) {
                logger.error("ExportException", ex);
            }
            setProgress(100);
            logger.info("Export finished.");
            return true;
        }

        @Override
        protected void process(List<String> chunks) {
            for (String s : chunks) {
                progressPanel.addText(s);
            }
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            progressPanel.setProgress(100);
            logger.info("ExportTask done!");
        }
    }

    // Invoked when task's progress property changes.
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressPanel.setProgress(progress);
            logger.info("progress: {}", progress);
        }
    }

    public RoundTripGUI() {
        // Create and set up the window.
        frame = new JFrame("Requirement Exchange Wizard V. " + VERSION);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //JLabel logo = new JLabel(Util.createImageIcon("/images/ImproveIt-logo-tp-xs.png"));
        JLabel logo = new JLabel("ReqEx");
        logo.setFont(new Font("SansSerif", Font.BOLD, 20));

        stepLabel = new JLabel(STEP_LABEL[0]);
        stepLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        JPanel topPane = new JPanel();
        topPane.setLayout(new BoxLayout(topPane, BoxLayout.Y_AXIS));

        topPane.add(logo);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPane.add(stepLabel);
        stepLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPane.add(Box.createRigidArea(new Dimension(0, 5)));
        topPane.add(new JSeparator(SwingConstants.HORIZONTAL));
        topPane.setBackground(Color.lightGray);

        JPanel bottomPane = new JPanel();
        bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.Y_AXIS));
        bottomPane.setBackground(Color.lightGray);

        JPanel buttonPane = new JPanel(); // flowlayout
        buttonPane.setBackground(Color.lightGray);
        prev = new JButton("< Previous");
        prev.setEnabled(false);
        next = new JButton("Next >");
        cancel = new JButton("Cancel");
        about = new JButton("About...");
        prev.addActionListener(this);
        next.addActionListener(this);
        cancel.addActionListener(this);
        about.addActionListener(this);
        buttonPane.add(prev);
        buttonPane.add(next);
        buttonPane.add(cancel);
        buttonPane.add(about);

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        bottomPane.add(separator);
        bottomPane.add(buttonPane);

        cp = new CaliberPanel();
        progressPanel = new ProgressPanel();
        ep = new ExportPanel();
        sp = new SummaryPanel();

        cards = new JPanel(new CardLayout());
        cards.add(cp, PANEL_NAME[0]);
        cards.add(ep, PANEL_NAME[1]);
        cards.add(progressPanel, PANEL_NAME[2]);
        cards.add(sp, PANEL_NAME[3]);

        Container pane = frame.getContentPane();
        pane.add(topPane, BorderLayout.PAGE_START);
        pane.add(cards, BorderLayout.CENTER);
        pane.add(bottomPane, BorderLayout.PAGE_END);

        CardLayout cl = (CardLayout) (cards.getLayout());
        cl.show(cards, PANEL_NAME[step]);
        stepLabel.setText(STEP_LABEL[step]);

        frame.getRootPane().setDefaultButton(next);
        frame.pack();
        frame.setVisible(true);

        theGUI = this;
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == next) {
            try {
                logger.info("NEXT pressed in step: {}", step);
                switch (step) {
                    case 0: // initial screen = CaliberPanel (login)
                        cs = new CaliberSession(cp.getServer(), cp.getUser(), cp
                                .getPassword());
                        logger.info("Successfully logged into Caliber server.");
                        prev.setEnabled(true);
                        Defaults.set("host", cp.getServer());
                        Defaults.set("user", cp.getUser());
                        Defaults.set("password", new String(cp.getPassword()));
                        Defaults.store();
                        ep.init(cs);
                        break;
                    case 1: // FilePanel
                        if (ep.isExcel()) {
                            if (ep.getReqType() == null) {
                                logger.info("Starting Excel project export");
                                exp = new ExcelExportProject(cs, ep.getProject(), ep.getReqType(), ep.getDirectory(),
                                        ep.getMaxRowCount(), ep, progressPanel);
                            }
                            else {
                                logger.info("Starting Excel requirement type export");
                                exp = new ExcelExportType(cs, ep.getProject(), ep.getReqType(), ep.getDirectory(),
                                        ep.getMaxRowCount(), ep, progressPanel);
                            }
                        } else {
                            if (ep.getReqType() == null) {
                                logger.info("Starting ReqIF project export");
                                exp = new ReqIFExportProject(cs, ep.getProject(), ep.getReqType(), ep.getDirectory(),
                                        ep.getMaxRowCount(), ep, progressPanel);
                            }
                            else {
                                logger.info("Starting ReqIF requirement type export");
                                exp = new ReqIFExportType(cs, ep.getProject(), ep.getReqType(), ep.getDirectory(),
                                        ep.getMaxRowCount(), ep, progressPanel);
                            }
                        }
                        maxRows = ep.getMaxRowCount();
                        logger.info("Max rows: {}", maxRows);
                        prev.setEnabled(false);
                        exportTask = new ExportTask();
                        exportTask.addPropertyChangeListener(this);
                        cancel.setEnabled(false);
                        logger.info("Ready to execute export");
                        exportTask.execute();
                        break;
                    case 2: // Summary panel
                        sp.setSummary();
                        next.setText("Finish");
                        break;
                    case 3: // Finish
                        cs.logout();
                        Defaults.store();
                        System.exit(0);
                        break;
                }
                step++;
            } catch (CaliberException | ExportException e) {
                JOptionPane.showMessageDialog(frame, e.getMessage(),
                        "ReqEx Error", JOptionPane.ERROR_MESSAGE);
                System.out.println(e.getMessage());
            }
        } else if (event.getSource() == prev) {
            System.out.println("Prev button");
            switch (step) {
                case 1:
                    prev.setEnabled(false);
                    break;
                case 2:
                    next.setText("Next >");
                    break;
            }
            next.setEnabled(true);
            step--;
        } else if (event.getSource() == cancel) {
            System.exit(0);
        } else if (event.getSource() == about) {
            String msg = "ReqEx " + VERSION + "\n\n" +
                    "Copyright 2021 ImproveIt Oy\n\n" +
                    "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                    "you may not use this file except in compliance with the License.\n" +
                    "You may obtain a copy of the License at\n\n" +

                    "http://www.apache.org/licenses/LICENSE-2.0\n\n" +

                    "Unless required by applicable law or agreed to in writing, software\n" +
                    "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                    "See the License for the specific language governing permissions and\n" +
                    "limitations under the License.\n\n" +

                    "Code repo and instructions: https://github.com/karialho/ReqEx";

            JOptionPane.showMessageDialog(frame, msg,
                    "ReqEx Information", JOptionPane.INFORMATION_MESSAGE);
        }
        CardLayout cl = (CardLayout) (cards.getLayout());
        cl.show(cards, PANEL_NAME[step]);
        logger.info("Showing card for step #: {}", step);
        stepLabel.setText(STEP_LABEL[step]);
        frame.getRootPane().setDefaultButton(next);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (UnsupportedLookAndFeelException | IllegalAccessException
                | InstantiationException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                Defaults.load();
                new RoundTripGUI();
            }
        });
    }

}
