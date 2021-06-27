package fi.improveit.req_ex;

import java.awt.*;

import javax.swing.*;

public class ProgressPanel extends JPanel {

    private final JProgressBar progressBar;
    private final JTextArea progressText;


    ProgressPanel() {
        this.setLayout(new GridBagLayout());

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        Font font = new Font("Arial", Font.PLAIN, 12);

        progressText = new JTextArea(8, 45);
        progressText.setFont(font);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 20, 20, 5);
        c.weightx = 0.5;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(progressBar, c);

        c = new GridBagConstraints();
        c.gridy = 1;
        c.weightx = 0.5;
        c.weighty = 1;
        c.anchor = GridBagConstraints.PAGE_START;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.BOTH;
        this.add(progressText, c);


//		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void setMax(int m) {
        progressBar.setMaximum(m);
    }

    public void setProgress(int p) {
        progressBar.setValue(p);
    }

    public void addText(String s) {
        progressText.append(s + "\n");
    }
}
