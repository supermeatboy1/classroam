package acim.net;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.table.*;

import acim.data.*;
import acim.gui.*;

/**
 * Represents a single client connection to the server.
 * 
 * This class manages input/output communication between the server and client,
 * processes client commands like login, messages, screenshots, and monitors
 * session usage time.
 * 
 * Each client has its own threads for reading input, sending output, and monitoring
 * time-based usage limits (account balance).
 */
public class ClientConnection {
	private Socket client;

	private BufferedReader reader;
	private BufferedWriter writer;
	
	private String ipAddress;
	private int port;
	
	private InputThread inThread;
	private OutputThread outThread;
	
	private Queue<String> commandQueue;
	private String currentUser;

	/**
     * Creates a new client connection handler.
     * 
     * @param client The socket connected to the client.
     * @throws IOException If stream initialization fails.
     */
	public ClientConnection(Socket client) throws IOException {
		super();
		this.client = client;

		reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
		
		ipAddress = client.getInetAddress().getHostAddress();
		port = client.getPort();
		
		inThread = new InputThread();
		outThread = new OutputThread();
		
		commandQueue = new LinkedList<String>();
	}
	/**
     * Starts input and output threads to handle communication with the client.
     */
	public void startThreads() {
		inThread.start();
		outThread.start();
	}
	/**
     * Queues a command to be sent to the client.
     * 
     * @param command The command string.
     */
	public void queueCommand(String command) {
		commandQueue.add(command);
	}
	/**
     * Forces the client to be kicked out.
     */
	public void kickout() {
		ClientManager.setClientPanelCurrentUser(ipAddress, "");
		ClientManager.setClientPanelCurrentName(ipAddress, "");
		ClientManager.setClientPanelStatus(ipAddress, ClientPanel.Status.ACTIVE);
	}
	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof ClientConnection))
			return false;

		ClientConnection otherConn = (ClientConnection) other;
		return otherConn.ipAddress.equals(ipAddress);
	}
	
	public String getIpAddress() { return ipAddress; }
	public void setCurrentUser(String newUser) {
		currentUser = newUser;
	}
	public String getCurrentUser() { return currentUser; }

	/**
     * Closes the connection to the client.
     * 
     * @throws IOException If an I/O error occurs.
     */
	public void closeConnection() throws IOException {
		client.close();
	}
	/**
     * Internal method to handle client disconnection and cleanup.
     */
	private void close() {
		try {
			closeConnection();
			ClientManager.removeClientConnection(this);
			System.out.println("Client disconnected: " + client.getInetAddress().getHostAddress() + ":" + client.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
     * Handles receiving and processing data from the client.
     */
	private class InputThread extends Thread {
		@Override
		public void run() {
			try {
				writer.write("Welcome to the server!\r\n");
				while (client.isConnected() && !client.isClosed()) {
					String input = reader.readLine();
					if (input == null) {
						close();
						return;
					}

					if (input.equals("quit") || input.equals("exit")) {
						close();
						break;
					} else if (input.equals("start receive screenshot")) {
						SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy, HH-mm-ss");
						String date_time_str = sdf.format(new Date());
						
						boolean complete = false;
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						Base64.Decoder decoder = Base64.getUrlDecoder();
						
						while (!complete) {
							String line = reader.readLine();
							
							if (line.startsWith("chunk length ")) {
								int chunk_length = Integer.parseInt(line.replaceFirst("chunk length ", ""));
								String chunk_line = "";
								while (chunk_line.trim().length() == 0 || chunk_line.equals("null"))
									chunk_line = reader.readLine();
								byte[] chunk = decoder.decode(chunk_line);
								baos.write(chunk, 0, chunk_length);
							} else if (line.startsWith("stop receive screenshot")) {
								complete = true;
								break;
							}
						}
						baos.flush();
						baos.close();
						
						BufferedImage screenshot = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
						
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								new PictureViewerFrame(screenshot, "[Screenshot] " + date_time_str).setVisible(true);
							}
						});
					} else if (input.startsWith("message ")) {
						// Display the message through a dialog box.
						JOptionPane.showMessageDialog(null, "<html>" + input.replaceFirst("message ", "") + "</html>",
								"Client (" + ipAddress + ":" + port + ") has a message for you!",
								JOptionPane.INFORMATION_MESSAGE);
					} else if (input.startsWith("login ")) {
						String[] stringArray = input.split(" ");
						String clientUsername = stringArray[1];
						String clientEncodedPassword = stringArray[2];

						Account account = DatabaseManager.getAccountByUsername(clientUsername);
						if (account == null) {
							queueCommand("login fail No account exists with that username.");
						} else if (!account.getEncodedPassword().equals(clientEncodedPassword)) {
							queueCommand("login fail Invalid password.");
						} else if (ClientManager.getConnectionFromUsername(clientUsername) != null) {
							queueCommand("login fail This username is currently in use.<br>Please try again later.");
						} else {
							queueCommand("allow access");
							ClientManager.setClientPanelCurrentUser(ipAddress, clientUsername);
							ClientManager.setClientPanelCurrentName(ipAddress,
									account.getFirstName() + " " + account.getLastName());
							ClientManager.setClientPanelStatus(ipAddress, ClientPanel.Status.IN_USE);
							currentUser = clientUsername;
						}
					}
				}
			} catch (Exception e) {
				System.out.println("Exception occured in InputThread (" + ipAddress + "): ");
				e.printStackTrace();
				close();
			}
		}
	}

	/**
     * Sends queued commands to the client.
     */
	private class OutputThread extends Thread {
		private int sentBytes;
		@Override
		public void run() {
			try {
				sentBytes = 0;
				while (client.isConnected() && !client.isClosed()) {
					// Wait until a command is queued.
					while (commandQueue.isEmpty()) {
						try {
							sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					// Empty all queued commands.
					while (!commandQueue.isEmpty()) {
						String command = commandQueue.poll();
						if (command == null)
							continue;
						
						/*
						if (command.equals("kickout") && usageThread != null && !usageThread.hasEnded()) {
							usageThread.interrupt();
						}
						*/
						writer.write(command + "\r\n");
						writer.flush();

						sentBytes += command.length();
						
						// Limit the amount of bytes being sent.
						if (sentBytes > 250000) {
							sleep(500);
							sentBytes = 0;
							System.out.println("Throttling");
						}
					}
				}
			} catch (Exception e) {
				System.out.println("Exception occured in OutputThread (" + ipAddress + "): ");
				e.printStackTrace();
			}
		}
	}
}
