package acim.gui;

import java.io.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.formdev.flatlaf.*;

import acim.data.*;
import acim.net.*;

public class MainWindow {
	private JFrame frame;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Env.load();
					WebsiteChecker.load();

					UIManager.setLookAndFeel(new FlatDarkLaf());
					
					ServerThread.startServer();
					ClientManager.initialize();

					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public MainWindow() throws IOException {
		initialize();
	}

	private void initialize() throws IOException {
		frame = new JFrame();
		frame.setSize(1124, 600);
		frame.setTitle("Access Control Inventory Manager");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {}

			@Override
			public void windowClosing(WindowEvent e) {
				ServerThread.stopServer();
				try {
					ClientManager.forceCloseEverything();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}

			@Override
			public void windowClosed(WindowEvent e) {}
			@Override
			public void windowIconified(WindowEvent e) {}
			@Override
			public void windowDeiconified(WindowEvent e) {}
			@Override
			public void windowActivated(WindowEvent e) {}
			@Override
			public void windowDeactivated(WindowEvent e) {}
		});
		
		frame.setLocationRelativeTo(null);
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frame.getContentPane().add(tabbedPane);

		ComputerManagerPanel panelComputerManager = new ComputerManagerPanel();
		tabbedPane.addTab("Computer Manager", null, panelComputerManager, null);
		JPanel panelRegistredAccounts = new RegisteredAccountsPanel();
		tabbedPane.addTab("Account Manager", null, panelRegistredAccounts, null);
		
		ClientManager.setManagerPanel(panelComputerManager.getComputerListPanel());
	}
}
