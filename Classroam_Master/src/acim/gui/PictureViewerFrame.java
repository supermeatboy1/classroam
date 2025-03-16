package acim.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.border.*;

public class PictureViewerFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	
	public PictureViewerFrame(BufferedImage img, String name) {
		int scaled_w = (int) (img.getWidth() * 0.65f);
		int scaled_h = (int) (img.getHeight() * 0.65f);
		BufferedImage scaled_img = new BufferedImage(
				scaled_w,
				scaled_h,
				BufferedImage.TYPE_3BYTE_BGR
			);
		Graphics g = scaled_img.getGraphics();
		g.drawImage(img.getScaledInstance(scaled_w, scaled_h, Image.SCALE_SMOOTH), 0, 0, null);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout(0, 0));
		contentPane.setBorder(new EmptyBorder(16, 16, 16, 16));
		
		contentPane.add(new JLabel(new ImageIcon(scaled_img)), BorderLayout.NORTH);
		contentPane.add(Box.createVerticalStrut(16), BorderLayout.CENTER);
		
		setResizable(false);
		setTitle(name);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setContentPane(contentPane);
		pack();
		setLocationRelativeTo(null);
	}
}
