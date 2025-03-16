package acim.client;

import java.awt.*;
import java.net.*;

import javax.swing.*;

import com.formdev.flatlaf.*;

public class Main {
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(new FlatDarkLaf());
					new DNSServer().start();

					LockFrame.initialize();
					Socket socket = ServerConnectionManager.askForTargetSocket();
					ConnectionThread connThread = new ConnectionThread(socket);
					connThread.start();
					
					LockFrame.showFrame();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
