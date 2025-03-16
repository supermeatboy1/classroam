package acim.net;

import java.io.*;
import java.net.*;

import javax.swing.JOptionPane;

/**
 * ServerThread class responsible for managing a simple server socket.
 * This class can start a server, accept incoming client connections,
 * and stop the server when needed.
 */
public class ServerThread {
	private static boolean serverRunning = false;
	private static ServerSocket server;
	
	/**
     * Starts the server on port 9600 and begins accepting client connections.
     * This method launches a new background thread to handle the server socket.
     * The server will continue running until `stopServer()` is called.
     */
	public static void startServer() {
		serverRunning = true;
		
		new Thread() {
			public void run() {
				// Try starting the ServerSocket...
				try (ServerSocket server = new ServerSocket(9600)) {
					System.out.println("Server is now listening on: " + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());
					
					while (serverRunning) {
						Thread.sleep(1000);
						
						Socket socket = server.accept();
						System.out.println("New client connected: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
						
						ClientManager.addClient(socket);
					}
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, "Server creation error: " + e.getLocalizedMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
					System.exit(-1);
					return;
				} catch (InterruptedException e) {}
			}
		}.start();
		
		System.out.println("Server starting...");
	}
	/**
     * Stops the running server. This method sets the running flag to false
     * and closes the server socket to unblock any waiting `accept()` call.
     */
	public static void stopServer() {
		System.out.println("Server stopping...");
		serverRunning = false;
	}
}
