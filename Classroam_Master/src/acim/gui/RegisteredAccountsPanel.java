package acim.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

import acim.data.*;
import acim.net.*;

/**
 * Panel for displaying and managing registered user accounts in a table format.
 * Provides options to view, update, create, delete, and manage accounts.
 */
public class RegisteredAccountsPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private JTable tableAccount;
	private DefaultTableModel tableModel;
	
	public RegisteredAccountsPanel() {
		setLayout(new GridLayout(0, 1, 0, 0));
		
		JSplitPane splitPaneAccountManager = new JSplitPane();
		splitPaneAccountManager.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPaneAccountManager.setResizeWeight(0.85);
		add(splitPaneAccountManager);
		
		String column[] = {"Username", "Password", "First Name", "Last Name", "Email", "Phone Number", "Available Seconds", "Last Login", "Total Hours", "Notes"};
		String data[][] = {};
		tableAccount = new JTable();
		tableAccount.setModel(new DefaultTableModel(data, column) {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		});
		// Adjust some column widths...
		TableColumnModel columnModel = tableAccount.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(40);
		columnModel.getColumn(1).setPreferredWidth(40);
		columnModel.getColumn(4).setPreferredWidth(40);
		columnModel.getColumn(5).setPreferredWidth(70);
		columnModel.getColumn(7).setPreferredWidth(100);
		columnModel.getColumn(8).setPreferredWidth(40);
		columnModel.getColumn(9).setPreferredWidth(100);
		tableAccount.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		
		// Only allow the user to select ONE row in the table.
		tableAccount.setRowSelectionAllowed(true);
		tableAccount.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		DatabaseManager.setAccountTable(tableAccount);
		DatabaseManager.updateAccountTable();
		tableModel = (DefaultTableModel) tableAccount.getModel();

		JScrollPane scrollPaneTableAccount = new JScrollPane();
		scrollPaneTableAccount.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneTableAccount.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPaneTableAccount.setViewportView(tableAccount);
		
		splitPaneAccountManager.setLeftComponent(scrollPaneTableAccount);
		
		JPanel panelAccountActions = new JPanel();
		splitPaneAccountManager.setRightComponent(panelAccountActions);

		panelAccountActions.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
		panelAccountActions.setLayout(new GridLayout(2, 2, 16, 16));
		
		JButton btnUpdateInformation = new JButton("Update Information");
		btnUpdateInformation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Return if there is no selected row.
				if (tableAccount.getSelectedRow() == -1) {
					JOptionPane.showMessageDialog(null, "Please select a row first.");
					return;
				}
				
				Account modifyingAccount = DatabaseManager.getAccountByUsername(
						(String) tableAccount.getValueAt(tableAccount.getSelectedRow(), 0));
				AccountModifierFrame.updateAccountFrame(tableAccount, modifyingAccount, tableAccount.getSelectedRow());
			}
		});
		panelAccountActions.add(btnUpdateInformation);

		JButton btnViewInformation = new JButton("View Information");
		btnViewInformation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Return if there is no selected row.
				if (tableAccount.getSelectedRow() == -1) {
					JOptionPane.showMessageDialog(null, "Please select a row first.");
					return;
				}
				String username = (String) tableAccount.getValueAt(tableAccount.getSelectedRow(), 0);
				
				Account account = DatabaseManager.getAccountByUsername(username);
				JOptionPane.showMessageDialog(null, account.getDialogString());
			}
		});
		panelAccountActions.add(btnViewInformation);
		
		JButton btnLogout = new JButton("Logout");
		btnLogout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Return if there is no selected row.
				if (tableAccount.getSelectedRow() == -1) {
					JOptionPane.showMessageDialog(null, "Please select a row first.");
					return;
				}
				String username = (String) tableAccount.getValueAt(tableAccount.getSelectedRow(), 0);
				
				ClientConnection conn = ClientManager.getConnectionFromUsername(username);
				if (conn == null)
					JOptionPane.showMessageDialog(null, "The user \"" + username + "\" is not currently "
							+ "logged in on any connected computers.");
				else
					conn.kickout();
			}
		});
		panelAccountActions.add(btnLogout);
		
		JButton btnNewAccount = new JButton("New Account");
		btnNewAccount.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AccountModifierFrame.newAccountFrame(tableAccount);
				DatabaseManager.updateAccountTable(true);
			}
		});
		panelAccountActions.add(btnNewAccount);
		
		JButton btnPayForMinutes = new JButton("Pay For Minutes");
		btnPayForMinutes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Return if there is no selected row.
				if (tableAccount.getSelectedRow() == -1) {
					JOptionPane.showMessageDialog(null, "Please select a row first.");
					return;
				}
				
				String input = JOptionPane.showInputDialog("How many minutes?");
				
				if (input == null)
					return;
				
				float minutes = 0;
				try {
					minutes = Float.parseFloat(input);
				} catch (NumberFormatException ne) { return; }

				Account modify = DatabaseManager.getAccountByUsername(
						(String) tableAccount.getValueAt(tableAccount.getSelectedRow(), 0));
				
				// Now add it to the balance.
				modify.setAvailableSeconds((long) (modify.getAvailableSeconds() + (60 * minutes)));

				DatabaseManager.updateAccount(modify);
				DatabaseManager.updateAccountTable();

				// Check if a computer is currently connected to the user that is selected.
				ClientConnection connection = ClientManager.getConnectionFromUsername(modify.getUsername());
				if (connection != null) {
					// Update the seconds counter on the computer that's connected to the user.
					connection.queueCommand("update available seconds " + modify.getAvailableSeconds());
					connection.setAccount(modify);
					connection.updateUsageSeconds();
				}
			}
		});
		panelAccountActions.add(btnPayForMinutes);
		
		JButton btnDeleteAccount = new JButton("Delete Account");
		btnDeleteAccount.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Return if there is no selected row.
				if (tableAccount.getSelectedRow() == -1) {
					JOptionPane.showMessageDialog(null, "Please select a row first.");
					return;
				}
				if (JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this account?") != 0)
					return;
				
				// Return if there is no selected row.
				if (tableAccount.getSelectedRow() == -1)
					return;
				String username = (String) tableAccount.getValueAt(tableAccount.getSelectedRow(), 0);
				
				Account modifyingAccount = DatabaseManager.getAccountByUsername(username);
				DatabaseManager.removeAccount(modifyingAccount);
				DatabaseManager.updateAccountTable(true);

				// Check if a computer is currently connected to the user that is selected.
				ClientConnection connection = ClientManager.getConnectionFromUsername(username);
				if (connection != null) {
					// Kick out that user since the account is now deleted.
					connection.kickout();
				}
			}
		});
		panelAccountActions.add(btnDeleteAccount);
	}
}
