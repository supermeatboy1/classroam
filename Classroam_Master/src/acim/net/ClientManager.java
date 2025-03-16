package acim.net;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

import acim.gui.*;

/**
 * This is responsible for maintaining a list of all connected clients,
 * managing their corresponding GUI panels, and providing utility functions
 * to send commands, select clients, and update the UI.
 */
public class ClientManager {
	private static ArrayList<ClientConnection> clientConnections;
	private static ClientConnection selectedClientConnection;
	private static JPanel managerPanel;
	
	/**
     * Initializes the client manager by creating the clientConnections list.
     */
	public static void initialize() {
		clientConnections = new ArrayList<ClientConnection>();
	}
	/**
     * Closes all currently connected client connections.
     * @throws IOException if an error occurs while closing connections.
     */
	public static void forceCloseEverything() throws IOException {
		for (ClientConnection conn : clientConnections) {
			conn.closeConnection();
		}
	}
	/**
     * Adds a new client connection to the manager.
     * If the client already exists (based on IP address), the connection is rejected.
     * 
     * @param client The socket representing the new client connection.
     * @throws IOException if an error occurs while handling the client socket.
     */
	public static void addClient(Socket client) throws IOException {
		// Reject duplicate client connections...
		if (getPanelFromIpAddress(client.getInetAddress().getHostAddress()) != null) {
			client.close();
			return;
		}

		ClientConnection conn = new ClientConnection(client);
		clientConnections.add(conn);
		conn.startThreads();
		
		addClientToPanel(client);
	}
	/**
     * Removes a client connection from the manager.
     * 
     * @param connection The client connection to remove.
     */
	public static void removeClientConnection(ClientConnection connection) {
		if (clientConnections.contains(connection)) {
			clientConnections.remove(connection);
		}
		removeClientFromPanel(connection.getIpAddress());
		
		// Deselect the current client connection just in case it's the same one we're removing.
		if (connection.equals(selectedClientConnection)) {
			selectedClientConnection = null;
		}
	}
	/**
     * Sets the currently selected client connection based on the selected GUI panel.
     * 
     * @param panel The selected ClientPanel.
     */
	public static void setSelectedClientConnection(ClientPanel panel) {
		for (ClientConnection connection : clientConnections) {
			if (connection.getIpAddress().equals(panel.getIpAddress())) {
				selectedClientConnection = connection;
				return;
			}
		}
	}
	/**
     * Queues a command to the currently selected client.
     * 
     * @param command The command to send.
     */
	public static void queueCommandToSelectedConnectionDirect(String command) {
		selectedClientConnection.queueCommand(command);
	}
	/**
     * Checks if there is a selected client connection.
     * If no client is selected, shows a warning message.
     * 
     * @return true if a client is selected, false otherwise.
     */
	public static boolean checkForSelectedConnection() {
		if (selectedClientConnection == null) {
			JOptionPane.showMessageDialog(null,
					"<html>No computer selected!"
					+ "<br>Please select a computer "
					+ "before performing an action.</html>",
					"No computer selected!", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	/**
     * Queues a command to the selected client, after confirming a client is selected.
     * 
     * @param command The command to send.
     */
	public static void queueCommandToSelectedConnection(String command) {
		if (!checkForSelectedConnection()) {
			return;
		}
		queueCommandToSelectedConnectionDirect(command);
	}
	/**
     * Returns the IP address of the currently selected client.
     * 
     * @return IP address of selected client, or null if none is selected.
     */
	public static String getSelectedIpAddress() {
		if (!checkForSelectedConnection()) {
			return null;
		}
		return selectedClientConnection.getIpAddress();
	}
	/**
     * Finds a client connection based on the currently logged-in username.
     * 
     * @param username The username to search for.
     * @return The matching ClientConnection, or null if none found.
     */
	public static ClientConnection getConnectionFromUsername(String username) {
		for (ClientConnection connection : clientConnections) {
			if (connection.getCurrentUser() != null &&
					connection.getCurrentUser().equals(username)) {
				return connection;
			}
		}
		return null;
	}
	
	 // ********************************************************************************************************
    // GUI Management Functions
    // ********************************************************************************************************

    /**
     * Sets the JPanel used to display client panels.
     * 
     * @param panel The panel to use.
     */
	public static void setManagerPanel(JPanel panel) {
		managerPanel = panel;
	}
	/**
     * Finds the ClientPanel corresponding to a given username.
     * 
     * @param username The username to search for.
     * @return The matching ClientPanel, or null if none found.
     */
	public static ClientPanel getPanelFromUser(String username) {
		for (Component c : managerPanel.getComponents()) {
			if (!(c instanceof ClientPanel))
				continue;
			ClientPanel panel = (ClientPanel) c;
			if (panel.getCurrentUser() != null && panel.getCurrentUser().equals(username))
				return panel;
		}
		return null;
	}
	/**
     * Finds the ClientPanel corresponding to a given IP address.
     * 
     * @param ipAddress The IP address to search for.
     * @return The matching ClientPanel, or null if none found.
     */
	public static ClientPanel getPanelFromIpAddress(String ipAddress) {
		for (Component c : managerPanel.getComponents()) {
			if (!(c instanceof ClientPanel))
				continue;
			ClientPanel panel = (ClientPanel) c;
			if (panel.getIpAddress() != null && panel.getIpAddress().equals(ipAddress))
				return panel;
		}
		return null;
	}
	/**
     * Adds a new active client panel to the manager panel.
     * 
     * @param client The client socket to represent.
     */
	public static void addClientToPanel(Socket client) {
		addClientToPanel(client, ClientPanel.Status.ACTIVE);
	}
	/**
     * Adds a new client panel with a specific status.
     * 
     * @param client The client socket.
     * @param status Initial status for the panel.
     */
	public static void addClientToPanel(Socket client, ClientPanel.Status status) {
		if (getPanelFromIpAddress(client.getInetAddress().getHostAddress()) == null) {
			ClientPanel panel = ClientPanel.createPanel(client);
			panel.setStatus(status);
			panel.updateText();
			managerPanel.add(panel);

			// https://stackoverflow.com/a/43267593
			managerPanel.revalidate();
			managerPanel.repaint();
		}
	}
	/**
     * Removes a client panel from the manager panel.
     * 
     * @param ipAddress The IP address of the client to remove.
     */
	public static void removeClientFromPanel(String ipAddress) {
		for (Component c : managerPanel.getComponents()) {
			if (!(c instanceof ClientPanel))
				continue;
			
			ClientPanel panel = (ClientPanel) c;
			
			if (panel.getIpAddress().equals(ipAddress)) {
				managerPanel.remove(panel);
				
				// https://stackoverflow.com/a/43267593
				managerPanel.revalidate();
				managerPanel.repaint();
			}
		}
	}
	/**
     * Sets the displayed name for a client panel.
     * 
     * @param ipAddress The client's IP.
     * @param name The new name.
     */
	public static void setClientPanelCurrentName(String ipAddress, String name) {
		for (Component c : managerPanel.getComponents()) {
			if (!(c instanceof ClientPanel))
				continue;
			
			ClientPanel panel = (ClientPanel) c;
			if (panel.getIpAddress().equals(ipAddress)) {
				panel.setCurrentName(name);
				panel.updateText();
				return;
			}
		}
	}
	/**
     * Sets the displayed username for a client panel.
     * 
     * @param ipAddress The client's IP.
     * @param user The new username.
     */
	public static void setClientPanelCurrentUser(String ipAddress, String user) {
		for (Component c : managerPanel.getComponents()) {
			if (!(c instanceof ClientPanel))
				continue;
			
			ClientPanel panel = (ClientPanel) c;
			if (panel.getIpAddress().equals(ipAddress)) {
				panel.setCurrentUser(user);
				panel.updateText();
				return;
			}
		}
	}
	public static void setClientPanelStatus(String ipAddress, ClientPanel.Status status) {
		for (Component c : managerPanel.getComponents()) {
			if (!(c instanceof ClientPanel))
				continue;
			
			ClientPanel panel = (ClientPanel) c;
			if (panel.getIpAddress().equals(ipAddress)) {
				panel.setStatus(status);
				panel.updateText();
				return;
			}
		}
	}
	/**
     * Resets all client panels' colors and clears the selected client.
     */
	public static void resetCurrentSelectedClient() {
		selectedClientConnection = null;
		
		for (Component c : managerPanel.getComponents()) {
			if (!(c instanceof ClientPanel))
				continue;
			
			ClientPanel panel = (ClientPanel) c;
			panel.resetColors();
		}
	}
}
