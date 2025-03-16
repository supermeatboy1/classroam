package acim.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import acim.data.*;
import acim.net.*;

/**
 * This class represents a GUI frame for creating or modifying user accounts.
 * It can be initialized either for creating a new account or for editing an
 * existing one. The frame collects and displays account information and updates
 * the backend database as needed.
 */
public class AccountModifierFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	private JTextField txtUsername, txtFirstName, txtLastName, txtEmail, txtPhoneNumber, txtNotes;
	private JLabel lblPassword, lblFirstName, lblLastName, lblEmail, lblPhoneNumber, lblNotes;
	private JPasswordField txtPassword;

	private JButton btnProceed, btnCancel;

	/**
     * Initializes the GUI with all common fields for both account creation and modification.
     *
     * @param isCreatingNewAccount true if creating a new account, false if modifying an existing one
     */
	private void initializeStart(boolean isCreatingNewAccount) {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		if (isCreatingNewAccount)
			setTitle("Create New Account");
		else
			setTitle("Modify Account Information");

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(new GridLayout(0, 2, 8, 8));

		JLabel lblUsername = new JLabel("<html>Username <b>(required)</b></html>");
		contentPane.add(lblUsername);

		txtUsername = new JTextField();
		txtUsername.setText("username");
		contentPane.add(txtUsername);
		txtUsername.setColumns(10);

		if (isCreatingNewAccount)
			lblPassword = new JLabel("<html>Password <b>(required)</b></html>");
		else
			lblPassword = new JLabel("<html>New password <b>(blank if unchanged)</b></html>");
		contentPane.add(lblPassword);

		txtPassword = new JPasswordField();
		txtPassword.setText("");
		contentPane.add(txtPassword);
		txtPassword.setColumns(10);

		lblFirstName = new JLabel("First Name");
		contentPane.add(lblFirstName);

		txtFirstName = new JTextField();
		txtFirstName.setText("First Name");
		contentPane.add(txtFirstName);
		txtFirstName.setColumns(10);

		lblLastName = new JLabel("Last Name");
		contentPane.add(lblLastName);

		txtLastName = new JTextField();
		txtLastName.setText("Last Name");
		contentPane.add(txtLastName);
		txtLastName.setColumns(10);

		lblEmail = new JLabel("Email");
		contentPane.add(lblEmail);

		txtEmail = new JTextField();
		txtEmail.setText("Email");
		contentPane.add(txtEmail);
		txtEmail.setColumns(10);

		lblPhoneNumber = new JLabel("Phone Number");
		contentPane.add(lblPhoneNumber);

		txtPhoneNumber = new JTextField();
		txtPhoneNumber.setText("Phone Number");
		contentPane.add(txtPhoneNumber);
		txtPhoneNumber.setColumns(10);

		lblNotes = new JLabel("Notes");
		contentPane.add(lblNotes);

		txtNotes = new JTextField();
		txtNotes.setText("notes");
		contentPane.add(txtNotes);
		txtNotes.setColumns(10);
	}
	
	/**
     * Finalizes the layout by adding the Cancel button and setting the frame's properties.
     */
	private void initializeEnd() {
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		contentPane.add(btnCancel);

		setSize(640, 480);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	/**
     * Private constructor used when creating a new account.
     *
     * @param table The JTable to be updated after account creation.
     */
	private AccountModifierFrame(JTable table) {
		initializeStart(true);

		btnProceed = new JButton("Create Account");
		btnProceed.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (txtUsername.getText().trim().length() == 0) {
					JOptionPane.showMessageDialog(null, "Blank usernames are not allowed.",
							"Account creation error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (txtPassword.getText().trim().length() == 0) {
					JOptionPane.showMessageDialog(null, "Blank passwords are not allowed.",
							"Account creation error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (DatabaseManager.getAccountByUsername(txtUsername.getText()) != null) {
					JOptionPane.showMessageDialog(null, "Duplicate usernames are not allowed.",
							"Account creation error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				Account newAccount = new Account(txtUsername.getText(), null, txtFirstName.getText(),
						txtLastName.getText(), txtEmail.getText(),
						txtPhoneNumber.getText(), txtNotes.getText());
				newAccount.setPassword(new String(txtPassword.getPassword()));
				DatabaseManager.createNewAccount(newAccount);

				DefaultTableModel model = (DefaultTableModel) table.getModel();
				model.addRow(new String[] {
						txtUsername.getText(),
						"\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022", // Dots to censor password
						txtFirstName.getText(),
						txtLastName.getText(),
						txtEmail.getText(),
						txtPhoneNumber.getText(),
						txtNotes.getText()
				});

				// Instantly update the JTable showing the accounts.
				DatabaseManager.updateAccountTable(true);

				dispose();
			}
		});
		contentPane.add(btnProceed);
		initializeEnd();
	}

	/**
     * Private constructor used when modifying an existing account.
     *
     * @param table       The JTable to update.
     * @param modify      The account to be modified.
     * @param selectedRow The row in the table corresponding to the account.
     */
	private AccountModifierFrame(JTable table, Account modify, int selectedRow) {
		initializeStart(false);
		
		txtUsername.setText(modify.getUsername());
		txtFirstName.setText(modify.getFirstName());
		txtLastName.setText(modify.getLastName());
		txtEmail.setText(modify.getEmail());
		txtPhoneNumber.setText(modify.getPhoneNumber());
		txtNotes.setText(modify.getNotes());

		btnProceed = new JButton("Modify");
		btnProceed.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (txtUsername.getText().trim().length() == 0) {
					JOptionPane.showMessageDialog(null, "Blank usernames are not allowed.",
							"Account creation error", JOptionPane.ERROR_MESSAGE);
					return;
				}
					
				String oldUsername = modify.getUsername();
				// Check if a computer is currently connected to the user that is selected.
				ClientConnection connection = ClientManager.getConnectionFromUsername(oldUsername);
				// Update the available seconds data based on the table.
				long currentSeconds = Long.parseLong((String) DatabaseManager.getAccountTableModel().getValueAt(selectedRow, 6));
				modify.setUsername(txtUsername.getText());
				if (connection != null) {
					// Change the username of the client connection.
					connection.setCurrentUser(txtUsername.getText());
					// Change the username and full name in the client panel, too.
					String ipAddress = connection.getIpAddress();
					ClientManager.setClientPanelCurrentUser(ipAddress, txtUsername.getText());
					ClientManager.setClientPanelCurrentName(ipAddress, txtFirstName.getText() + " " + txtLastName.getText());
				}
				
				// Update the account information in the object.
				String passStr = new String(txtPassword.getPassword());
				if (!passStr.isEmpty())
					modify.setPassword(passStr);
				modify.setFirstName(txtFirstName.getText());
				modify.setLastName(txtLastName.getText());
				modify.setEmail(txtEmail.getText());
				modify.setPhoneNumber(txtPhoneNumber.getText());
				modify.setNotes(txtNotes.getText());

				/*
				if (connection != null) {
					connection.setAccount(modify);
				}
				*/
				
				// Change the username in the accounts file if necessary.
				DatabaseManager.updateAccountUsername(modify, oldUsername);
				// Save it to the database file.
				DatabaseManager.updateAccount(modify);
				// Instantly update the JTable showing the accounts.
				DatabaseManager.updateAccountTable(true);
				
				dispose();
			}
		});
		contentPane.add(btnProceed);
		initializeEnd();
	}

	public static AccountModifierFrame newAccountFrame(JTable table) {
		AccountModifierFrame frame = new AccountModifierFrame(table);
		return frame;
	}

	public static AccountModifierFrame updateAccountFrame(JTable table, Account modify, int selectedRow) {
		AccountModifierFrame frame = new AccountModifierFrame(table, modify, selectedRow);
		return frame;
	}
}
