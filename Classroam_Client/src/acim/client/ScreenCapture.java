package acim.client;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import java.io.*;

import javax.imageio.*;

/**
 * This class provides functionality to capture a screenshot of the user's screen
 * or, in case of failure, generate an error message image.
 * The captured image or error message image is returned as a byte array.
 */
public class ScreenCapture {
	// https://stackoverflow.com/a/9417836
	
	/**
     * Captures a screenshot of the current screen and returns it as a byte array.
     * If capturing the screenshot fails due to security settings or other issues,
     * this method generates a fallback image containing an error message.
     * 
     * @return byte array representing the captured or error image in JPG format
     * @throws HeadlessException if the environment does not support a display
     * @throws AWTException if the screen capture fails at the system level
     */
	public static byte[] getScreencapBytes() throws HeadlessException, AWTException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedImage img;
		
		try {
			// Try taking a screenshot first.
			
			Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
			img = new Robot().createScreenCapture(new Rectangle(screen_size));
		} catch (Exception e) {
			// Display the exception to an image
			// so that the server owner knows the potential cause of the Exception.
			
			String errorMsg = "Unable to capture screenshot.\n\n"
					+ "This might be due to temporary issues or security settings on this computer.\n"
					+ "Please try again later. If the problem persists, contact your IT department\n"
					+ "for assistance.\n\n"
					+ e.getClass().getSimpleName() + ": \n" + e.getLocalizedMessage();
			
			Font font = new Font("Arial", Font.PLAIN, 16);
			int width = 4, height = 4;
			
			// Create a blank BufferedImage for getFontMetrics to work.
			img = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
			Graphics g = img.getGraphics();
			
			// Calculate the required image dimensions to fit the error message.
			for (String line : errorMsg.split("\n")) {
				Rectangle2D rect = g.getFontMetrics(font).getStringBounds(line, g);
				
				width = Math.max(width, (int) rect.getWidth());
				height += rect.getHeight();
			}
			// Extra padding to avoid cutoff.
			height += g.getFontMetrics(font).getHeight();

			// Create the final image with the correct dimensions.
			img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			g = img.getGraphics();
			
			g.setColor(new Color(126, 239, 0));
			g.fillRect(0, 0, img.getWidth(), img.getHeight());
			g.setColor(Color.BLACK);
			g.setFont(font);
			
			int lineNumber = 1;
			for (String line : errorMsg.split("\n")) {
	            g.drawString(line, 2, 2 + lineNumber * g.getFontMetrics().getHeight());
	            lineNumber++;
			}
		}
		try {
			ImageIO.write(img, "jpg", baos);
			baos.flush();
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos.toByteArray();
	}
}
