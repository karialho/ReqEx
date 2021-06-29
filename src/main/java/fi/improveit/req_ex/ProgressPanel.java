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
