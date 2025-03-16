package acim.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import acim.net.*;

/**
 * A panel that manages and displays a list of connected computers
 * along with a set of control buttons to send commands to the selected computer.
 * This panel acts as the primary interface for interacting with remote clients.
 */
public class ComputerManagerPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private JPanel panelComputerList = new JPanel();	
	public JPanel getComputerListPanel() { return panelComputerList; }

    /**
     * Constructs the ComputerManagerPanel, initializing the layout and 
     * adding the control buttons and the computer list display.
     */
	public ComputerManagerPanel() {
		JPanel thisPanel = this;

		setLayout(new GridLayout(0, 1, 0, 0));

		JSplitPane splitPaneComputerManager = new JSplitPane();
		splitPaneComputerManager.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPaneComputerManager.setResizeWeight(0.85);
		add(splitPaneComputerManager);

		JScrollPane scrollPaneComputers = new JScrollPane();
		scrollPaneComputers.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneComputers.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		splitPaneComputerManager.setLeftComponent(scrollPaneComputers);

		panelComputerList = new JPanel();
		scrollPaneComputers.setViewportView(panelComputerList);
		panelComputerList.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
		panelComputerList.setLayout(new GridLayout(2, 2, 16, 16));
		panelComputerList.add(ClientPanel.createLocalPanel());

		JPanel panelComputerControl = new JPanel();
		splitPaneComputerManager.setRightComponent(panelComputerControl);
		panelComputerControl.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
		panelComputerControl.setLayout(new GridLayout(2, 2, 16, 16));

		JButton btnMessage = new JButton("Message");
		btnMessage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String ipAddress = ClientManager.getSelectedIpAddress();
				if (ipAddress == null)
					return;

				String msg = JOptionPane.showInputDialog(thisPanel, "Enter a message for " + ipAddress + ".");
				ClientManager.queueCommandToSelectedConnection("message " + msg);
			}
		});
		panelComputerControl.add(btnMessage);

		JButton btnSendFile = new JButton("Send File");
		btnSendFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!ClientManager.checkForSelectedConnection())
					return;
				
				JFileChooser chooser = new JFileChooser();
				if (chooser.showOpenDialog(thisPanel) != JFileChooser.APPROVE_OPTION)
					return;

				File file = chooser.getSelectedFile();

				System.out.println("Sending file " + file.getAbsolutePath());

				ClientManager.queueCommandToSelectedConnection("start sending file");
				ClientManager.queueCommandToSelectedConnectionDirect("filename " + file.getName());

				FileInputStream fis;
				try {
					fis = new FileInputStream(file);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
					return;
				}

				// Create a progress window
				JFrame progressFrame = new JFrame("File operation");
				JPanel panel = new JPanel();
				JLabel lblProgress = new JLabel("Sending file " + file.getName() + "...");
				panel.add(lblProgress);

				progressFrame.setLocationRelativeTo(thisPanel);
				progressFrame.add(panel);
				progressFrame.pack();
				progressFrame.setResizable(false);
				progressFrame.setVisible(true);
				progressFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				
				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
					@Override
					protected Void doInBackground() throws Exception {

						byte[] buffer = new byte[512];
						long read_bytes = 0;
						try {
							Base64.Encoder encoder = Base64.getUrlEncoder();
							while ((read_bytes = fis.read(buffer)) > 0) {
								String encoded_string = encoder.encodeToString(buffer);
								ClientManager.queueCommandToSelectedConnectionDirect("chunk length " + read_bytes);
								ClientManager.queueCommandToSelectedConnectionDirect(encoded_string);
								progressFrame.requestFocus();
							}
							ClientManager.queueCommandToSelectedConnectionDirect("end sending file");
							
							fis.close();
						} catch (IOException e1) {
							e1.printStackTrace();
							ClientManager.queueCommandToSelectedConnectionDirect("cancel sending file");
						}
						progressFrame.dispose();
						return null;
					}
				};

				worker.execute();
			}
		});
		panelComputerControl.add(btnSendFile);

		JButton btnScreenCapture = new JButton("Screenshot");
		btnScreenCapture.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ClientManager.queueCommandToSelectedConnection("request screenshot");
			}
		});
		panelComputerControl.add(btnScreenCapture);

		JButton btnKickOut = new JButton("Kick Out");
		btnKickOut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!ClientManager.checkForSelectedConnection()) {
					return;
				}
				ClientManager.queueCommandToSelectedConnection("kickout");
				ClientManager.setClientPanelStatus(ClientManager.getSelectedIpAddress(), ClientPanel.Status.ACTIVE);
			}
		});
		panelComputerControl.add(btnKickOut);

		JButton btnShutdown = new JButton("Shutdown");
		btnShutdown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ClientManager.queueCommandToSelectedConnection("shutdown");
			}
		});
		panelComputerControl.add(btnShutdown);

		JButton btnRestart = new JButton("Restart");
		btnRestart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ClientManager.queueCommandToSelectedConnection("restart");
			}
		});
		panelComputerControl.add(btnRestart);
	}
}
