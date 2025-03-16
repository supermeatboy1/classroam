package acim.data;

import java.sql.*;

import javax.swing.*;
import javax.swing.table.*;

public class DatabaseManager {
	private static final long TABLE_UPDATE_MILLISECONDS_LIMIT = 1000;
	private static String ROW_SEPARATOR = "\uE000";
	private static JTable tableAccounts = null;
	private static DefaultTableModel tableModel = null;
	private static long lastTableUpdateMillis = 0;
	private static Connection connection = null;

	private static Connection newConnection() throws SQLException {
		if (connection == null || connection.isClosed()) {
			if (connection != null) {
				connection.close();
			}
			String url = "jdbc:mysql://" + Env.get("DB_HOST") + ":" + Env.get("DB_PORT") + "/" + Env.get("DB_NAME") + "?useSSL=true";
			connection = DriverManager.getConnection(url, Env.get("DB_USER"), Env.get("DB_PASSWORD"));
		}
		return connection;
	}
	private static void getStudentSnippet() throws SQLException {
		Connection conn = newConnection();
		Statement stmt = conn.createStatement();
		ResultSet result = stmt.executeQuery("SELECT * FROM StudentSnippet");
		while (result.next()) {
			tableModel.addRow(new String[] {
				result.getString("username"),
				"\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022", // Dots to censor password
				result.getString("first_name"),
				result.getString("last_name"),
				result.getString("email"),
				result.getString("phone_number"),
				result.getString("notes"),
			});
		}
		conn.close();
	}
	
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
				try {
					while (tableModel.getRowCount() > 0) {
						tableModel.removeRow(0);
					}
					getStudentSnippet();
				} catch (Exception e) {
					e.printStackTrace();
				}
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
		Account account = null;
		try {
			Connection conn = newConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Students WHERE username = ?");
			stmt.setString(1, username);
			ResultSet result = stmt.executeQuery();
			while (result.next()) {
				account = new Account(result.getLong("student_id"),
						result.getString("username"),
						result.getString("password"),
						result.getString("first_name"),
						result.getString("last_name"),
						result.getString("email"),
						result.getString("phone_number"),
						result.getString("notes"));
			}
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return account;
	}

	/**
     * Removes an account from the database.
     * 
     * @param account the account to remove.
     */
	public static void removeAccount(Account account) {
		try {
			Connection conn = newConnection();
			PreparedStatement stmt = conn.prepareStatement("UPDATE Students SET is_active = 0 WHERE username = ?");
			stmt.setString(1, account.getUsername());
			int result = stmt.executeUpdate();
			if (result == 0) {
				conn.close();
				throw new SQLException("Error removing account.");
			}
			conn.close();
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "Database error: " + e.getLocalizedMessage(),
					e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
     * Adds a new account to the database.
     * 
     * @param account the new account to add.
     */
	public static void createNewAccount(Account account) {
		try {
			Connection conn = newConnection();
			PreparedStatement stmt = conn.prepareStatement(
					"INSERT INTO Students(first_name, last_name, email, phone_number,"
					+ "username, password, notes) VALUES (?, ?, ?, ?, ?, ?, ?)");
			stmt.setString(1, account.getFirstName());
			stmt.setString(2, account.getLastName());
			stmt.setString(3, account.getEmail());
			stmt.setString(4, account.getPhoneNumber());
			stmt.setString(5, account.getUsername());
			stmt.setString(6, account.getEncodedPassword());
			stmt.setString(7, account.getNotes());
			int result = stmt.executeUpdate();
			if (result == 0) {
				conn.close();
				throw new SQLException("Error creating account.");
			}
			conn.close();
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "Database error: " + e.getLocalizedMessage(),
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
		try {
			Connection conn = newConnection();
			PreparedStatement stmt = conn.prepareStatement("UPDATE Students SET username = ? WHERE username = ?");
			stmt.setString(0, updatedAccount.getUsername());
			stmt.setString(1, oldUsername);
			int result = stmt.executeUpdate();
			if (result == 0) {
				conn.close();
				throw new SQLException("Error removing account.");
			}
			conn.close();
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "Database error: " + e.getLocalizedMessage(),
					e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
     * Updates the data of an existing account.
     * 
     * @param account the account to update.
     */
	public static void updateAccount(Account account) {
		try {
			Connection conn = newConnection();
			PreparedStatement stmt = conn.prepareStatement(
					"UPDATE Students SET first_name = ?, last_name = ?,"
					+ "email = ?, phone_number = ?, username = ?,"
					+ "password = ?, notes = ? WHERE username = ?");
			stmt.setString(1, account.getFirstName());
			stmt.setString(2, account.getLastName());
			stmt.setString(3, account.getEmail());
			stmt.setString(4, account.getPhoneNumber());
			stmt.setString(5, account.getUsername());
			stmt.setString(6, account.getEncodedPassword());
			stmt.setString(7, account.getNotes());
			stmt.setString(8, account.getUsername());
			int result = stmt.executeUpdate();
			if (result == 0) {
				conn.close();
				throw new SQLException("Error creating account.");
			}
			conn.close();
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "Database error: " + e.getLocalizedMessage(),
					e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	private static String serializeAccount(Account account) {
		return account.getUsername() + ROW_SEPARATOR +
				account.getEncodedPassword() + ROW_SEPARATOR +
				account.getFirstName() + ROW_SEPARATOR +
				account.getLastName() + ROW_SEPARATOR +
				account.getEmail() + ROW_SEPARATOR +
				account.getPhoneNumber() + ROW_SEPARATOR +
				account.getNotes();
	}
}
