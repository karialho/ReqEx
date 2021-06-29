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

import javax.swing.*;
import java.awt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummaryPanel extends JPanel{

	protected static String outputDir;
	protected static String outputFile;
	protected static int types;
	protected static int exported;

	private final JLabel outputDirL;
	private final JLabel outputFileL;
	private final JLabel typesL;
	private final JLabel exportedL;

    private static final Logger logger = LoggerFactory.getLogger("SummaryPanel");

	SummaryPanel() {
		this.setLayout(new GridBagLayout());

		JLabel summaryLabel = new JLabel("Summary");
		summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 11));

		JLabel outputDirLabel = new JLabel("Output directory:");
		JLabel outputFileLabel = new JLabel("Output file:");
		JLabel typesLabel = new JLabel("Requirement types:");
		JLabel exportedLabel = new JLabel("Exported requirements:");

		outputDirL = new JLabel("0");
		outputFileL = new JLabel("0");
		typesL = new JLabel("0");
		exportedL = new JLabel("0");

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.insets = new Insets(5, 5, 5, 0);
		this.add(summaryLabel, c);

		c = new GridBagConstraints();
		c.gridy = 1;
		c.anchor = GridBagConstraints.LINE_END;
		c.insets = new Insets(5, 5, 0, 0);
		this.add(outputDirLabel, c);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.insets = new Insets(5, 5, 0, 5);
		this.add(outputDirL, c);

		c = new GridBagConstraints();
		c.gridy = 2;
		c.anchor = GridBagConstraints.LINE_END;
		c.insets = new Insets(5, 5, 0, 0);
		this.add(outputFileLabel, c);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 2;
		c.insets = new Insets(5, 5, 0, 5);
		this.add(outputFileL, c);

		c = new GridBagConstraints();
		c.gridy = 3;
		c.anchor = GridBagConstraints.LINE_END;
		c.insets = new Insets(5, 5, 0, 0);
		this.add(typesLabel, c);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 3;
		c.insets = new Insets(5, 5, 0, 5);
		this.add(typesL, c);

		c = new GridBagConstraints();
		c.gridy = 4;
		c.anchor = GridBagConstraints.LINE_END;
		c.insets = new Insets(5, 5, 0, 0);
		this.add(exportedLabel, c);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 4;
		c.insets = new Insets(5, 5, 0, 5);
		this.add(exportedL, c);

	}

	public void setSummary() {
		logger.info("Setting summary.");
		this.outputDirL.setText(outputDir);
		this.outputFileL.setText(outputFile);
		this.typesL.setText(Integer.toString(types));
		this.exportedL.setText(Integer.toString(exported));
	}
}
