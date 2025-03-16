package acim.client;

import java.io.*;

/**
 * This class contains the function
 * neccessary for the client computer to shutdown or restart.
 */
public class SystemCloser {
	// Original function code by David Crow (2008).
	// Edited by Suraj Shingade (2022).
	// https://stackoverflow.com/a/25666
	public static void shutdown(boolean restart) throws RuntimeException, IOException {
	    String shutdownCommand;
	    String operatingSystem = System.getProperty("os.name");

	    if ("Linux".equals(operatingSystem) || "Mac OS X".equals(operatingSystem)) {
	        shutdownCommand = "shutdown -" + (restart ? "r" : "h") + " now";
	    }
	    else if (operatingSystem.contains("Windows")) {
	        shutdownCommand = "shutdown.exe -" + (restart ? "r" : "s") +  " -t 0";
	    }
	    else {
	        throw new RuntimeException("Unsupported operating system.");
	    }

	    Runtime.getRuntime().exec(shutdownCommand);
	    System.exit(0);
	}
}
