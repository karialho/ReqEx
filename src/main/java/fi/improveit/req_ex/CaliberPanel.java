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

public class CaliberPanel extends JPanel {

	private final JTextField server;
	private final JTextField user;
	private final JPasswordField password;

	CaliberPanel() {
		this.setLayout(new GridBagLayout());

		JLabel serverLabel = new JLabel("Caliber server: ");
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 0, 0);
		c.anchor = GridBagConstraints.LINE_END;
		this.add(serverLabel, c);

		server = new JTextField(15);
		server.setText(Defaults.get("host"));
		c = new GridBagConstraints();
		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 0, 90);
		this.add(server, c);

		JLabel userLabel = new JLabel("Username: ");
		c = new GridBagConstraints();
		c.gridy = 1;
		c.anchor = GridBagConstraints.LINE_END;
		c.insets = new Insets(5, 5, 0, 0);
		this.add(userLabel, c);

		user = new JTextField(15);
		user.setText(Defaults.get("user"));
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 0, 90);
		this.add(user, c);

		JLabel passwordLabel = new JLabel("Password: ");
		c = new GridBagConstraints();
		c.gridy = 2;
		c.anchor = GridBagConstraints.LINE_END;
		c.insets = new Insets(5, 5, 5, 0);
		this.add(passwordLabel, c);

		password = new JPasswordField(15);
		password.setText(Defaults.get("password"));
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 90);
		this.add(password, c);
	}

	public String getServer() {
		return server.getText();
	}

	public String getUser() {
		return user.getText();
	}

	public char[] getPassword() {
		return password.getPassword();
	}

}
