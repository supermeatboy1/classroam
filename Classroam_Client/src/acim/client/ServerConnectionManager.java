package acim.client;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

import javax.swing.*;

/**
 * This class manages the connection to a server.
 * It prompts the user for the server's IP address and port if they are not saved.
 * Connection information is stored in a file (Server.txt) for future use.
 */
public class ServerConnectionManager {
	/**
     * Asks the user for the server's IP address and port, either by reading from a saved file (Server.txt)
     * or by displaying a dialog box. Attempts to connect to the provided server, and loops until a valid
     * connection is established.
     * 
     * @return A connected Socket object for communicating with the server.
     */
	public static Socket askForTargetSocket() {
		String ipAddress = null;
		int port = 0;

		// Load connection information if available.
		File file = new File("Server.txt");
		try {
			if (file.exists()) {
				Scanner scan;
				scan = new Scanner(file);

				ipAddress = scan.nextLine();				// First line: IP address or hostname
				port = Integer.parseInt(scan.nextLine());	// Second line: Port number

				scan.close();
			}
		} catch (FileNotFoundException e) {
			// This should never happen since file existence is checked.
		}

		Socket socket = null;

		// Loop until a successful connection is established.
		while (socket == null) {
			JTextField txtIpAddress = new JTextField(5);
			JTextField txtPort = new JTextField(5);
			
			try {
				// Pre-fill fields with saved IP and port if available.
				txtIpAddress.setText(ipAddress);
				txtPort.setText("" + port);

				JPanel panel = new JPanel();

				// Layout ensures vertical stacking.
				panel.setLayout(new GridLayout(0, 1));

				if (!file.exists())
					panel.add(new JLabel("It seems this is your first time connecting to a server."));
				panel.add(new JLabel("Please enter server information: "));
				panel.add(Box.createHorizontalStrut(15));
				panel.add(new JLabel("Server IP Address / Hostname: "));
				panel.add(txtIpAddress);
				panel.add(Box.createHorizontalStrut(15));
				panel.add(new JLabel("Server Port: "));
				panel.add(txtPort);

				// Show dialog box.
				if (JOptionPane.showConfirmDialog(null, panel, "Server Information", JOptionPane.OK_CANCEL_OPTION) !=
						JOptionPane.OK_OPTION) {
					// If the user cancels the dialog, exit the program.
					System.exit(0);
				}

				// Read user input.
				ipAddress = txtIpAddress.getText();
				port = Integer.parseInt(txtPort.getText());

				socket = new Socket(ipAddress, port);
				
				// Save/replace connection information.
				if (file.exists())
					file.delete();
				
				Files.write(Paths.get("Server.txt"), (ipAddress + "\r\n" + port + "\r\n"
						).getBytes(), StandardOpenOption.CREATE_NEW);
			}  catch (NumberFormatException e) {
				if (txtIpAddress.getText().trim().isEmpty() ||
						txtPort.getText().trim().isEmpty())
					JOptionPane.showMessageDialog(null, "<html>You have provided empty input/s.<br>Please enter valid input only.<html>", e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
				else
					JOptionPane.showMessageDialog(null, "<html>Your input contains invalid characters.<br>Please enter valid input only.<html>", e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
			} catch (UnknownHostException e) {
				JOptionPane.showMessageDialog(null, "Unknown host error: " + e.getLocalizedMessage(), e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "I/O error: " + e.getLocalizedMessage(), e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Error: " + e.getLocalizedMessage(), e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
			}
		}
		return socket;
	}
}
