package acim.client;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

/**
 * LockFrame is a full-screen JFrame that displays a login panel.
 * It acts as a lock screen requiring the user to enter a username and password.
 * The login credentials are encoded and added to a command queue for processing.
 */
public class LockFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private Queue<String> commandQueue;

	/**
     * Creates a new LockFrame instance, sets it to fullscreen,
     * and displays a login panel. The frame is undecorated
     * and centered on the screen.
     */
	private LockFrame() {
		super();

		JPanel contentPane = new JPanel();
		Dimension size = Toolkit.getDefaultToolkit().getScreenSize();

		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setMinimumSize(size);
		contentPane.setMaximumSize(size);
		contentPane.setPreferredSize(size);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {}
			@Override
			public void windowClosing(WindowEvent e) {}
			@Override
			public void windowClosed(WindowEvent e) {
				System.exit(0);
			}
			@Override
			public void windowIconified(WindowEvent e) {}
			@Override
			public void windowDeiconified(WindowEvent e) {}
			@Override
			public void windowActivated(WindowEvent e) {}
			@Override
			public void windowDeactivated(WindowEvent e) {}			
		});
		
		// https://stackoverflow.com/a/20913713
		// To center the internal panel:
		contentPane.setLayout(new GridBagLayout());
		
		getContentPane().add(contentPane, BorderLayout.CENTER);
		JPanel internal = new LoginPanel();
		contentPane.add(internal);
		
		// https://stackoverflow.com/a/17237207
		setUndecorated(true);
		pack();
		
		setResizable(false);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	/**
     * Inner class representing the login panel displayed within the LockFrame.
     * It contains fields for username and password, and a submit button.
     */
	public class LoginPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private JTextField txtUsername;
		private JPasswordField pwdPassword;
		private JButton btnSubmit;

		/**
         * Constructs a login panel with username and password fields,
         * along with a submit button that sends credentials to the command queue.
         */
		public LoginPanel() {
			setLayout(new GridLayout(0, 2, 8, 8));

			add(new JLabel("A.C.I.M. Login"));
			add(new JLabel(" "));
			
			JLabel lblUsername = new JLabel("Username");
			add(lblUsername);
			
			txtUsername = new JTextField();
			add(txtUsername);
			txtUsername.setColumns(10);
			
			JLabel lblPassword = new JLabel("Password");
			add(lblPassword);
			
			pwdPassword = new JPasswordField();
			add(pwdPassword);
			
			add(new JLabel(" "));
			add(new JLabel(" "));
			add(new JLabel(" "));
			
			btnSubmit = new JButton("Submit");
			btnSubmit.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (txtUsername.getText() == null || txtUsername.getText().trim().length() == 0) {
						JOptionPane.showMessageDialog(null,
								"<html>Username is blank.<html>",
									"Missing input",
									JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (pwdPassword.getText() == null || pwdPassword.getText().trim().length() == 0) {
						JOptionPane.showMessageDialog(null,
								"<html>Password is blank.<html>",
									"Missing input",
									JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					commandQueue.add("login " + txtUsername.getText() + " " +
						Base64.getUrlEncoder().encodeToString(pwdPassword.getText().getBytes())
					);
				}
			});
			add(btnSubmit);
		}
	}
	
	private static LockFrame frame;
	private static PersistenceThread persistThread;
	
	/**
     * Initializes and creates the LockFrame.
     */
	public static void initialize() {
		frame = new LockFrame();
	}
	/**
     * Sets the command queue where login commands will be added.
     * @param queue the queue to set
     */
	public static void setCommandQueue(Queue<String> queue) {
		frame.commandQueue = queue;
	}
	/**
     * Makes the LockFrame visible and starts the persistence thread.
     */
	public static void showFrame() {
		frame.setVisible(true);
		frame.setAlwaysOnTop(true);

		persistThread = new PersistenceThread(frame);
		persistThread.start();
	}
	/**
     * Hides the LockFrame and stops the persistence thread if it's running.
     */
	public static void hideFrame() {
		if (persistThread != null)
			persistThread.stopPersisting();
		frame.setAlwaysOnTop(false);
		frame.setVisible(false);
	}
}
