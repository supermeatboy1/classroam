package acim.data;

import java.io.*;
import java.util.*;

import acim.data.structure.Node;

public class WebsiteChecker {
	private static ArrayList<String> normalList;
	private static Node trieRoot;
	public static void load() {
		trieRoot = new Node();
		normalList = new ArrayList<String>();
		
		System.out.println("Reading website ban list...");
		try (BufferedReader reader = new BufferedReader(new FileReader(Env.get("BAN_LIST_FILE")))) {
            String line;
            while ((line = reader.readLine()) != null) {
            	if (!line.startsWith("#") && !line.isEmpty()) {
            		normalList.add(line);
        			Node currentChild = trieRoot;
        			for (int i = 0; i < line.length(); i++) {
        				currentChild = currentChild.createChildIfNotExist(line.charAt(i));
        				if (i == line.length() - 1) {
        					currentChild.markAsEnd();
        				}
        			}
            	}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		benchmark();
	}
	public static void benchmark() {
		System.out.println();
		long startTime = System.nanoTime();
		boolean result = normalList.contains("pornhub.com");
		long endTime = System.nanoTime();
		System.out.println("ArrayList lookup result (should be true):\t" + result);
		long arrayTrueTime = endTime - startTime;
		System.out.println("ArrayList lookup duration in nanoseconds:\t" + arrayTrueTime);
		startTime = System.nanoTime();
		result = trieRoot.entryExists("pornhub.com");
		endTime = System.nanoTime();
		System.out.println("Trie lookup check result (should be true):\t" + result);
		long trieTrueTime = endTime - startTime;
		System.out.println("Trie lookup duration in nanoseconds:\t\t" + trieTrueTime);
		System.out.println();
		startTime = System.nanoTime();
		result = normalList.contains("pornhubber.com.ph");
		endTime = System.nanoTime();
		System.out.println("ArrayList lookup result (should be false):\t" + result);
		long arrayFalseTime = endTime - startTime;
		System.out.println("ArrayList lookup duration in nanoseconds:\t" + arrayFalseTime);
		startTime = System.nanoTime();
		result = trieRoot.entryExists("pornhubber.com.ph");
		endTime = System.nanoTime();
		System.out.println("Trie lookup check result (should be false):\t" + result);
		long trieFalseTime = endTime - startTime;
		System.out.println("Trie lookup duration in nanoseconds:\t\t" + trieFalseTime);
		System.out.println();
		System.out.println("Trie vs ArrayList lookup difference (true result):   " + (arrayTrueTime - trieTrueTime));
		System.out.println("Trie vs ArrayList lookup difference (false result):  " + (arrayFalseTime - trieFalseTime));
	}
	public static boolean isBanned(String str) {
		if (str.length() == 0) {
			return false;
		}
		return trieRoot.entryExists(str);
	}
}
