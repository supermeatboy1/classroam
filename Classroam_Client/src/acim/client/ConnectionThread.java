package acim.client;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

/**
 * ConnectionThread handles the main client-server connection logic.
 * It manages reconnection attempts, input and output handling, and
 * communication between the client and the server using two internal threads:
 * InputThread for receiving data and OutputThread for sending data.
 * 
 * It also handles commands received from the server, such as file transfer,
 * login responses, and system control commands such as shutdown, restart, etc.
 */
public class ConnectionThread extends Thread {
	public static final int MAXIMUM_RECONNECTION_TRIES = 60;
	
	private Socket socket;
	private InetSocketAddress addr;
	private BufferedReader reader;
	private BufferedWriter writer;

	private boolean running = false;
	private int connectionTries = 0;
	
	private Queue<String> commandQueue;

	/**
     * Initializes a ConnectionThread with an already-established socket.
     * Also sets up I/O streams and prepares the command queue.
     * 
     * @param s The socket connected to the server.
     * @throws IOException if the socket streams fail to initialize.
     */
	public ConnectionThread(Socket s) throws IOException {
		socket = s;
		addr = (InetSocketAddress) s.getRemoteSocketAddress();

		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

		commandQueue = new LinkedList<String>();
		LockFrame.setCommandQueue(commandQueue);
	}

	/**
     * Safely closes the socket and resets the UI to the lock screen.
     */
	public void closeSocket() {
		try {
			if (!socket.isClosed()) {
				UsageMonitorFrame.hideFrame();
				UsageMonitorFrame.interruptUpdateThread();
				LockFrame.showFrame();
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
     * Main connection loop.
     * 
     * Starts input and output threads to handle communication.
     * When disconnected, it will attempt reconnection up to the
     * maximum allowed attempts.
     */
	@Override
	public void run() {
		running = true;

		while (running) {
			connectionTries = 0;
			
			try {
				new InputThread().start();
				new OutputThread().start();
				
				// Wait until the connection between the server is lost.
				while (socket.isConnected() && !socket.isClosed()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} 
				}
				
				reader.close();
				writer.close();
				closeSocket();
			}catch (Exception e) {
				e.printStackTrace();
				closeSocket();
			}
			
			// Try reconnecting with the server...
			try {
				boolean reconnectionSuccess = false;
				while (!reconnectionSuccess) {
					if (connectionTries >= MAXIMUM_RECONNECTION_TRIES) {
						JOptionPane.showMessageDialog(null,
								"<html>Cannot reconnect to server " + addr.toString() + " after <i>"
									+ MAXIMUM_RECONNECTION_TRIES + "</i> tries. Closing...<html>",
									"Reconnection failed.",
									JOptionPane.ERROR_MESSAGE);
						System.exit(-1);
					}
					
					connectionTries++;
					System.out.println("Trying to reconnect with server... (" + connectionTries + ") " + addr.toString());
					sleep(1000);
				
					try {
						socket = new Socket(addr.getAddress().getHostAddress(), addr.getPort());

						reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
						
						reconnectionSuccess = true;
						
						System.out.println("Reconnection successful.");
					} catch (IOException e) {}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
     * This class handles incoming data from the server.
     * 
     * Processes commands such as login responses, file transfer instructions,
     * system control commands (shutdown/restart), and messages from the server.
     */
	private class InputThread extends Thread {
		@Override
		public void run() {
			try {
				while (socket.isConnected() && !socket.isClosed()) {
					String input = reader.readLine();
					if (input == null) {
						System.out.println("Input from server is null");
						closeSocket();
						return;
					}

					if (input.startsWith("update available seconds ")) {
						long newSeconds = Long.parseLong(input.replaceFirst("update available seconds ", ""));
						UsageMonitorFrame.updateRemainingSeconds(newSeconds);
					} else if (input.startsWith("login fail ")) {
						String failMsg = input.replaceFirst("login fail ", "");
						JOptionPane.showMessageDialog(null,
								"<html>Failed to login: <br>" + failMsg + "<html>",
									"Login failed.",
									JOptionPane.ERROR_MESSAGE);
					} else if (input.equals("kickout")) {
						LockFrame.showFrame();
						UsageMonitorFrame.hideFrame();
					} else if (input.startsWith("allow access ")) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								JOptionPane.showMessageDialog(null,
										"<html>Login successful!<html>",
										"Success!",
										JOptionPane.INFORMATION_MESSAGE);
							}});
						LockFrame.hideFrame();
						UsageMonitorFrame.showFrame();
						long seconds = Long.parseLong(input.replaceFirst("allow access ", ""));
						UsageMonitorFrame.createUpdateThread(seconds);
					} else if (input.startsWith("message ")) {
						String msg = input.replaceFirst("message ", "");
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								JOptionPane.showMessageDialog(null, "<html>Message from server:<br>" + msg + "</html>");
							}
						});
					} else if (input.equals("shutdown")) {
						try {
							SystemCloser.shutdown(false);
						} catch (Exception e) {
							e.printStackTrace();
							commandQueue.add("message " + e.getClass().getSimpleName() + ": Cannot shutdown the target computer.<br>" + e.getLocalizedMessage());
						}
					} else if (input.equals("restart")) {
						try {
							SystemCloser.shutdown(true);
						} catch (Exception e) {
							e.printStackTrace();
							commandQueue.add("message " + e.getClass().getSimpleName() + ": Cannot restart the target computer.<br>" + e.getLocalizedMessage());
						}
					} else if (input.equals("start sending file")) {
						String filename = reader.readLine().replaceFirst("filename ", "");
						File file = new File(filename);
						file.createNewFile();
						FileOutputStream fos = new FileOutputStream(file);
						Base64.Decoder decoder = Base64.getUrlDecoder();
						boolean complete = false;
						
						try {
							while (!complete) {
								String line = reader.readLine();
								if (line.startsWith("cancel sending file")) {
									complete = true;
									fos.flush();
									fos.close();
									file.delete();
									break;
								} else if (line.startsWith("end sending file")) {
									complete = true;
									fos.flush();
									fos.close();
									SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											JOptionPane.showMessageDialog(null, "Received file: " + filename);
										}});
									break;
								} else if (line.startsWith("chunk length ")) {
									int chunk_length = Integer.parseInt(line.replaceFirst("chunk length ", ""));
									byte[] chunk = decoder.decode(reader.readLine());
									fos.write(chunk, 0, chunk_length);
								}
							}
						} catch (NullPointerException e) {
							fos.flush();
							fos.close();
						}
					} else if (input.equals("request screenshot")) {
						commandQueue.add("start receive screenshot");
						ByteArrayInputStream bais = new ByteArrayInputStream(ScreenCapture.getScreencapBytes());
						byte[] buffer = new byte[512];
						long read_bytes = 0;
						Base64.Encoder encoder = Base64.getUrlEncoder();
						while ((read_bytes = bais.read(buffer)) > 0) {
							String encoded_string = encoder.encodeToString(buffer);
							commandQueue.add("chunk length " + read_bytes);
							commandQueue.add(encoded_string);
						}
						commandQueue.add("stop receive screenshot");
					}
				}
			} catch (Exception e) {
				System.out.println("Exception occured in InputThread: ");
				e.printStackTrace();
				closeSocket();
			}
		}
	}

	/**
     * Handles outgoing data to the server.
     * 
     * Continuously checks for new commands in the queue and sends them.
     */
	private class OutputThread extends Thread {
		@Override
		public void run() {
			try {
				while (socket.isConnected() && !socket.isClosed()) {
					// Wait until a command is queued.
					try {
						sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					// Empty all queued commands
					while (!commandQueue.isEmpty()) {
						String command = commandQueue.poll();
						if (command == null)
							continue;
						
						writer.write(command + "\r\n");
						writer.flush();
					}
				}
			} catch (Exception e) {
				System.out.println("Exception occured in OutputThread: ");
				e.printStackTrace();
			}
		}
	}
}
