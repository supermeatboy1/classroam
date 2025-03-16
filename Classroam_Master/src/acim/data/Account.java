package acim.data;

import java.time.*;
import java.util.*;
import java.text.*;

/**
 * The Account class represents a single user account in the system.
 * Each account contains information such as username, encoded password,
 * personal details, usage time, and timestamps related to the user's activity.
 */
public class Account {
	private String username;
	private String encodedPassword;
	private long id;
	private String firstName, lastName, email, phoneNumber, notes;
	
	/**
	 * Constructor to initialize a new Account with basic personal information.
	 * The account is initialized with zero usage time, zero available seconds,
	 * and the last login time is set to the current time.
	 * 
	 * @param username The username of the account
	 * @param encodedPassword The encoded (base64) password
	 * @param firstName The user's first name
	 * @param lastName The user's last name
	 * @param email The user's email address
	 * @param phoneNumber The user's phone number
	 * @param notes Additional notes related to the account
	 */
	public Account(long id, String username, String encodedPassword,
					String firstName, String lastName, String email,
					String phoneNumber, String notes) {
		this.username = username;
		this.encodedPassword = encodedPassword;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.notes = notes;
	}
	/*
	 * Encodes the given password and stores it in the account.
	 * Password is encoded using Base64 URL encoding.
	 * 
	 * @param password The plain text password to encode and store.
	 */
	public void setPassword(String password) {
		Base64.Encoder encoder = Base64.getUrlEncoder();
		this.encodedPassword = encoder.encodeToString(password.getBytes());
	}

	
	// Getters for all fields (provides read-only access to fields)
	public String getUsername() { return username; }
	public String getEncodedPassword() { return encodedPassword; }
	public String getFirstName() { return firstName; }
	public String getLastName() { return lastName; }
	public String getEmail() { return email; }
	public String getPhoneNumber() { return phoneNumber; }
	public String getNotes() { return notes; }
	public long getId() { return id; }

	// Setters for fields which provides ability to modify account information
	public void setUsername(String username) { this.username = username; }
	public void setFirstName(String firstName) { this.firstName = firstName; }
	public void setLastName(String lastName) { this.lastName = lastName; }
	public void setEmail(String email) { this.email = email; }
	public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
	public void setNotes(String notes) { this.notes = notes; }

	/**
	 * Returns a string representation of the account object.
	 * Primarily used for debugging or logging.
	 * 
	 * @return A string describing the account and its fields.
	 */
	@Override
	public String toString() {
		return "Account [username=" + username + ", encodedPassword=" + encodedPassword + ", firstName="
				+ firstName + ", lastName=" + lastName + ", email=" + email + ", phoneNumber=" + phoneNumber
				+ ", notes=" + notes + "]";
	}
	
	/**
	 * Generates a formatted HTML string for displaying account details in a dialog.
	 * This method is useful for showing user-friendly account information in a popup.
	 * 
	 * @return HTML-formatted string with account information.
	 */
	public String getDialogString() {
		return "<html><h1>Account Details: </h1><br>" +
				"<b>Username:</b> " + username + "<br>" +
				"<b>First Name:</b> " + firstName + "<br>" +
				"<b>Last Name:</b> " + lastName + "<br>" +
				"<b>Email:</b> " + email + "<br>" +
				"<b>Phone Number:</b> " + phoneNumber + "<br><br>" +
				"<b>Notes:</b> " + notes + "<br></html>";
	}
}
