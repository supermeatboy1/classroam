package acim.data;

import java.text.*;
import java.io.File;
import java.io.IOException;
//import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import javax.swing.*;
import javax.swing.table.*;

// TODO: Replace with Sqlite.

/**
 * DatabaseManager handles account data persistence and retrieval using a simple text file-based system.
 * The account data is stored in "Accounts.txt", and operations include reading, writing, updating,
 * and deleting accounts. This class also integrates with a JTable to display account data in a GUI.
 */
public class DatabaseManager {
	private static final long TABLE_UPDATE_MILLISECONDS_LIMIT = 1000;
	private static String ROW_SEPARATOR = "\uE000";
	private static JTable tableAccounts = null;
	private static DefaultTableModel tableModel = null;
	private static long lastTableUpdateMillis = 0;
	private static NumberFormat decimalFormatter = new DecimalFormat("0.00");

	/**
     * Sets the JTable that will be used to display account data.
     * 
     * @param table the JTable instance.
     */
	public static void setAccountTable(JTable table) {
		tableAccounts = table;
		tableModel = (DefaultTableModel) tableAccounts.getModel();
	}

	/**
     * Returns the table model for the accounts table.
     * 
     * @return the DefaultTableModel for the account JTable.
     */
	public static DefaultTableModel getAccountTableModel() { return tableModel; }
	
	/**
     * Ensures that the "Accounts.txt" file exists. Creates it if necessary.
     * If the file exists but is a directory, deletes and recreates it.
     */
	private static void createAccountsFile() {
		try {
			File f = new File("Accounts.txt");
			if (!f.exists()) {
				f.createNewFile();
			} else {
				if (f.isDirectory()) {
					f.delete();
					f.createNewFile();
				}
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Accounts file creation error: " + e.getLocalizedMessage(),
					e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
     * Opens "Accounts.txt" as a Stream of lines for reading.
     * 
     * @return Stream of lines from the file, or null if an error occurs.
     */
	private static Stream<String> getAccountContentsStream() {
		createAccountsFile();
		try {
			return Files.lines(Paths.get("Accounts.txt"));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "File read error: " + e.getLocalizedMessage(),
					e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}

	/**
     * Reads all lines from "Accounts.txt" into a List.
     * 
     * @return List of account rows, or null if an error occurs.
     */
	private static List<String> getAccountContentsList() {
		createAccountsFile();
		try {
			return Files.readAllLines(Paths.get("Accounts.txt"));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "File read error: " + e.getLocalizedMessage(),
					e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}

	/**
     * Saves a list of account rows back to "Accounts.txt".
     * 
     * @param contents List of account rows.
     */
	private static void saveAccountListContents(List<String> contents) {
		createAccountsFile();
		try {
			Files.write(Paths.get("Accounts.txt"), contents);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "File write error: " + e.getLocalizedMessage(),
					e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	/**
     * Adds a single row to the JTable by deserializing a row string into an Account object.
     * 
     * @param row the serialized account row.
     */
	private static void addRowToTableFromString(String row) {
		Account account = deserializeAccount(row);
		if (account == null)
			return;
		
		tableModel.addRow(new String[] {
				account.getUsername(),
				"\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022", // Dots to censor password
				account.getFirstName(),
				account.getLastName(),
				account.getEmail(),
				account.getPhoneNumber(),
				"" + account.getAvailableSeconds(),
				account.getLastLoginFormattedString(),
				decimalFormatter.format(account.getTotalHours()),
				account.getNotes()
			});
	}

	/**
     * Triggers a refresh of the account table if enough time has passed since the last update.
     */
	public static void updateAccountTable() {
		updateAccountTable(false);
	}

	/**
     * Updates the JTable displaying account data, optionally forcing the update.
     * 
     * @param forceUpdate true to bypass update throttling.
     */
	public static void updateAccountTable(boolean forceUpdate) {
		if (!forceUpdate && System.currentTimeMillis() - lastTableUpdateMillis < TABLE_UPDATE_MILLISECONDS_LIMIT)
			return;
		
		// To fix JTable flickering...
		// https://forums.oracle.com/ords/apexds/post/jtable-flickering-when-updated-4725
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Save the current selected row.
				int selectedRow = tableAccounts.getSelectedRow();
				
				while (tableModel.getRowCount() > 0)
					tableModel.removeRow(0);

				// Get the contents of the file as a Stream of Strings.
				Stream<String> dbContentStream = getAccountContentsStream();
				if (dbContentStream == null) {
					return;
				}
				dbContentStream.forEach(row -> addRowToTableFromString(row));
				if (selectedRow != -1) {
					try {
						tableAccounts.setRowSelectionInterval(selectedRow, selectedRow);
					} catch (Exception e) {
						// Ignore "row selection out of range" errors.
					}
				}
				dbContentStream.close();
			}
		});
		
		if (!forceUpdate)
			lastTableUpdateMillis = System.currentTimeMillis();
	}

	/**
     * Finds and returns an Account object by its username.
     * 
     * @param username the username to search for.
     * @return the matching Account object, or null if not found.
     */
	public static Account getAccountByUsername(String username) {
		// Get the contents of the file as a Stream of Strings.
		Stream<String> dbContentStream = getAccountContentsStream();
		if (dbContentStream == null) {
			return null;
		}
		String line = dbContentStream.filter(row -> row.startsWith(username + ROW_SEPARATOR))
									.findFirst()
									.orElse(null);
		if (line == null)
			return null;
		
		return deserializeAccount(line);
	}

	/**
     * Removes an account from the file.
     * 
     * @param account the account to remove.
     */
	public static void removeAccount(Account account) {
		List<String> rowList = getAccountContentsList();
		if (rowList == null || rowList.isEmpty())
			return;
		
		int lineNumber = 0;
		for (String row : rowList) {
			if (account.getUsername().equals(row.split(ROW_SEPARATOR)[0]))
				break;
			lineNumber++;
		}
		rowList.remove(lineNumber);
		saveAccountListContents(rowList);
	}

	/**
     * Adds a new account to the file.
     * 
     * @param account the new account to add.
     */
	public static void createNewAccount(Account account) {
		// Add account to account list stored in the file.
		createAccountsFile();
		try {
			Files.write(Paths.get("Accounts.txt"), (serializeAccount(account) + "\r\n"
						).getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "File write error: " + e.getLocalizedMessage(),
					e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
     * Updates the username of an existing account.
     * 
     * @param updatedAccount the account with the new username.
     * @param oldUsername the previous username.
     */
	public static void updateAccountUsername(Account updatedAccount, String oldUsername) {
		if (updatedAccount.getUsername().equals(oldUsername))
			return;
		
		List<String> rowList = getAccountContentsList();
		int lineNumber = 0;
		for (String row : rowList) {
			Account outdatedAccount = deserializeAccount(row);
			if (outdatedAccount.getUsername().equals(oldUsername))
				rowList.set(lineNumber, serializeAccount(updatedAccount));
			lineNumber++;
		}
		saveAccountListContents(rowList);
	}

	/**
     * Updates the data of an existing account.
     * 
     * @param account the account to update.
     */
	public static void updateAccount(Account account) {
		List<String> rowList = getAccountContentsList();
		int lineNumber = 0;
		for (String row : rowList) {
			if (account.getUsername().equals(row.split(ROW_SEPARATOR)[0]))
				rowList.set(lineNumber, serializeAccount(account));
			lineNumber++;
		}
		saveAccountListContents(rowList);
	}
	
	private static Account deserializeAccount(String line) {
		if (line == null || line.strip().isEmpty())
			return null;
		String[] split = line.split(ROW_SEPARATOR);
		Account account = new Account(
			split[0], split[1], split[2], split[3], split[4], split[5], split[9]
		);
		account.setLastLogin(Long.parseLong(split[7]));
		account.setAvailableSeconds(Long.parseLong(split[6]));
		account.setTotalHours(Float.parseFloat(split[8]));
		return account;
	}
	private static String serializeAccount(Account account) {
		return account.getUsername() + ROW_SEPARATOR +
				account.getEncodedPassword() + ROW_SEPARATOR +
				account.getFirstName() + ROW_SEPARATOR +
				account.getLastName() + ROW_SEPARATOR +
				account.getEmail() + ROW_SEPARATOR +
				account.getPhoneNumber() + ROW_SEPARATOR +
				account.getAvailableSeconds() + ROW_SEPARATOR +
				account.getLastLogin() + ROW_SEPARATOR +
				account.getTotalHours() + ROW_SEPARATOR +
				account.getNotes();
	}
}
