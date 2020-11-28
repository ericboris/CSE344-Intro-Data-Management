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

    // Used to clear the BookedSeats table.
    private static final String CLEAR_BOOKED_SEATS = "DELETE FROM BookedSeats;";
    private PreparedStatement clearBookedSeatsStatement;

    // Used to check if username already exists
    private static final String CHECK_USERNAME_EXISTS = "SELECT * FROM Users WHERE username = ?;";
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
    
    // Used to get the top one-hop flights from src to dst on a given day in July 2015.
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
					       + "       f2.dest_city AS f2_dest_city,"  
					       + "       f2.actual_time AS f2_actual_time,"  
					       + "       f2.capacity AS f2_capacity,"  
					       + "       f2.price AS f2_price"  + "  FROM flights as f1,"
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

    // Add a new Reservation to the Reservations table.
    private static final String ADD_RESERVATION = "INSERT INTO Reservations"
						    + " VALUES (?, ?, ?, ?, ?, ?);";  
    private PreparedStatement addReservationStatement;

    // Get Reservation ID from the Reservations table.
    private static final String GET_RESERVATION_ID = "SELECT id AS id"
						   + "  FROM Reservations"
						   + " WHERE username = ?"
						   + "   AND price = ?"
						   + "   AND paid = ?"
						   + "   AND cancelled = ?"
						   + "   AND fid1 = ?;";
    private PreparedStatement getReservationIdStatement;

    // Get a user's reservations on a given day.
    /*
    private static final String GET_USER_RESERVATIONS_ON_DAY = "SELECT COUNT(*) AS count"
							 + "  FROM Flights AS f,"
							 + "       (SELECT fid1"
							 + "          FROM Reservations"
							 + "         WHERE username = ?) AS r"
							 + " WHERE r.fid1 = f.fid"
							 + "   AND f.day_of_month = ?;"; 
    */
    private static final String GET_USER_RESERVATIONS_ON_DAY = "SELECT *"
							 + "  FROM Flights AS f,"
							 + "       (SELECT fid1"
							 + "          FROM Reservations"
							 + "         WHERE username = ?) AS r"
							 + " WHERE r.fid1 = f.fid"
							 + "   AND f.day_of_month = ?;"; 

    private PreparedStatement getUserReservationsOnDayStatement;

    // Get the number of booked seats on a given flight.
    private static final String GET_FLIGHTS_BOOKED_SEATS = "SELECT seats"
						       + "  FROM BookedSeats"
						       + " WHERE fid = ?;";
    private PreparedStatement getFlightsBookedSeatsStatement;

    // Increment the number of booked seats on a given flight.
    private static final String INCREMENT_FLIGHTS_BOOKED_SEATS = "UPDATE BookedSeats"
							       + "   SET seats = seats + 1"
							       + " WHERE fid = ?;";
    private PreparedStatement incrementFlightsBookedSeatsStatement;

    // Return whether the given username booked the given reservation.
    private static final String DID_USER_BOOK_RESERVATION = "SELECT username"
							  + "  FROM Reservations"
							  + " WHERE id = ?;";
							 // + "   AND username = ?" // TODO Uncomment
							 // + "   AND paid = 0"
							 // + "   AND cancelled = 0;";
    private PreparedStatement didUserBookReservationStatement;

    // Return the attribute for the given reservationId.
    private static final String GET_RESERVATION_ATTRIBUTE = "SELECT ?"
							  + "  FROM Reservations"
							  + " WHERE id = ?;";
    private PreparedStatement getReservationAttributeStatement;
    
    // Update the attribute for the given reservationId with the given value.
    private static final String UPDATE_RESERVATION_ATTRIBUTE = "UPDATE Reservations"
							     + "   SET ? = ?"
							     + " WHERE id = ?"; 
    private PreparedStatement updateReservationAttributeStatement;

    // Return the attribute for the given username.
    private static final String GET_USER_ATTRIBUTE = "SELECT ?"
						   + "  FROM Users"
						   + " WHERE username = ?;";
    private PreparedStatement getUserAttributeStatement;
    
    // Update the attribute for the given username with the given value.
    private static final String UPDATE_USER_ATTRIBUTE = "UPDATE Users"
						      + "   SET ? = ?"
						      + " WHERE username = ?"; 
    private PreparedStatement updateUserAttributeStatement;

    // Get reservation cost TODO clarify description
    private static final String GET_RESERVATION_PRICE = "Select price"
						      + "FROM Reservations"
						      + "WHERE id = ?;";
    private PreparedStatement getReservationPriceStatement;

    // Get the given user's reservations.
    private static final String GET_USER_RESERVATIONS = "Select *"
						       + "FROM Reservations"
						       + "WHERE username = ?"
						       + " AND cancelled = 0;";
    private PreparedStatement getUserReservationsStatement;

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
	    clearUsersStatement.executeUpdate();

	    // Clear the Reservations table.
	    clearReservationsStatement.executeUpdate();

	    // Clear the Booked Seats table.
	    clearBookedSeatsStatement.executeUpdate();
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
	clearBookedSeatsStatement = conn.prepareStatement(CLEAR_BOOKED_SEATS);
	checkUsernameExistsStatement = conn.prepareStatement(CHECK_USERNAME_EXISTS);
	insertUserDataStatement = conn.prepareStatement(INSERT_USER_DATA);
	userLoginStatement = conn.prepareStatement(USER_LOGIN);
	directFlightStatement = conn.prepareStatement(DIRECT_FLIGHT);
	oneHopFlightStatement = conn.prepareStatement(ONE_HOP_FLIGHT);
	addReservationStatement = conn.prepareStatement(ADD_RESERVATION);
	getUserReservationsOnDayStatement = conn.prepareStatement(GET_USER_RESERVATIONS_ON_DAY);
	getFlightsBookedSeatsStatement = conn.prepareStatement(GET_FLIGHTS_BOOKED_SEATS);
	incrementFlightsBookedSeatsStatement = conn.prepareStatement(INCREMENT_FLIGHTS_BOOKED_SEATS);
	getReservationIdStatement = conn.prepareStatement(GET_RESERVATION_ID);
	didUserBookReservationStatement = conn.prepareStatement(DID_USER_BOOK_RESERVATION);
	getReservationAttributeStatement = conn.prepareStatement(GET_RESERVATION_ATTRIBUTE);
	updateReservationAttributeStatement = conn.prepareStatement(UPDATE_RESERVATION_ATTRIBUTE);
	getUserAttributeStatement = conn.prepareStatement(GET_USER_ATTRIBUTE);
	updateUserAttributeStatement = conn.prepareStatement(UPDATE_USER_ATTRIBUTE);
	getReservationPriceStatement = conn.prepareStatement(GET_RESERVATION_PRICE);
	getUserReservationsStatement = conn.prepareStatement(GET_USER_RESERVATIONS);
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
			// Clear any existing itineraries for the new user.
			this.itineraries = new ArrayList<>();

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
	    boolean usernameExists = isUsernameTaken(username);
	    if (usernameExists || initAmount < 0) {
		return "Failed to create user\n";
	    }
	   
	    // Salt and hash the password. 
	    byte[] salt = getSalt();
	    byte[] hash = getHash(password, salt);

	    // Add a new user to the Users table.
	    addUser(username, salt, hash, initAmount);

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
     * TODO
     */
    private boolean isUsernameTaken(String username) throws SQLException {
	try {
	    checkUsernameExistsStatement.clearParameters();
	    checkUsernameExistsStatement.setString(1, username.toLowerCase());

	    ResultSet checkUsernameExistsResult = checkUsernameExistsStatement.executeQuery();

	    if (checkUsernameExistsResult.next()) {
		return true;
	    } else {
		return false;
	    }
	} catch (SQLException e) {
	    throw e;
	}
    }

    /**
     * TODO
     */
    private boolean addUser(String username, byte[] salt, byte[] hash, int amount) throws SQLException {
	try {
	    insertUserDataStatement.clearParameters();
	    insertUserDataStatement.setString(1, username.toLowerCase());
	    insertUserDataStatement.setBytes(2, salt);
	    insertUserDataStatement.setBytes(3, hash);
	    insertUserDataStatement.setInt(4, amount);

	    insertUserDataStatement.executeUpdate();
	    
	    return true;
	} catch (SQLException e) {
	    throw e;
	}
    }

    /**
     * TODO
     */
    private byte[] getSalt() {
	// Generate a random crypographic salt.
	SecureRandom random = new SecureRandom();
	byte[] salt = new byte[16];
	random.nextBytes(salt);
	return salt;
    }

    /**
     * TODO
     */
    private byte[] getHash(String password, byte[] salt) {
	// Specify the hash parameters.
	KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);

	// Generate the hash.
	SecretKeyFactory factory = null;
	try {
	    factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
	    return factory.generateSecret(spec).getEncoded();
	} catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
	    throw new IllegalStateException();
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
		// Get all the direct flights, regardless of directFlight boolean value.

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
		// Give each itinerary a unique number from 0 to n.
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
    public String transaction_book(int itineraryId) { // currenthead
	try {
	    // A user must be logged in to book.
	    if (this.currentUser == null) {
		return "Cannot book reservations, not logged in\n";
	    }

	    // The user must have performed a search in the current session
	    // and the itinerary id must be a valid itinerary in that search.
	    if (this.itineraries.size() == 0 || itineraryId < 0 || this.itineraries.size() <= itineraryId) {
		return "No such itinerary " + itineraryId + "\n";
	    }
	  
	    // Let itinerary be a convenience variable since it will be accessed numerous times.
	    Itinerary itinerary = itineraries.get(itineraryId);

	    // The user cannot book multiple flights on the same day. 
	    // Let itineraryDay be the day of the month that the user is trying to book.
	    int itineraryDay = itinerary.flight1.dayOfMonth;
	    boolean userHasReservationOnDay = doesUserHaveReservationOnDay(this.currentUser, itineraryDay);
	    if (userHasReservationOnDay) {
		return "You cannot book two flights in the same day\n";
	    }
	    


	    // The user cannot book if there is insufficient capacity on either flight.

	    // Get the available seats on the flight1.
	    int flight1Capacity = itinerary.flight1.capacity;
	    int flight1Fid = itinerary.flight1.fid;

	    // Query the booked seats table for how many seats are booked on flight1.
	    int flight1BookedSeats = 0;
	    try {
		getFlightsBookedSeatsStatement.clearParameters();
		getFlightsBookedSeatsStatement.setInt(1, flight1Fid);
		ResultSet flight1BookedSeatsResult = getFlightsBookedSeatsStatement.executeQuery();
		if (flight1BookedSeatsResult.next()) {
		    flight1BookedSeats = flight1BookedSeatsResult.getInt("seats");
		}
		flight1BookedSeatsResult.close();
	    } catch (SQLException e) {
		e.printStackTrace();
		System.out.println("failed to get flight1 booked seats query");
		return "Booking failed\n";
	    }
	    int flight1AvailableSeats = flight1Capacity - flight1BookedSeats;

	    int flight2AvailableSeats = -1;
	    int flight2BookedSeats = 0;

	    // If the itinerary has a second flight, get the available seats on flight2.
	    if (!itinerary.isDirectFlight) {
		int flight2Capacity = itinerary.flight2.capacity;
		int flight2Fid = itinerary.flight2.fid;

		// Query the booked seats table for how many seats are booked on flight2;
		try {
		    getFlightsBookedSeatsStatement.clearParameters();
		    getFlightsBookedSeatsStatement.setInt(1, flight2Fid);
		    ResultSet flight2BookedSeatsResult = getFlightsBookedSeatsStatement.executeQuery();
		    flight2BookedSeats = flight2BookedSeatsResult.getInt("seats");
		    flight2BookedSeatsResult.close();
		} catch (SQLException e) {
		    e.printStackTrace();
		    System.out.println("failed to get flight2 booked seats query");
		    return "Booking failed\n";
		}

		flight2AvailableSeats = flight2Capacity - flight2BookedSeats;
	    }
	    
	    // The user cannot book the flight if there isn't available seating.
	    if (flight1AvailableSeats == 0 || flight2AvailableSeats == 0) {
		System.out.println("insufficient seating");
		return "Booking failed\n";
	    }

	    // The conditions are met and the itinerary can be booked.

	    // Add the flights in the itinerary to reservations.
	    // Reservation needs to have: rid, username, price, paid, cancelled, fid1, fid2
	    // but already have variables for rid, username, fid1, and fid2.
	    int paid = (itinerary.paid) ? 1 : 0;
	    int cancelled = (itinerary.cancelled) ? 1 : 0;     	    

	    try {
		addReservationStatement.clearParameters();	
	
		addReservationStatement.setString(1, this.currentUser);
		addReservationStatement.setInt(2, itinerary.price);
		addReservationStatement.setInt(3, paid);
		addReservationStatement.setInt(4, cancelled);
		addReservationStatement.setInt(5, flight1Fid);

		if (!itinerary.isDirectFlight) {
		    addReservationStatement.setInt(6, itinerary.flight2.fid);
		} else {
		    addReservationStatement.setNull(6, Types.INTEGER);
		}
    
		addReservationStatement.executeUpdate();
	    } catch (SQLException e) {
		e.printStackTrace();
		System.out.println("failed add reservation query");
		return "Booking failed\n";   
	    }

	    // Update the booked seats on the flight(s) in the itinerary.
	    try {
		incrementFlightsBookedSeatsStatement.clearParameters();
		incrementFlightsBookedSeatsStatement.setInt(1, flight1Fid);
		incrementFlightsBookedSeatsStatement.executeUpdate();

		if (!itinerary.isDirectFlight) {
		    incrementFlightsBookedSeatsStatement.clearParameters();
		    incrementFlightsBookedSeatsStatement.setInt(1, itinerary.flight2.fid);
		    incrementFlightsBookedSeatsStatement.executeUpdate();
		}
	    } catch (SQLException e) {
		e.printStackTrace();
		System.out.println("failed to increment booked seats query");
		return "Booking failed\n";   
	    }

	    boolean userHasReservation = doesUserHaveReservation(this.currentUser);
	    if (userHasReservation) {
		System.out.println("USER HAS RESERVATION");
	    } else {
		System.out.println("USER HAS NO RESERVATION");
	    }

	    // Get the reservation id that was just created.
	    int reservationId = -1;
	    try {
		getReservationIdStatement.clearParameters();
    
		getReservationIdStatement.setString(1, this.currentUser);
		getReservationIdStatement.setInt(2, itinerary.price);
		getReservationIdStatement.setInt(3, paid);
		getReservationIdStatement.setInt(4, cancelled);
		getReservationIdStatement.setInt(5, flight1Fid);
	   
		ResultSet getReservationIdResult = getReservationIdStatement.executeQuery();
	    
		if (getReservationIdResult.next()) {
		    reservationId = getReservationIdResult.getInt("id");	
		}

		getReservationIdResult.close();
	    } catch (SQLException e) {
		e.printStackTrace();
		System.out.println("failed to get reservation id");
		return "Booking failed\n";
	    }

	    // If everything has worked then notify that the booking was successful.
	    return "Booked flight(s), reservation ID: " + reservationId + "\n";
	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * TODO
     */
    private boolean doesUserHaveReservationOnDay(String username, int itineraryDay) {
	// Query the reservations table for any flights by the user on itineraryDay.
	try {
	    getUserReservationsOnDayStatement.clearParameters();

	    getUserReservationsOnDayStatement.setString(1, this.currentUser);
	    getUserReservationsOnDayStatement.setInt(2, itineraryDay);

	    ResultSet userReservationsResult = getUserReservationsOnDayStatement.executeQuery();
	    //int count = -1;
	    // Let count be the number of reservations the user for the day.
	    if (userReservationsResult.next()) {
		//count = userReservationsResult.getInt("count");
		System.out.println("user has reservation");
		return true;
	    } else {
		return false;
		/*
		// Can't have multiple flights booked on a single day.
		if (count > 0) {
		return "You cannot book two flights in the same day\n";
		} */
	    }
	} catch (SQLException e) {
	    //e.printStackTrace();
	    return false;
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
	    // A user must be logged in to pay.
	    if (this.currentUser == null) {
		return "Cannot pay, not logged in\n";
	    }

	    // The current user must have booked the reservationId.
	    if (!userBookedReservationId(reservationId)) {
		return "Cannot find unpaid reservation " + reservationId + " under user: " + this.currentUser + "\n";
	    }  

	    //int reservationPrice = getReservationAttribute(reservationId, "price");
	    int reservationPrice = getReservationPrice(reservationId);
	    int userBalance = getUserAttribute(this.currentUser, "balance");
	    System.out.println("reservationPrice: " + reservationPrice);
	    System.out.println("userBalance:      " + userBalance);

	    // The current user must have enough funds in their account to pay for the reservation.
	    int newUserBalance = reservationPrice - userBalance;
	    if (newUserBalance < 0) {
		return "User has only " + userBalance + " in account but itinerary costs " + reservationPrice + "\n";
	    }

	    // Pay for the reservation.

	    // Remove the cost of the reservation from the users account.
	    boolean updateBalanceWasSuccessful = updateUserAttribute(this.currentUser, "balance", newUserBalance);
	    if (!updateBalanceWasSuccessful) {
		return "Failed to pay for reservation " + reservationId + "\n";
	    }

	    // Update the reservation paid attribute to true.
	    boolean updatePaidWasSuccessful = updateReservationAttribute(reservationId, "paid", "true");
	    if (!updatePaidWasSuccessful) {
		return "Failed to pay for reservation " + reservationId + "\n";
	    }  

	    // The payment was succesful, notify.
	    return "Paid reservation: " + reservationId + " remaining balance: " + newUserBalance + "\n";
 	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * Returns a boolean of whether the currently logged in user booked the given reservationid.
     * Assumes that a user is currently logged in.
     *
     * @param reservationId the reservationId to check against.
     * 
     * @return true if the current user booked the given reservationId and false otherwise.
     */
    private boolean userBookedReservationId(int reservationId) {
	try {
	    didUserBookReservationStatement.clearParameters();
	    didUserBookReservationStatement.setInt(1, reservationId);
	    //didUserBookReservationStatement.setString(2, this.currentUser);
	    ResultSet didUserBookReservationResult = didUserBookReservationStatement.executeQuery();
	    boolean result = (didUserBookReservationResult.next()) ? true : false;
	    return result;
	} catch (SQLException e) {
	    return false;
	} 
    }
   
    /**
     * Queries the Reservations table for the price of the given reservationId.
     * 
     * @param reservationId the reservationId to search for.
     * 
     * @return an integer representing the cost of the reservation.
     */
    private int getReservationPrice(int reservationId) {
	try {
	    getReservationPriceStatement.setInt(1, reservationId);
	    ResultSet getReservationPriceResult = getReservationPriceStatement.executeQuery();

	    int reservationPrice = 0;
	    while (getReservationPriceResult.next()) {
		reservationPrice += getReservationPriceResult.getInt(1);
	    }
	    return reservationPrice;
	} catch (SQLException e) {
	    return -1;
	}
    } 


    /**
     * Gets the attribute of the given reservationId from the ReservationsTable.
     * 
     * @param reservationId the reservationId to search for.
     * @param attribute the attribute to get.
     * 
     * @return the value of the attribute for the reservationId.
     */
    private int getReservationAttribute(int reservationId, String attribute) {
	int result = -1;
	try {
	    getReservationAttributeStatement.clearParameters();
	    getReservationAttributeStatement.setInt(2, reservationId);
	    getReservationAttributeStatement.setString(1, attribute);
	    ResultSet getReservationAttributeResult = getReservationAttributeStatement.executeQuery();
	    
	    result = (getReservationAttributeResult.next()) ? getReservationAttributeResult.getInt(attribute) : -1;

	    getReservationAttributeResult.close();
	} catch (SQLException e) {
	    result = -1;
	} finally {
	    return result;
	}
    }

    /**
     * Gets the attribute of the given username from the Users table.
     * 
     * @param username the username to search for.
     * @param attribute the attribute to get.
     * 
     * @return the value of the attribute for the username.
     */

    private int getUserAttribute(String username, String attribute) {
	int result = -1;
	try {
	    System.out.println("gua: 1");
	    getUserAttributeStatement.clearParameters();
	    System.out.println("gua: 2");
	    getUserAttributeStatement.setString(2, username);
	    System.out.println("gua: 3");
	    getUserAttributeStatement.setString(1, attribute);
	    System.out.println("gua: 4");
	    ResultSet getUserAttributeResult = getUserAttributeStatement.executeQuery();
	    System.out.println("gua: 5");
	    if (getUserAttributeResult.next()) {
		System.out.println("gua: in if");
		result = Integer.parseInt(getUserAttributeResult.getString(attribute));
	    } 
	    System.out.println("get user attribute query succeeded, result = " + result);
	    
	    getUserAttributeResult.close();
	} catch (SQLException e) {
	    result = -1;
	} finally {
	    return result;
	}
    }

    /**
     * Queries the Users table for the account balance of the given username.
     * 
     * @param username the username to search for.
     * 
     * @return The account balance of the given user.
     */
    /*
    private int getUserBalance(String username) {
	result = -1;
	try {
	    getUserBalanceStatement.clearParameters();
	    getUserBalanceStatement.setInt(1, reservationId);
	    ResultSet getUserBalanceResult = getUserBalanceStatement.executeQuery();

	    result = (getUserBalanceResult.next()) ? getUserBalanceResult.getInt("price") : -1;
	} catch (SQLException e) {
	    result = -1;
	} finally {
	    getUserBalanceResult.close();
	    return result;
	}
    }*/ 

    /**
     * Changes the attribute for the given username to the given value.
     * 
     * @param username the username to change an attribute of.
     * @param attribute the attribute to change the value of.
     * @param value the value to set.
     * 
     * @return true if the update was successful and false otherwise.
     */
    private boolean updateUserAttribute(String username, String attribute, int value) {
	try {
	    updateUserAttributeStatement.clearParameters();
	    updateUserAttributeStatement.setString(3, username);
	    updateUserAttributeStatement.setString(1, attribute);
	    updateUserAttributeStatement.setInt(2, value);
	    updateUserAttributeStatement.executeUpdate();
	    
	    return true;
	} catch (SQLException e) {
	    e.printStackTrace();
	    return false;
	}
    }

    /**
     * Removes the given amount from the given users account balance in Users table.
     * 
     * @param username the username to change the account balance of.
     * @param amount the new amount to set the users account balance to.
     * 
     * @return true if the update was successful and false otherwise.
     */
    /*
    private boolean updateUserBalance(String username, int amount) {
	try {
	    updateUserBalanceStatement.clearParameters();
	    updateUserBalanceStatement.setString(1, username);
	    updateUserBalanceStatement.setInt(2, amount);
	    updateUserBalanceStatement.executeUpdate();
	    
	    return true;
	} catch (SQLException e) {
	    e.printStackTrace();
	    return false;
	}
    }*/ 

    /**
     * Update the reservationId's attribute to the value.
     * 
     * @param reservationId the id to update. 
     * @param attribute the attribute to update.
     * @param value the value to update.
     * 
     * @return true if the update was successful and false otherwise.
     */
    private boolean updateReservationAttribute(int reservationId, String attribute, String value) {
	try {
	    updateReservationAttributeStatement.clearParameters();
	    updateReservationAttributeStatement.setInt(3, reservationId);
	    updateReservationAttributeStatement.setString(1, attribute);
	    
	    switch (value) {
		case "true":
		    int state = 1;
		    updateReservationAttributeStatement.setInt(2, state);
		default:
		    updateReservationAttributeStatement.setNull(2, Types.INTEGER);	    
	    }
	    
	    updateReservationAttributeStatement.executeUpdate();
	    
	    return true;
	} catch (SQLException e) {
	    e.printStackTrace();
	    return false;
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
	    // A user must be logged in to view reservations.
	    if (this.currentUser == null) {
		return "Cannot view reservations, not logged in\n";
	    }

	    // Check that user indeed has reservations.
	    boolean userHasReservation = doesUserHaveReservation(this.currentUser);
	    if (!userHasReservation) {
		return "No Reservations found\n";
	    }

	    System.out.println("User has reservation.>");

	    
		
	    return "Failed to retrieve reservations\n";
	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * TODO
     */
    private boolean doesUserHaveReservation(String username) {
	try {
	    getUserReservationsStatement.clearParameters();
	    getUserReservationsStatement.setString(1, username);
	    ResultSet getUserReservationsResult = getUserReservationsStatement.executeQuery();
	    
	    if (getUserReservationsResult.next()) {
		return true;
	    } else {
		return false;
	    }
	} catch (SQLException e) {
	    return false;
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
	public int price;
	public boolean paid;
	public boolean cancelled;

	/**
	 * Class constructor.
	 */
	public Itinerary(Flight flight1, Flight flight2, int flightTime, boolean isDirectFlight) {
	    this.flight1 = flight1;
	    this.flight2 = flight2;
	    this.flightTime = flightTime;
	    this.isDirectFlight = isDirectFlight;
	    this.price = (isDirectFlight) ? flight1.price : flight1.price + flight2.price;
	    this.paid = false;
	    this.cancelled = false;
	}

	@Override
	/**
	 * Provides a method for sorting Itineraries.
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
