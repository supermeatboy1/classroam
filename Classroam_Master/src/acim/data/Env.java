package acim.data;

import java.io.*;
import java.util.*;

public class Env {
	private static HashMap<String, String> envMap;
	public static void load() throws FileNotFoundException, IOException {
		envMap = new HashMap<String, String>();

		try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Ignore empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // Split by the first '='
                int idx = line.indexOf('=');
                if (idx != -1) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    envMap.put(key, value);
                }
            }
        }
	}
	public static String get(String key) {
		return envMap.get(key);
	}
}
