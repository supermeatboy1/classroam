package acim.client;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * This is responsible for preventing certain key combinations like Alt, Ctrl, Windows, and Tab
 * from interfering with a locked screen by continuously releasing these keys.
 * It runs in a loop until stopped.
 */
public class PersistenceThread extends Thread {
	private boolean persisting = false;
	private JFrame frame;
	
	/**
     * Creates a PersistenceThread linked to the given JFrame.
     * @param frame The JFrame to keep persistent (typically a lock screen frame).
     */
	public PersistenceThread(JFrame frame) {
		this.frame = frame;
		persisting = true;
	}
	
	/**
     * Continuously releases special keys like Alt, Ctrl, Windows, and Tab to
     * block certain system shortcuts.
     * This loop runs until persisting is set to false.
     */
	public void run() {
		Robot robot;
		try {
			robot = new Robot();
			while (persisting) {
				robot.keyRelease(KeyEvent.VK_ALT);
				robot.keyRelease(KeyEvent.VK_CONTROL);
				robot.keyRelease(KeyEvent.VK_WINDOWS);
				robot.keyRelease(KeyEvent.VK_TAB);
				// Can hinder the user's ability to type information.
				// Originally intended to force focus on the frame.
				// frame.requestFocusInWindow();
				Thread.sleep(50);
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Error: " + e.getLocalizedMessage(), e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
			return;
		}
	}
	
	public void stopPersisting() { persisting = false; }
}
