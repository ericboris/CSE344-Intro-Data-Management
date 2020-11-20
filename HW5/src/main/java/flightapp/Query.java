package flightapp;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * Runs queries against a back-end database
 */
public class Query {
    // DB Connection
    private Connection conn;

    // Password hashing parameter constants
    private static final int HASH_STRENGTH = 65536;
    private static final int KEY_LENGTH = 128;

    // Let currentUser be the currently logged in username.
    private String currentUser;

    // Let itineraries maintain the current itinerary search results.
    private List<Itinerary> itineraries;

    // Let the following section contain predefined SQL queries.

    // Canned queries
    private static final String CHECK_FLIGHT_CAPACITY = "SELECT capacity FROM Flights WHERE fid = ?";
    private PreparedStatement checkFlightCapacityStatement;

    // For check dangling
    private static final String TRANCOUNT_SQL = "SELECT @@TRANCOUNT AS tran_count";
    private PreparedStatement tranCountStatement;

    // Used to clear the Users table.
    private static final String CLEAR_USERS = "DELETE FROM Users;";
    private PreparedStatement clearUsersStatement;

    // Used to clear the Reservations table.
    private static final String CLEAR_RESERVATIONS = "DELETE FROM Reservations;";
    private PreparedStatement clearReservationsStatement;

    // Used to check if username already exists
    private static final String CHECK_USERNAME_EXISTS = "SELECT * FROM Users WHERE LOWER(username) = ?;";
    private PreparedStatement checkUsernameExistsStatement;

    // Used to add a new user to the Users table.
    private static final String INSERT_USER_DATA = "INSERT INTO Users VALUES (?, ?, ?, ?);";
    private PreparedStatement insertUserDataStatement;

    // Used to get a salt and hash based on username for user login.
    private static final String USER_LOGIN = "SELECT salt, hash FROM Users WHERE LOWER(username) = ?;";
    private PreparedStatement userLoginStatement;

    // Used to get the top n direct flights from src to dst on a given day in July 2015. 
    private static final String DIRECT_FLIGHT = "SELECT top(?)"
					      + "       fid,"  
					      + "       day_of_month,"
					      + "       carrier_id,"  
					      + "       flight_num,"  
					      + "       origin_city,"  
					      + "       dest_city,"  
					      + "       actual_time,"  
					      + "       capacity,"  
					      + "       price"  
					      + "  FROM Flights"
					      + " WHERE origin_city = ?"
					      + "   AND dest_city = ?"
					      + "   AND day_of_month = ?"
					      + "   AND canceled <> 1"
					      + " ORDER BY actual_time ASC,"
					      + "       fid ASC;";
    private PreparedStatement directFlightStatement;
    
    // Used to get the top one-hop flights from src to dst in a month.
    private static final String ONE_HOP_FLIGHT = "select TOP(?)"
					       + "       f1.fid as f1_fid,"  
					       + "       f1.day_of_month AS f1_day_of_month,"
					       + "       f1.carrier_id AS f1_carrier_id,"  
					       + "       f1.flight_num AS f1_flight_num,"  
					       + "       f1.origin_city AS f1_origin_city,"  
					       + "       f1.dest_city AS f1_dest_city,"  
					       + "       f1.actual_time AS f1_actual_time,"  
					       + "       f1.capacity AS f1_capacity,"  
					       + "       f1.price AS f1_price,"  
					       + "       f2.fid AS f2_fid,"  
					       + "       f2.day_of_month AS f2_day_of_month,"
					       + "       f2.carrier_id AS f2_carrier_id,"  
					       + "       f2.flight_num AS f2_flight_num,"  
					       + "       f2.origin_city AS f2_origin_city,"  
					       + "       f2.dest_city AS f2_dest_city,"  
					       + "       f2.actual_time AS f2_actual_time,"  
					       + "       f2.capacity AS f2_capacity,"  
					       + "       f2.price AS f2_price"  
					       + "  FROM flights as f1,"
					       + "       flights as f2"
					       + " WHERE f1.origin_city = ?"
					       + "   AND f2.dest_city = ?"
					       + "   AND f1.day_of_month = ?"
					       + "   AND f1.dest_city = f2.origin_city"
					       + "   AND f1.day_of_month = f2.day_of_month"
					       + "   AND f1.canceled <> 1"
					       + "   AND f2.canceled <> 1"
					       + " ORDER BY (f1.actual_time + f2.actual_time) ASC,"
					       + "       f1.fid,"
					       + "       f2.fid;";
    private PreparedStatement oneHopFlightStatement;

    /**
     * Class constructor.
     */
    public Query() throws SQLException, IOException {
	this(null, null, null, null);
	this.itineraries = new ArrayList<>();
	this.currentUser = null;
    }

    /**
     * Class constructor.
     */
    protected Query(String serverURL, String dbName, String adminName, String password)
	throws SQLException, IOException {
	    conn = serverURL == null ? openConnectionFromDbConn()
		: openConnectionFromCredential(serverURL, dbName, adminName, password); prepareStatements(); } /**

     * Return a connecion by using dbconn.properties file
     *
     * @throws SQLException
     * @throws IOException
     */
    public static Connection openConnectionFromDbConn() throws SQLException, IOException {
	// Connect to the database with the provided connection configuration
	Properties configProps = new Properties();
	configProps.load(new FileInputStream("dbconn.properties"));
	String serverURL = configProps.getProperty("flightapp.server_url");
	String dbName = configProps.getProperty("flightapp.database_name");
	String adminName = configProps.getProperty("flightapp.username");
	String password = configProps.getProperty("flightapp.password");
	return openConnectionFromCredential(serverURL, dbName, adminName, password);
    }

    /**
     * Return a connecion by using the provided parameter.
     *
     * @param serverURL example: example.database.widows.net
     * @param dbName    database name
     * @param adminName username to login server
     * @param password  password to login server
     *
     * @throws SQLException
     */
    protected static Connection openConnectionFromCredential(String serverURL, String dbName,
	    String adminName, String password) throws SQLException {
	String connectionUrl =
	    String.format("jdbc:sqlserver://%s:1433;databaseName=%s;user=%s;password=%s", serverURL,
		    dbName, adminName, password);
	Connection conn = DriverManager.getConnection(connectionUrl);

	// By default, automatically commit after each statement
	conn.setAutoCommit(true);

	// By default, set the transaction isolation level to serializable
	conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

	return conn;
    }

    /**
     * Get underlying connection
     */
    public Connection getConnection() {
	return conn;
    }

    /**
     * Closes the application-to-database connection
     */
    public void closeConnection() throws SQLException {
	conn.close();
    }

    /**
     * Clear the data in any custom tables created.
     *
     * WARNING! Do not drop any tables and do not clear the flights table.
     */
    public void clearTables() {
	try {
	    // Clear the Users table.
	    clearUsersStatement.clearParameters();
	    clearUsersStatement.executeUpdate();
	    clearUsersStatement.close();

	    // Clear the Reservations table.
	    clearReservationsStatement.clearParameters();
	    clearReservationsStatement.executeUpdate();
	    clearReservationsStatement.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /*
     * prepare all the SQL statements in this method.
     */
    private void prepareStatements() throws SQLException {
	checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);
	tranCountStatement = conn.prepareStatement(TRANCOUNT_SQL);
	clearUsersStatement = conn.prepareStatement(CLEAR_USERS);
	clearReservationsStatement = conn.prepareStatement(CLEAR_RESERVATIONS);
	checkUsernameExistsStatement = conn.prepareStatement(CHECK_USERNAME_EXISTS);
	insertUserDataStatement = conn.prepareStatement(INSERT_USER_DATA);
	userLoginStatement = conn.prepareStatement(USER_LOGIN);
	directFlightStatement = conn.prepareStatement(DIRECT_FLIGHT);
	oneHopFlightStatement = conn.prepareStatement(ONE_HOP_FLIGHT);
    }

    /**
     * Takes a user's username and password and attempts to log the user in.
     *
     * @param username user's username
     * @param password user's password
     *
     * @return If someone has already logged in, then return "User already logged in\n" For all other
     *         errors, return "Login failed\n". Otherwise, return "Logged in as [username]\n".
     */
    public String transaction_login(String username, String password) {
	try {
	    // We use lowercase for name comparisons because username is case insensitive.
	    String lowercaseUsername = username.toLowerCase();
    
	    // Prevent multiple users from being logged in simultaneously.
	    if (this.currentUser != null) {
		return "User already logged in\n";
	    }

	    // Log the user in.
	    try {
		// Prepare the username statement.
		userLoginStatement.clearParameters();
		userLoginStatement.setString(1, lowercaseUsername);

		// Let rs be the results of the query requesting salt and hash.
		ResultSet rs = userLoginStatement.executeQuery();

		// Only proceed if the query returned a result.
		if (rs.next()) {
		    // Grab the stored salt.
		    byte[] salt = rs.getBytes("salt");

		    // Specify the hash parameters from the given salt.
		    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);

		    // Generate a hash.
		    SecretKeyFactory factory = null;
		    byte[] generatedHash = null;
		    try {
			factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			generatedHash = factory.generateSecret(spec).getEncoded();
		    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
			throw new IllegalStateException();
		    }

		    // Get the hash returned by the query.
		    byte[] returnedHash = rs.getBytes("hash");
		   
		    // We can log the user in if the hashes are equivalent.
		    if (Arrays.equals(generatedHash, returnedHash)) {
			rs.close();
			// Store the lowercase name to maintain case insensitive username comparisons.
			this.currentUser = username;
			return "Logged in as " + username + "\n";
		    }
		}
	    } catch (SQLException e) {
		e.printStackTrace();
	    }
	    
	    // Notify if any of the above failed.
	    return "Login failed\n";

	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * Implement the create user function.
     *
     * @param username   new user's username. User names are unique the system.
     * @param password   new user's password.
     * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure
     *                   otherwise).
     *
     * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
     */
    public String transaction_createCustomer(String username, String password, int initAmount) {
	try {
	    // Verify that the username doesn't alredy exist
	    // and that the initAmount is valid.

	    // Prepare the username statement.
	    checkUsernameExistsStatement.clearParameters();
	    checkUsernameExistsStatement.setString(1, username.toLowerCase());

	    // Query the Users table for any usernames matching username.
	    ResultSet rs = checkUsernameExistsStatement.executeQuery();

	    // Return an error if the query returned any rows
	    // or if the amount being added to the acount is negative.
	    if (rs.next() || initAmount < 0) {
		return "Failed to create user\n";
	    } else {
		// Remember to close the query connection.
		rs.close();
	    }

	    // Our inputs were valid.
	    // And we can add the new user to the table.
	    // But first, encrypt the password.

	    // Generate a random crypographic salt.
	    SecureRandom random = new SecureRandom();
	    byte[] salt = new byte[16];
	    random.nextBytes(salt);

	    // Specify the hash parameters.
	    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);

	    // Generate the hash.
	    SecretKeyFactory factory = null;
	    byte[] hash = null;
	    try {
		factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		hash = factory.generateSecret(spec).getEncoded();
	    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
		throw new IllegalStateException();
	    }

	    // With the data prepared, insert the user data into the table.
	    insertUserDataStatement.clearParameters();

	    insertUserDataStatement.setString(1, username);
	    insertUserDataStatement.setBytes(2, salt);
	    insertUserDataStatement.setBytes(3, hash);
	    insertUserDataStatement.setInt(4, initAmount);

	    insertUserDataStatement.executeUpdate();

	    // Notify that the insertion was successful.
	    return "Created user " + username + "\n";

	} catch (SQLException e) {
	    e.printStackTrace();
	    return "Failed to create user\n";
	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * Implement the search function.
     *
     * Searches for flights from the given origin city to the given destination city, on the given day
     * of the month. If {@code directFlight} is true, it only searches for direct flights, otherwise
     * is searches for direct flights and flights with two "hops." Only searches for up to the number
     * of itineraries given by {@code numberOfItineraries}.
     *
     * The results are sorted based on total flight time.
     *
     * @param originCity
     * @param destinationCity
     * @param directFlight        if true, then only search for direct flights, otherwise include
     *                            indirect flights as well
     * @param dayOfMonth
     * @param numberOfItineraries number of itineraries to return
     *
     * @return If no itineraries were found, return "No flights match your selection\n". If an error
     *         occurs, then return "Failed to search\n".
     *
     *         Otherwise, the sorted itineraries printed in the following format:
     *
     *         Itinerary [itinerary number]: [number of flights] flight(s), [total flight time]
     *         minutes\n [first flight in itinerary]\n ... [last flight in itinerary]\n
     *
     *         Each flight should be printed using the same format as in the {@code Flight} class.
     *         Itinerary numbers in each search should always start from 0 and increase by 1.
     *
     * @see Flight#toString()
     */
    public String transaction_search(String originCity, String destinationCity, boolean directFlight,
	    int dayOfMonth, int numberOfItineraries) {
	try {
	    StringBuffer sb = new StringBuffer();
	    
	    // Reinitialize itineraries to an empty list;
	    itineraries = new ArrayList<>();
    
	    try {
		// Fill itineraries with flight itineraries.
		// Get all the direct flights, regardless of directFlight bool value.

		// Prepare the query.
		directFlightStatement.clearParameters();
		directFlightStatement.setInt(1, numberOfItineraries);
		directFlightStatement.setString(2, originCity);
		directFlightStatement.setString(3, destinationCity);
		directFlightStatement.setInt(4, dayOfMonth);
	
		// Get the query.	
		ResultSet directFlightQueryResult = directFlightStatement.executeQuery();

		// For each itinerary in the query, add it to itineraries.
		while (directFlightQueryResult.next()) {	
		    // Extract the relevant query information.
		    // Flight 1 info.
		    int f1fid = directFlightQueryResult.getInt("fid");
		    int f1dayOfMonth = directFlightQueryResult.getInt("day_of_month");
		    String f1carrierId = directFlightQueryResult.getString("carrier_id");
		    String f1flightNum = directFlightQueryResult.getString("flight_num");
		    String f1originCity = directFlightQueryResult.getString("origin_city");
		    String f1destCity = directFlightQueryResult.getString("dest_city");
		    int f1time = directFlightQueryResult.getInt("actual_time");
		    int f1capacity = directFlightQueryResult.getInt("capacity");
		    int f1price = directFlightQueryResult.getInt("price");

		    // Use the information to create a new flight.
		    Flight flight1 = new Flight(f1fid, f1dayOfMonth, f1carrierId, f1flightNum, f1originCity, f1destCity, f1time, f1capacity, f1price);

		    // Add the flight to an itinerary.
		    Itinerary itinerary = new Itinerary(flight1, null, f1time, true);
	
		    // And add the itinerary to the itinerarys list.
		    itineraries.add(itinerary);
		}

		// Finished with the query.
		directFlightQueryResult.close();

		// Get the number of itineraries currently stored.
		int currNumItineraries = itineraries.size();

		// Get the number of available spaces for itinerary entries.
		int numItinerarySpaces = numberOfItineraries - currNumItineraries;

		// If possible, any indirect flight itineraries to itineraries too.	
		// Can only accept indirect flights if, they are admissible
		// and there aren't already too many itineraries stored.
		if (numItinerarySpaces > 0 && directFlight == false) {
		    // Prepare the query.
		    oneHopFlightStatement.clearParameters();
		    oneHopFlightStatement.setInt(1, numItinerarySpaces);
		    oneHopFlightStatement.setString(2, originCity);
		    oneHopFlightStatement.setString(3, destinationCity);
		    oneHopFlightStatement.setInt(4, dayOfMonth);

		    // Get the query.	
		    ResultSet oneHopFlightQueryResult = oneHopFlightStatement.executeQuery();

		    // For each itinerary in the query, add it to itineraries.
		    while (oneHopFlightQueryResult.next()) {
			// Extract the relevant query information.
			// Flight 1 info.
			int f1fid = oneHopFlightQueryResult.getInt("f1_fid");
			int f1dayOfMonth = oneHopFlightQueryResult.getInt("f1_day_of_month");
			String f1carrierId = oneHopFlightQueryResult.getString("f1_carrier_id");
			String f1flightNum = oneHopFlightQueryResult.getString("f1_flight_num");
			String f1originCity = oneHopFlightQueryResult.getString("f1_origin_city");
			String f1destCity = oneHopFlightQueryResult.getString("f1_dest_city");
			int f1time = oneHopFlightQueryResult.getInt("f1_actual_time");
			int f1capacity = oneHopFlightQueryResult.getInt("f1_capacity");
			int f1price = oneHopFlightQueryResult.getInt("f1_price");

			// Flight 2 info.
			int f2fid = oneHopFlightQueryResult.getInt("f2_fid");
			int f2dayOfMonth = oneHopFlightQueryResult.getInt("f2_day_of_month");
			String f2carrierId = oneHopFlightQueryResult.getString("f2_carrier_id");
			String f2flightNum = oneHopFlightQueryResult.getString("f2_flight_num");
			String f2originCity = oneHopFlightQueryResult.getString("f2_origin_city");
			String f2destCity = oneHopFlightQueryResult.getString("f2_dest_city");
			int f2time = oneHopFlightQueryResult.getInt("f2_actual_time");
			int f2capacity = oneHopFlightQueryResult.getInt("f2_capacity");
			int f2price = oneHopFlightQueryResult.getInt("f2_price");

			// Create the flights from the query info.
			Flight flight1 = new Flight(f1fid, f1dayOfMonth, f1carrierId, f1flightNum, f1originCity, f1destCity, f1time, f1capacity, f1price);
			Flight flight2 = new Flight(f2fid, f2dayOfMonth, f2carrierId, f2flightNum, f2originCity, f2destCity, f2time, f2capacity, f2price);

			// Itinerary with multiple flights holds the total flight time.
			int totalFlightTime = f1time + f2time;
    
			// Add the flights to an itinerary.
			Itinerary itinerary = new Itinerary(flight1, flight2, totalFlightTime, false);

			// And add the itinerary to the itinerarys list.
			itineraries.add(itinerary);
		    } 		    
		    // Finished with the query.
		    oneHopFlightQueryResult.close();

		    // If indirect flights have been added to itineraries,
		    // then itineraries needs to be sorted by ascending flight times.
		    Collections.sort(itineraries);
		}

	    // Fill the string builder with the ordered itineraries.
	    int n = itineraries.size();
	    for (int i = 0; i < n; i++) {
		sb.append("Itinerary " + i);

		Itinerary itinerary = itineraries.get(i);
		if (itinerary.isDirectFlight) {
		    sb.append(": 1 flight(s), " + itinerary.flightTime + " minutes\n" + itinerary.flight1.toString() + "\n"); 
		} else {
		    sb.append(": 2 flight(s), " + itinerary.flightTime + " minutes\n" + itinerary.flight1.toString() + "\n" + itinerary.flight2.toString() + "\n");
		}
	    }
	    
	    // Handle any errors while querying.
	    } catch (SQLException e) {
		e.printStackTrace();
	    }
	    
	    // Return a string message about the query results. 
	    String output = (sb.length() == 0) ? "No flights match your selection\n" : sb.toString();
	    return output;
    
	// Clean up. 
	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * Implements the book itinerary function.
     *
     * @param itineraryId ID of the itinerary to book. This must be one that is returned by search in
     *                    the current session.
     *
     * @return If the user is not logged in, then return "Cannot book reservations, not logged in\n".
     *         If the user is trying to book an itinerary with an invalid ID or without having done a
     *         search, then return "No such itinerary {@code itineraryId}\n". If the user already has
     *         a reservation on the same day as the one that they are trying to book now, then return
     *         "You cannot book two flights in the same day\n". For all other errors, return "Booking
     *         failed\n".
     *
     *         And if booking succeeded, return "Booked flight(s), reservation ID: [reservationId]\n"
     *         where reservationId is a unique number in the reservation system that starts from 1 and
     *         increments by 1 each time a successful reservation is made by any user in the system.
     */
    public String transaction_book(int itineraryId) {
	try {
	    // TODO: YOUR CODE HERE
	    return "Booking failed\n";
	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * Implements the pay function.
     *
     * @param reservationId the reservation to pay for.
     *
     * @return If no user has logged in, then return "Cannot pay, not logged in\n" If the reservation
     *         is not found / not under the logged in user's name, then return "Cannot find unpaid
     *         reservation [reservationId] under user: [username]\n" If the user does not have enough
     *         money in their account, then return "User has only [balance] in account but itinerary
     *         costs [cost]\n" For all other errors, return "Failed to pay for reservation
     *         [reservationId]\n"
     *
     *         If successful, return "Paid reservation: [reservationId] remaining balance:
     *         [balance]\n" where [balance] is the remaining balance in the user's account.
     */
    public String transaction_pay(int reservationId) {
	try {
	    // TODO: YOUR CODE HERE
	    return "Failed to pay for reservation " + reservationId + "\n";
	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * Implements the reservations function.
     *
     * @return If no user has logged in, then return "Cannot view reservations, not logged in\n" If
     *         the user has no reservations, then return "No reservateons found\n" For all other
     *         errors, return "Failed to retrieve reservations\n"
     *
     *         Otherwise return the reservations in the following format:
     *
     *         Reservation [reservation ID] paid: [true or false]:\n [flight 1 under the
     *         reservation]\n [flight 2 under the reservation]\n Reservation [reservation ID] paid:
     *         [true or false]:\n [flight 1 under the reservation]\n [flight 2 under the
     *         reservation]\n ...
     *
     *         Each flight should be printed using the same format as in the {@code Flight} class.
     *
     * @see Flight#toString()
     */
    public String transaction_reservations() {
	try {
	    // TODO: YOUR CODE HERE
	    return "Failed to retrieve reservations\n";
	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * Implements the cancel operation.
     *
     * @param reservationId the reservation ID to cancel
     *
     * @return If no user has logged in, then return "Cannot cancel reservations, not logged in\n" For
     *         all other errors, return "Failed to cancel reservation [reservationId]\n"
     *
     *         If successful, return "Canceled reservation [reservationId]\n"
     *
     *         Even though a reservation has been canceled, its ID should not be reused by the system.
     */
    public String transaction_cancel(int reservationId) {
	try {
	    // TODO: YOUR CODE HERE:
	    return "Failed to cancel reservation " + reservationId + "\n";
	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * Example utility function that uses prepared statements
     */
    private int checkFlightCapacity(int fid) throws SQLException {
	checkFlightCapacityStatement.clearParameters();
	checkFlightCapacityStatement.setInt(1, fid);
	ResultSet results = checkFlightCapacityStatement.executeQuery();
	results.next();
	int capacity = results.getInt("capacity");
	results.close();

	return capacity;
    }

    /**
     * Throw IllegalStateException if transaction not completely complete, rollback.
     *
     */
    private void checkDanglingTransaction() {
	try {
	    try (ResultSet rs = tranCountStatement.executeQuery()) {
		rs.next();
		int count = rs.getInt("tran_count");
		if (count > 0) {
		    throw new IllegalStateException(
			    "Transaction not fully commit/rollback. Number of transaction in process: " + count);
		}
	    } finally {
		conn.setAutoCommit(true);
	    }
	} catch (SQLException e) {
	    throw new IllegalStateException("Database error", e);
	}
    }

    private static boolean isDeadLock(SQLException ex) {
	return ex.getErrorCode() == 1205;
    }

    /**
     * A class to store flight information.
     */
    class Flight {
	public int fid;
	public int dayOfMonth;
	public String carrierId;
	public String flightNum;
	public String originCity;
	public String destCity;
	public int time;
	public int capacity;
	public int price;

	/**
	 * Class constructor.
	 */
	public Flight(int fid, int dayOfMonth, String carrierId, String flightNum, String originCity, String destCity,
		      int time, int capacity, int price) {
	    this.fid = fid;
	    this.dayOfMonth = dayOfMonth;
	    this.carrierId = carrierId;
	    this.flightNum = flightNum;
	    this.originCity = originCity;
	    this.destCity = destCity;
	    this.time = time;
	    this.capacity = capacity;
	    this.price = price;
	}

	@Override
	    public String toString() {
		return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
		    + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
		    + " Capacity: " + capacity + " Price: " + price;
	    }
    }

    /**
     * A class to store itinerary information.
     */
    class Itinerary implements Comparable<Itinerary> {
	public Flight flight1;
	public Flight flight2;
	public int flightTime;
	public boolean isDirectFlight;

	/**
	 * Class constructor.
	 */
	public Itinerary(Flight flight1, Flight flight2, int flightTime, boolean isDirectFlight) {
	    this.flight1 = flight1;
	    this.flight2 = flight2;
	    this.flightTime = flightTime;
	    this.isDirectFlight = isDirectFlight;
	}

	@Override
	/**
	 * Provides for sort between Itineraries.
	 */
	public int compareTo(Itinerary other) {
	    // Order by flight time.
	    if (this.flightTime != other.flightTime) {
		return Integer.compare(this.flightTime, other.flightTime);
	    }

	    // Break flight time ties with first flight id.
	    if (this.flight1.fid != other.flight1.fid) {
		return Integer.compare(this.flight1.fid, other.flight1.fid);
	    }

	    // Break first flight id ties with second flight id. 
	    // (Only if both have second flight ids).
	    if (!(this.isDirectFlight || other.isDirectFlight)) {
		return Integer.compare(this.flight2.fid, other.flight2.fid);
	    }

	    // If only one itinerary has a second flight,
	    // the itinerary with the direct flight is ordered first. 
	    int comparison = (this.isDirectFlight) ? -1 : 1;
	    return comparison;
	}
    }
}
