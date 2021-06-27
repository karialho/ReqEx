package fi.improveit.req_ex;

import javax.swing.ImageIcon;

public class Util {

	/** Returns an ImageIcon, or null if the path was invalid. */
	static protected ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = fi.improveit.req_ex.Util.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
	}

}
