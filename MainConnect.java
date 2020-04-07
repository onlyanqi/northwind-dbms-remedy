import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;

public class MainConnect {
	public static void main(String[] args) {
		Statement statement = null;
		Connection connect = null;
		try {
			
			// Use the information from MyIdentity.java to get connected to the database
			
			Properties identity = new Properties();
			Class.forName("com.mysql.cj.jdbc.Driver");
			MyIdentity.setIdentity(identity);
			String user = identity.getProperty("user");
			String password = identity.getProperty("password");
			String dbName = identity.getProperty("database");
			connect = DriverManager.getConnection("jdbc:mysql://localhost:3306?serverTimezone=UTC&useSSL=false", user,
					password);
			statement = connect.createStatement();
			statement.executeQuery("use " + dbName + ";");

			// Create an object ph to call buildhistory method from PurchaseHistory.java
			// Build a history of the purchases made from suppliers
			
			PurchaseHistory ph = new PurchaseHistory();
			ph.buildhistory(connect);
			
			// Create an object iv to implement some methods from Inventory.java
			
			Inventory iv = new Inventory();
			iv.getconnection(connect);

			// Let the user know how to use this interface
			// Ask for the commands from users to trigger a meaningful transaction
			
			String shipCommand = "ship";
			String reorderIssuesCommand = "reorder";
			String receiveCommand = "receive";
			String quitCommand = "quit";
			
			System.out.println("Commands available:");
			System.out.println("  " + shipCommand + " orderNumber");
			System.out.println("  " + reorderIssuesCommand + " Year Month Day");
			System.out.println("  " + receiveCommand + " referenceNumber");
			System.out.println("  " + quitCommand);

			String userCommand = "";
			Scanner userInput = new Scanner(System.in);
			do {
				// Find out what the user wants to do
				userCommand = userInput.next();
				
				if (userCommand.equalsIgnoreCase(shipCommand)) {

					int orderNumber = userInput.nextInt();
					
					// Call the Ship_order method
					// Catch the OrderException if it throws an exception and print the message

					try {
						iv.Ship_order(orderNumber);
						System.out.println("The order has been shipped, the changes in table orders and products are done");
					} catch(OrderException order) {
						System.out.println(order.getMessage());
					}					

				} else if (userCommand.equalsIgnoreCase(reorderIssuesCommand)) {
					
					int year = userInput.nextInt();
					int month = userInput.nextInt();
					int day = userInput.nextInt();
					
					// Call the Issue_reorder method
					// Return the number of suppliers from whom we will be placing an order
					
					int day_issues = iv.Issue_reorders(year, month, day);
					
					System.out.println("The number of issues that need to be reordered is: " + day_issues);

				} else if (userCommand.equalsIgnoreCase(receiveCommand)) {

					int referenceNumber = userInput.nextInt();
					
					// Call the Receive_order method
					// Catch the OrderException if it throws an exception and print the message and reference number
					
					try{
						iv.Receive_order(referenceNumber);
						System.out.println("The order is received, the changes in table reorder and products are done");
					}catch(OrderException order) {
						System.out.println(order.getMessage());
						System.out.println(order.getReference());
					}	
					
				} else if (userCommand.equalsIgnoreCase(quitCommand)) {
					break;
				} else {
					System.out.println("Bad command: " + userCommand);
				}
			} while (!userCommand.equalsIgnoreCase("quit"));
			System.out.println("quit");
			userInput.close();
		}

		catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		} finally {
			try {
				if (connect != null) {
					connect.close();
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

}
