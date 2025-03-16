package acim.client;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.JProgressBar;
import javax.swing.JTextPane;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * This class represents a small frame that displays the remaining usage time
 * for a client computer in an internet cafe system.
 * It shows a countdown timer and changes color when time is almost up.
 */
public class UsageMonitorFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JLabel lblTimeRemaining;
	private static Color defaultLabelTextColor;

	private UsageMonitorFrame() {
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setSize(240, 96);
		setLocation(0, 24);
		setUndecorated(true);
		setAlwaysOnTop(true);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		lblTimeRemaining = new JLabel("");
		defaultLabelTextColor = lblTimeRemaining.getForeground();
		lblTimeRemaining.setHorizontalAlignment(SwingConstants.CENTER);
		lblTimeRemaining.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblTimeRemaining, BorderLayout.CENTER);
	}
	
	private static UsageMonitorFrame frame;
	private static UsageUpdateThread thread;

	/**
     * Shows the usage monitor frame. If the frame does not exist, it creates one.
     * This also resets the label color to its default color.
     */
	public static void showFrame() {
		if (frame == null)
			frame = new UsageMonitorFrame();
		frame.setVisible(true);
		frame.lblTimeRemaining.setForeground(defaultLabelTextColor);
	}

	/**
     * Hides the usage monitor frame, and interrupts the timer update thread if it's running.
     * This is used when the session ends or the monitor is no longer needed.
     */
	public static void hideFrame() {
		if (frame == null)
			return;
		frame.setVisible(false);
		frame.lblTimeRemaining.setForeground(defaultLabelTextColor);
		interruptUpdateThread();
	}
	
	/**
     * Creates and starts a new update thread that counts down from the given time in seconds.
     * This also ensures any existing thread is stopped before creating a new one.
     *
     * @param seconds the initial time to count down from in seconds
     */
	public static void createUpdateThread(long seconds) {
		interruptUpdateThread();
		
		thread = new UsageUpdateThread(seconds);
		thread.start();
	}

	/**
     * Stops the current update thread if it's running.
     */
	public static void interruptUpdateThread() {
		if (thread != null && !thread.isInterrupted())
			thread.interrupt();
	}

	/**
     * Updates the time remaining in the currently running update thread.
     *
     * @param newSeconds the new countdown time (in seconds)
     */
	public static void updateRemainingSeconds(long newSeconds) {
		if (thread != null) {
			thread.setSeconds(newSeconds);
		}
	}

	/**
     * Internal thread class responsible for updating the remaining time and
     * updating the label on the frame. It counts down the seconds and updates the
     * label each second.
     */
	private static class UsageUpdateThread extends Thread {
		private long seconds, startMillis, endMillis, secondsRemaining;

		/**
         * Initializes the thread with the starting time.
         * @param beginSeconds initial countdown time in seconds
         */
		private UsageUpdateThread(long beginSeconds) {
			setSeconds(beginSeconds);
		}
		/**
         * Updates the countdown time and recalculates start and end times.
         * @param newSeconds new countdown time in seconds
         */
		private void setSeconds(long newSeconds) {
			seconds = newSeconds;
			startMillis = System.currentTimeMillis();
			secondsRemaining = seconds - 3;
			endMillis = startMillis + (seconds * 1000);
		}
		/**
         * Main run loop that updates the remaining time label every second,
         * and handles color changes when the remaining time is low.
         */
		@Override
		public void run() {
			try {
				while (System.currentTimeMillis() <= endMillis) {
					sleep(1000);

					long minutesTime = Math.floorDiv(secondsRemaining, 60);
					long hoursTime = Math.floorDiv(minutesTime, 60);
					minutesTime %= 60;
					long secondsTime = secondsRemaining % 60;
					
					if (secondsRemaining <= 60) {
						float normalTextBrightness = secondsRemaining / 60.0f;
						normalTextBrightness *= normalTextBrightness;
						float redBrightness = 1.0f - normalTextBrightness;
						Color color = new Color(
							(int) Math.max((redBrightness * 255) +
									(normalTextBrightness * defaultLabelTextColor.getRed()), 255),
							(int) (normalTextBrightness * defaultLabelTextColor.getGreen()),
							(int) (normalTextBrightness * defaultLabelTextColor.getBlue())
						);
						frame.lblTimeRemaining.setForeground(color);
					} else {
						frame.lblTimeRemaining.setForeground(defaultLabelTextColor);
					}
					
					String hold = "<html>Time remaining is<br>";
					if (hoursTime > 0)
						hold += hoursTime + " hours,<br>";
					
					hold += minutesTime + " minutes and<br>" + secondsTime + " seconds.</html>";

					frame.lblTimeRemaining.setText(hold);
					if (secondsRemaining > 0)
						secondsRemaining--;
					
					frame.setLocation(0, 24);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
