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

    // Let user be the currently logged in username.
    private String user;

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

    // Reset Reservations Identity seed.
    private static final String RESET_RESERVATIONS_IDENTITY = "DBCC CHECKIDENT ('Reservations', RESEED, 0);";
    private PreparedStatement resetReservationsIdentityStatement;

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
					       + " 	 f2.flight_num AS f2_flight_num,"
					       + "	 f2.origin_city AS f2_origin_city,"
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
						    + " VALUES (?, ?, ?, ?, ?, ?, ?);";  
    private PreparedStatement addReservationStatement;

    // Get Reservation ID from the Reservations table.
    private static final String GET_RESERVATION_ID = "SELECT id AS id"
						   + "  FROM Reservations"
						   + " WHERE username = ?"
						   + "   AND price = ?"
						   + "   AND paid = ?"
						   + "   AND canceled = ?"
						   + "   AND fid1 = ?;";
    private PreparedStatement getReservationIdStatement;

    // Get a user's reservations on a given day.
    /*
    private static final String GET_USER_OPEN_RESERVATIONS_ON_DAY = "SELECT COUNT(*) AS count"
							 + "  FROM Flights AS f,"
							 + "       (SELECT fid1"
							 + "          FROM Reservations"
							 + "         WHERE username = ?) AS r"
							 + " WHERE r.fid1 = f.fid"
							 + "   AND f.day_of_month = ?;"; 
    */
    private static final String GET_USER_OPEN_RESERVATIONS_ON_DAY = "SELECT *"
							 + "  FROM Flights AS f,"
							 + "       (SELECT fid1"
							 + "          FROM Reservations"
							 + "         WHERE username = ?) AS r"
							 + " WHERE r.fid1 = f.fid"
							 + "   AND f.day_of_month = ?;"; 

    private PreparedStatement getUserReservationsOnDayStatement;

    // Get a byte array attribute from the Users table.
    private static final String GET_USER_BYTE_ATTRIBUTE = "SELECT ?"
							+ "FROM Users"
							+ "WHERE username = ?;";
    private PreparedStatement getUserByteAttributeStatement;

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
							  + " WHERE id = ?"
							  + "   AND username = ?" 
							  + "   AND paid = 0"
							  + "   AND canceled = 0;";
    private PreparedStatement userHasUnpaidReservationStatement;

    // Return the attribute for the given reservationId.
    private static final String GET_RESERVATION_ATTRIBUTE = "SELECT ?"
							  + "  FROM Reservations"
							  + " WHERE id = ?;";
    private PreparedStatement getReservationAttributeStatement;
    
    // Update the attribute for the given reservationId with the given value.
    private static final String UPDATE_RESERVATION_PAID = "UPDATE Reservations"
							     + "   SET paid = ?"
							     + " WHERE id = ?"; 
    private PreparedStatement updateReservationPaidStatement;

    // Return the attribute for the given username.
    private static final String GET_USER_BALANCE = "SELECT balance"
						   + "  FROM Users"
						   + " WHERE username = ?;";
    private PreparedStatement getUserBalanceStatement;
    
    // Update the attribute for the given username with the given value.
    private static final String UPDATE_USER_BALANCE = "UPDATE Users"
						      + "   SET balance = ?"
						      + " WHERE username = ?"; 
    private PreparedStatement updateUserBalanceStatement;

    // Get reservation cost TODO clarify description
    private static final String GET_RESERVATION_PRICE = "Select price from Reservations where id = ?;";
    private PreparedStatement getReservationPriceStatement;

    // Get the given user's reservations.
    private static final String GET_USER_OPEN_RESERVATIONS = "Select id, price, paid, canceled, fid1, fid2 From Reservations WHERE username = ? and canceled = 0;";
    private PreparedStatement getOpenReservations;

    // Get the given user's salt.
    private static final String GET_USER_SALT = "Select salt FROM Users WHERE username = ?;";
    private PreparedStatement getUserSaltStatement;

    // Get the given user's hash.
    private static final String GET_USER_HASH = "Select hash FROM Users WHERE username = ?;";
    private PreparedStatement getUserHashStatement;

    // Get the reservation information for the given reservation id.
    private static final String GET_RESERVATION = "Select id, username, price, paid, canceled, fid1, fid2 from Reservations where id = ?;";
    private PreparedStatement getReservationStatement;

    // Get the size of the reservations table.
    private static final String GET_RESERVATIONS_SIZE = "Select count(*) as count From reservations;";
    private PreparedStatement getReservationsSizeStatement;

    // Get a flight's information via fid.
    private static final String GET_FLIGHT = "Select day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price FROM Flights WHERE fid = ?;";
    private PreparedStatement getFlightStatement;

    // Change a reservation's cancellation attribute status.
    private static final String SET_RESERVATION_CANCELLATION = "UPDATE Reservations SET canceled = ? WHERE id = ?;";
    private PreparedStatement setReservationCancellationStatement;
   
    // Returns results there's a user in the Reservations table with the given reservation Id. 
    private static final String RESERVATION_MATCHES_USER = "SELECT * FROM Reservations WHERE id = ? AND username = ? AND canceled = 0;";
    private PreparedStatement reservationMatchesUserStatement;

    /**
     * Class constructor.
     */
    public Query() throws SQLException, IOException {
	this(null, null, null, null);
	this.itineraries = new ArrayList<>();
	this.user = null;
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
	    //resetReservationsIdentityStatement.executeUpdate();

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
	getUserReservationsOnDayStatement = conn.prepareStatement(GET_USER_OPEN_RESERVATIONS_ON_DAY);
	getFlightsBookedSeatsStatement = conn.prepareStatement(GET_FLIGHTS_BOOKED_SEATS);
	incrementFlightsBookedSeatsStatement = conn.prepareStatement(INCREMENT_FLIGHTS_BOOKED_SEATS);
	getReservationIdStatement = conn.prepareStatement(GET_RESERVATION_ID);
	userHasUnpaidReservationStatement = conn.prepareStatement(DID_USER_BOOK_RESERVATION);
	getReservationAttributeStatement = conn.prepareStatement(GET_RESERVATION_ATTRIBUTE);
	updateReservationPaidStatement = conn.prepareStatement(UPDATE_RESERVATION_PAID);
	getUserBalanceStatement = conn.prepareStatement(GET_USER_BALANCE);
	updateUserBalanceStatement = conn.prepareStatement(UPDATE_USER_BALANCE);
	getReservationPriceStatement = conn.prepareStatement(GET_RESERVATION_PRICE);
	getOpenReservations = conn.prepareStatement(GET_USER_OPEN_RESERVATIONS);
	getUserByteAttributeStatement = conn.prepareStatement(GET_USER_BYTE_ATTRIBUTE);
	getUserSaltStatement = conn.prepareStatement(GET_USER_SALT);
	getUserHashStatement = conn.prepareStatement(GET_USER_HASH);
	getReservationStatement = conn.prepareStatement(GET_RESERVATION);
	resetReservationsIdentityStatement = conn.prepareStatement(RESET_RESERVATIONS_IDENTITY);
	getReservationsSizeStatement = conn.prepareStatement(GET_RESERVATIONS_SIZE);
	getFlightStatement = conn.prepareStatement(GET_FLIGHT);
	setReservationCancellationStatement = conn.prepareStatement(SET_RESERVATION_CANCELLATION);
	reservationMatchesUserStatement = conn.prepareStatement(RESERVATION_MATCHES_USER);
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
	    // Prevent multiple users from being logged in simultaneously.
	    if (this.user != null) {
		System.out.println("SOMEONE LOGGED IN");
		return "User already logged in\n";
	    }

	    // Check whether the given username and password combination are valid.
	    
	    // Get the salt and hash of the stored username.
	    byte[] storedSalt = getUserSalt(username);
	    byte[] storedHash = getUserHash(username);
	    if (storedSalt == null || storedHash == null) {
		System.out.println("SALT OR HASH FAIL");
	       return "Login failed\n";
	    }

	    // Generate a hash from the user's salt.
	    byte[] generatedHash = getHash(password, storedSalt);

	    // If the stored and generated hashes are equal
	    // then the username an password are valid.
	    if (Arrays.equals(storedHash, generatedHash)) {
		this.itineraries = new ArrayList<>();
		this.user = username;
		return "Logged in as " + username + "\n";
	    }
   
	    System.out.println("HASH NOT EQUAL"); 
	    return "Login failed\n";

	} catch (SQLException e) {
	    e.printStackTrace();
	    // Notify if any of the above failed.
	    System.out.println("SQLEXECEPTION");
	    return "Login failed\n";
	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * TODO
     */
    public byte[] getUserSalt(String username) throws SQLException {
	try {
	    getUserSaltStatement.clearParameters();
	    getUserSaltStatement.setString(1, username.toLowerCase());

	    ResultSet getUserSaltResult = getUserSaltStatement.executeQuery();
	    if (getUserSaltResult.next()) {
		return getUserSaltResult.getBytes("salt");
	    } else {
		return null;
	    }
	} catch (SQLException e) {
	    throw e;
	}
    }

    /**
     * TODO
     */
    public byte[] getUserHash(String username) throws SQLException {
	try {
	    getUserHashStatement.clearParameters();
	    getUserHashStatement.setString(1, username.toLowerCase());

	    ResultSet getUserHashResult = getUserHashStatement.executeQuery();
	    if (getUserHashResult.next()) {
		return getUserHashResult.getBytes("hash");
	    } else {
		return null;
	    }
	} catch (SQLException e) {
	    throw e;
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
	for (int i = 0; i < 3; i++) {
	    try {
		conn.setAutoCommit(false);

		// Verify that the username doesn't alredy exist
		// and that the initAmount is valid.
		boolean usernameExists = isUsernameTaken(username);
		if (usernameExists || initAmount < 0) {
		    conn.setAutoCommit(true);
		    return "Failed to create user\n";
		}

		// Salt and hash the password. 
		byte[] salt = getSalt();
		byte[] hash = getHash(password, salt);

		// Add a new user to the Users table.
		addUser(username, salt, hash, initAmount);

		conn.commit();
		conn.setAutoCommit(true);

		// Notify that the insertion was successful.
		return "Created user " + username + "\n";

	    } catch (SQLException e) {
		try {
		    if (isDeadLock(e)) {
			conn.rollback();
		    }
		    conn.setAutoCommit(true);
		} catch (SQLException e2) {

		    e2.printStackTrace();
		}
	    } finally {
		checkDanglingTransaction();
	    }
	}
	return "Failed to create user\n";
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
	// Reinitialize itineraries to an empty list;
	List<Itinerary> localItineraries = new ArrayList<>();

	try {
	    // Get the direct flights
	    localItineraries = getDirectFlights(localItineraries, numberOfItineraries, originCity, destinationCity, dayOfMonth); 

	    // Get the number of itineraries currently stored.
	    int currNumItineraries = localItineraries.size();

	    // Get the number of remaining itinerary availabilities.
	    int numItinerarySpaces = numberOfItineraries - currNumItineraries;

	    // If possible, add any indirect flight itineraries to itineraries too.	
	    // Can only accept indirect flights if, they are admissible
	    // and there aren't already too many itineraries stored.
	    if (numItinerarySpaces > 0 && directFlight == false) {
		localItineraries = getIndirectFlights(localItineraries, numItinerarySpaces, originCity, destinationCity, dayOfMonth);

		// If indirect flights have been added to itineraries,
		// then itineraries needs to be sorted by ascending flight times.
		Collections.sort(localItineraries);
	    } 		    

	    // Save the new itineraries.
	    this.itineraries = localItineraries;

	    // Generate and format the output string.
	    StringBuffer sb = itineraryStringBuffer(itineraries);
	    String output = (sb.length() == 0) ? "No flights match your selection\n" : sb.toString();
	    
	    return output;

	} catch (SQLException e) {
	    e.printStackTrace();
	    return "Failed to search\n";
	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * TODO
     */
    private StringBuffer itineraryStringBuffer(List<Itinerary> itineraries) {
	StringBuffer sb = new StringBuffer();	

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
	return sb;
    }


    /**
     * TODO
     */
    private List<Itinerary> getDirectFlights(List<Itinerary> itineraries, int max, String origin, String destination, int day) throws SQLException {
	try {
	    // Fill itineraries with flight itineraries.
	    // Get all the direct flights, regardless of directFlight boolean value.

	    // Prepare the query.
	    directFlightStatement.clearParameters();
	    directFlightStatement.setInt(1, max);
	    directFlightStatement.setString(2, origin);
	    directFlightStatement.setString(3, destination);
	    directFlightStatement.setInt(4, day);

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
    
	    return itineraries;
	} catch (SQLException e) {
	    throw e;
	}
    }

    /**
     * TODO
     */
    private List<Itinerary> getIndirectFlights(List<Itinerary> itineraries, int max, String origin, String destination, int day) throws SQLException {
	try {
	    // Prepare the query.
	    oneHopFlightStatement.clearParameters();

	    oneHopFlightStatement.setInt(1, max);
	    oneHopFlightStatement.setString(2, origin);
	    oneHopFlightStatement.setString(3, destination);
	    oneHopFlightStatement.setInt(4, day);

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

	    return itineraries;
	} catch (SQLException e) {
	    throw e;
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
	for (int i = 0; i < 3; i++) {
	    try {
		// A user must be logged in to book.
		if (this.user == null) {
		    return "Cannot book reservations, not logged in\n";
		}

		// The user must have performed a search in the current session
		// and the itinerary id must be a valid itinerary in that search.
		if (this.itineraries.size() == 0 || this.itineraries.size() <= itineraryId) {
		    return "No such itinerary " + itineraryId + "\n";
		}

		String username = this.user;
		Itinerary itinerary = itineraries.get(itineraryId);

		// The user cannot book multiple flights on the same day. 
		// Let itineraryDay be the day of the month that the user is trying to book.
		boolean userAlreadyHasReservation = doesUserHaveReservationOnDay(username, itinerary.flight1.dayOfMonth);
		if (userAlreadyHasReservation) {
		    return "You cannot book two flights in the same day\n";
		}

		conn.setAutoCommit(false);

		// The user cannot book if there is insufficient capacity on either flight.
		boolean flightsHaveAvailableSeats = doFlightsHaveAvailableSeats(itinerary);
		if (!flightsHaveAvailableSeats) {
		    conn.setAutoCommit(true);
		    return "Booking failed\n";
		}

		// Add the flights in the itinerary to reservations.
		// Reservation needs to have: rid, username, price, paid, canceled, fid1, fid2
		// but already have variables for rid, username, fid1, and fid2.

		addReservation(username, itinerary); 

		// Update the booked seats on the flight(s) in the itinerary.
		incrementBookedSeats(itinerary);

		// Get the reservation id that was just created.
		int reservationId = getReservationId(username, itinerary);
		if (reservationId == -1) {
		    conn.setAutoCommit(true);
		    return "Booking failed\n";
		}

		conn.commit();
		conn.setAutoCommit(true);

		// If everything has worked then notify that the booking was successful.
		return "Booked flight(s), reservation ID: " + reservationId + "\n";
	    } catch (SQLException e) {
		e.printStackTrace();
		try {
		    if (isDeadLock(e)) {
			conn.rollback();
		    }
		    conn.setAutoCommit(true);
		} catch (SQLException e2) {
		    e2.printStackTrace();	
		}
	    } finally {
		checkDanglingTransaction();
	    }
	}
	return "Booking failed\n";
    }

    /**
     * TODO
     */
    private int getReservationsSize() throws SQLException {
	try {
	    ResultSet rs = getReservationsSizeStatement.executeQuery();
	    if (rs.next()) {
		return rs.getInt("count");
	    } else {
		return -1;
	    }
	} catch (SQLException e) {
	    throw e;
	}
    }

    /**
     * TODO
     */
    private void printReservation(int reservationId) throws SQLException {
	try {
	    getReservationStatement.clearParameters();
	    getReservationStatement.setInt(1, reservationId);
	    ResultSet rs = getReservationStatement.executeQuery();
    
	    while (rs.next()) {
		System.out.println("id:		" + rs.getInt("id"));
		System.out.println("username: 	" + rs.getString("username"));
		System.out.println("price:	" + rs.getInt("price"));
		System.out.println("paid:	" + rs.getInt("paid"));
		System.out.println("canceled: 	" + rs.getInt("canceled"));
		System.out.println("fid1:	" + rs.getInt("fid1"));
		System.out.println("fid2:	" + rs.getInt("fid2"));
	    }
	    System.out.println("\n");
	} catch (SQLException e) {
	    throw e;
	}	
    }

    /**
     * TODO
     */
    private boolean doFlightsHaveAvailableSeats(Itinerary itinerary) throws SQLException {
	try {
	    // Get the available seats on the flight1.
	    int f1Capacity = itinerary.flight1.capacity;
	    int fid1 = itinerary.flight1.fid;

	    // Query the booked seats table for how many seats are booked on flight1.
	    int f1BookedSeats = getFlightsBookedSeats(fid1);
	    int f1AvailableSeats = f1Capacity - f1BookedSeats;

	    if (f1AvailableSeats <= 0) {
		return false;
	    }

	    // If the itinerary has a second flight, check that there are available seats on flight2.
	    if (!itinerary.isDirectFlight) {
		int f2Capacity = itinerary.flight2.capacity;
		int fid2 = itinerary.flight2.fid;

		int f2BookedSeats = getFlightsBookedSeats(fid2);
		int f2AvailableSeats = f2Capacity - f2BookedSeats;

		if (f2AvailableSeats <= 0) {
		    return false;
		}
	    }

	    // Both flights have available seating.
	    return true;

	} catch (SQLException e) {
	    throw e;
	}
    }

    /**
     * TODO
     */
    private int getReservationId(String username, Itinerary itinerary) throws SQLException {
	try {
	    getReservationIdStatement.clearParameters();

	    getReservationIdStatement.setString(1, username);
	    getReservationIdStatement.setInt(2, itinerary.price);
	    getReservationIdStatement.setBoolean(3, itinerary.paid);
	    getReservationIdStatement.setBoolean(4, itinerary.canceled);
	    getReservationIdStatement.setInt(5, itinerary.flight1.fid);
	    
	    /*
	    if (itinerary.isDirectFlight) {
		getReservationIdStatement.setNull(6, Types.INTEGER); 
	    } else {
		getReservationIdStatement.setInt(6, itinerary.flight2.fid);
	    }
	    */

	    ResultSet getReservationIdResult = getReservationIdStatement.executeQuery();
	    
	    if (getReservationIdResult.next()) {
		return getReservationIdResult.getInt("id");	
	    } else {
		return -1;
	    }
	} catch (SQLException e) {
	    throw e;
	}
    }

    /**
     * TODO
     */
    private boolean incrementBookedSeats(Itinerary itinerary) throws SQLException {
	try {
	    incrementFlightsBookedSeatsStatement.clearParameters();
	    incrementFlightsBookedSeatsStatement.setInt(1, itinerary.flight1.fid);
	    incrementFlightsBookedSeatsStatement.executeUpdate();

	    if (!itinerary.isDirectFlight) {
		incrementFlightsBookedSeatsStatement.clearParameters();
		incrementFlightsBookedSeatsStatement.setInt(1, itinerary.flight2.fid);
		incrementFlightsBookedSeatsStatement.executeUpdate();
	    }

	    return true;
	} catch (SQLException e) {
	    throw e;
	}
    }


    /**
     * TODO
     */
    private boolean addReservation(String username, Itinerary itinerary) throws SQLException {
	try {
	    addReservationStatement.clearParameters();	

	    int size = getReservationsSize();	
	    if (size == -1) {
		return false;
	    }	
   
	    addReservationStatement.setInt(1, size + 1); 
	    addReservationStatement.setString(2, username);
	    addReservationStatement.setInt(3, itinerary.price);
	    addReservationStatement.setBoolean(4, itinerary.paid);
	    addReservationStatement.setBoolean(5, itinerary.canceled);
	    addReservationStatement.setInt(6, itinerary.flight1.fid);
	
	    if (itinerary.isDirectFlight) {
		addReservationStatement.setNull(7, Types.INTEGER);
	    } else {
		addReservationStatement.setInt(7, itinerary.flight2.fid);
	    }
	    
	    addReservationStatement.executeUpdate();
	
	    return true;
	} catch (SQLException e) {
	    throw e;
	}
    }


    /**
     * TODO
     */
    private int getFlightsBookedSeats(int fid) throws SQLException {
	try {
	    getFlightsBookedSeatsStatement.clearParameters();
	    getFlightsBookedSeatsStatement.setInt(1, fid);
	    ResultSet flight1BookedSeatsResult = getFlightsBookedSeatsStatement.executeQuery();
	    if (flight1BookedSeatsResult.next()) {
		return flight1BookedSeatsResult.getInt("seats");
	    } else {
		return -1;
	    }
	} catch (SQLException e) {
	    throw e;
	}
    }

    /**
     * TODO
     */
    private boolean doesUserHaveReservationOnDay(String username, int itineraryDay) {
	// Query the reservations table for any flights by the user on itineraryDay.
	try {
	    getUserReservationsOnDayStatement.clearParameters();

	    getUserReservationsOnDayStatement.setString(1, this.user);
	    getUserReservationsOnDayStatement.setInt(2, itineraryDay);

	    ResultSet userReservationsResult = getUserReservationsOnDayStatement.executeQuery();
	    //int count = -1;
	    // Let count be the number of reservations the user for the day.
	    if (userReservationsResult.next()) {
		//count = userReservationsResult.getInt("count");
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
	for (int i = 0; i < 3; i++) {
	    try {
		// A user must be logged in to pay.
		if (this.user == null) {
		    return "Cannot pay, not logged in\n";
		}

		String username = this.user;

		// The current user must have booked the reservationId.
		if (!userHasUnpaidReservation(reservationId, username)) {
		    return "Cannot find unpaid reservation " + reservationId + " under user: " + username + "\n";
		}  

		//int reservationPrice = getReservationAttribute(reservationId, "price");
		int reservationPrice = getReservationPrice(reservationId);
		int userBalance = getUserBalance(username);
		if (reservationPrice == -1 || userBalance == -1) {
		    return "Failed to pay for reservation " + reservationId + "\n";
		}

		// The current user must have enough funds in their account to pay for the reservation.
		int newUserBalance = userBalance - reservationPrice;
		if (newUserBalance < 0) {
		    return "User has only " + userBalance + " in account but itinerary costs " + reservationPrice + "\n";
		}

		conn.setAutoCommit(false);

		// Pay for the reservation.

		// Remove the cost of the reservation from the users account.
		boolean updateBalanceWasSuccessful = updateUserBalance(this.user, newUserBalance);

		// Update the reservation paid attribute to true.
		boolean updatePaidWasSuccessful = updateReservationPaid(reservationId, 1);
		if (!updatePaidWasSuccessful) {
		    conn.setAutoCommit(true); // TODO possible remove;
		    return "Failed to pay for reservation " + reservationId + "\n";
		}  

		conn.commit();
		conn.setAutoCommit(true);

		// The payment was succesful, notify.
		return "Paid reservation: " + reservationId + " remaining balance: " + newUserBalance + "\n";
	    } catch (SQLException e) {
		e.printStackTrace();
		try {
		    if (isDeadLock(e)) {
			conn.rollback();
		    }
		    conn.setAutoCommit(true);
		} catch (SQLException e2) {
		    return "Failed to pay for reservation " + reservationId + "\n";
		}
	    } finally {
		checkDanglingTransaction();
	    }
	}
	return "Failed to pay for reservation " + reservationId + "\n";
    }

    /**
     * Returns a boolean of whether the currently logged in user booked the given reservationid.
     * Assumes that a user is currently logged in.
     *
     * @param reservationId the reservationId to check against.
     * 
     * @return true if the current user booked the given reservationId and false otherwise.
     */
    private boolean userHasUnpaidReservation(int reservationId, String username) throws SQLException {
	try {
	    userHasUnpaidReservationStatement.clearParameters();
	    userHasUnpaidReservationStatement.setInt(1, reservationId);
	    userHasUnpaidReservationStatement.setString(2, username);
	    ResultSet rs = userHasUnpaidReservationStatement.executeQuery();
    
	    if (rs.next()) {
		return true;
	    } else {
		return false;
	    }
	} catch (SQLException e) {
	    throw e;
	} 
    }
   
    /**
     * Queries the Reservations table for the price of the given reservationId.
     * 
     * @param reservationId the reservationId to search for.
     * 
     * @return an integer representing the cost of the reservation.
     */
    private int getReservationPrice(int reservationId) throws SQLException {
	try {
	    getReservationPriceStatement.setInt(1, reservationId);
	    ResultSet getReservationPriceResult = getReservationPriceStatement.executeQuery();

	    if (getReservationPriceResult.next()) {
		return getReservationPriceResult.getInt("price");
	    } else {
		return -1;
	    }
	} catch (SQLException e) {
	    throw e;
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
    private int getUserBalance(String username) throws SQLException {
	try {
	    getUserBalanceStatement.setString(1, username);
	    ResultSet getUserBalanceResult = getUserBalanceStatement.executeQuery();
	    if (getUserBalanceResult.next()) {
		return getUserBalanceResult.getInt("balance");
	    } else {
		return -1;
	    }
	} catch (SQLException e) {
	    throw e;
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
    /*
    private boolean updateUserAttribute(String username, String attribute, int value) throws SQLException {
	try {
	    updateUserAttributeStatement.clearParameters();
	    updateUserAttributeStatement.setString(3, username);
	    updateUserAttributeStatement.setString(1, attribute);
	    updateUserAttributeStatement.setInt(2, value);
	    updateUserAttributeStatement.executeUpdate();
	    
	    return true;
	} catch (SQLException e) {
	    throw e;
	}
    }
    */

    /**
     * Updates the user's account balance to the given amount.
     * 
     * @param username the username to change the account balance of.
     * @param amount the new amount to set the users account balance to.
     * 
     * @return true if the update was successful.
     */
    private boolean updateUserBalance(String username, int amount) throws SQLException {
	try {
	    updateUserBalanceStatement.clearParameters();
	    updateUserBalanceStatement.setInt(1, amount);
	    updateUserBalanceStatement.setString(2, username);
	    updateUserBalanceStatement.executeUpdate();
	    
	    return true;
	} catch (SQLException e) {
	    throw e;
	}
    }

    /**
     * Update the reservationId's attribute to the value.
     * 
     * @param reservationId the id to update. 
     * @param attribute the attribute to update.
     * @param value the value to update.
     * 
     * @return true if the update was successful and false otherwise.
     */
    private boolean updateReservationPaid(int reservationId, int value) throws SQLException {
	try {
	    updateReservationPaidStatement.clearParameters();
	    updateReservationPaidStatement.setInt(1, value);
	    updateReservationPaidStatement.setInt(2, reservationId);
	    updateReservationPaidStatement.executeUpdate();
	    return true;
	} catch (SQLException e) {
	    throw e;
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
	for (int i = 0; i < 3; i++) {
	    try {
		// A user must be logged in to view reservations.
		if (this.user == null) {
		    return "Cannot view reservations, not logged in\n";
		}

		String username = this.user;

		List<Itinerary> reservations = getOpenReservations(username);
		if (reservations.size() == 0) {
		    return "No reservations found\n";
		}

		conn.setAutoCommit(false);

		StringBuffer sb = reservationStringBuffer(reservations);

		if (sb.length() == 0) {
		    //conn.setAutoCommit(true);
		    return "No flights match your selection\n";
		} else {
		    conn.commit();
		    conn.setAutoCommit(true);
		    return sb.toString();
		}
	    } catch (SQLException e) {
		e.printStackTrace();
		try {
		    if (isDeadLock(e)) {
			conn.rollback();
		    }
		    conn.setAutoCommit(true);
		} catch (SQLException e2) {
		    //e2.printStackTrace();
		    return "Failed to retrieve reservations\n";
		}
	    } finally {
		checkDanglingTransaction();
	    }
	}
	return "Failed to retrieve reservations\n";
    }

    /**
     * TODO
     */
    private StringBuffer reservationStringBuffer(List<Itinerary> reservations) {
	StringBuffer sb = new StringBuffer();	

	// Fill the string builder with the ordered itineraries.
	int n = itineraries.size();
	for (int i = 0; i < n; i++) {
	    sb.append("Reservation " + Integer.toString(i + 1));

	    Itinerary reservation = reservations.get(i);

	    String paid = (reservation.paid == true) ? "true" : "false";
	    sb.append(" paid: " + paid + ":\n");

	    if (reservation.isDirectFlight) {
		sb.append(reservation.flight1.toString() + "\n"); 
	    } else {
		sb.append(reservation.flight1.toString() + "\n" + reservation.flight2.toString() + "\n");
	    }
	}
	return sb;
    }

    /*
     * TODO
     */
    /*
    private boolean doesUserHaveReservation(String username) throws SQLException {
	try {
	    getOpenReservations.clearParameters();
	    getOpenReservations.setString(1, username);
	    getOpenReservations.setInt(2, 0);

	    ResultSet getUserReservationsResult = getOpenReservations.executeQuery();
	    
	    if (getUserReservationsResult.next()) {
		return true;
	    } else {
		return false;
	    }
	} catch (SQLException e) {
	    throw e;
	}
    }
    */

    /**
     * TODO
     */
    private List<Itinerary> getOpenReservations(String username) throws SQLException {
	try {
	    getOpenReservations.clearParameters();
	    getOpenReservations.setString(1, username.toLowerCase());
	    ResultSet rs = getOpenReservations.executeQuery();	

	    List<Itinerary> itineraries = new ArrayList<>();

	    while (rs.next()) {
		int id = rs.getInt("id");
		
		int price = rs.getInt("price");
		int paid = rs.getInt("paid");
		int fid1 = rs.getInt("fid1");
		int fid2 = rs.getInt("fid2");	
	
		Flight f1 = getFlight(fid1);
		if (fid2 != 0) {
		    Flight f2 = getFlight(fid2);

		    Itinerary itinerary = new Itinerary(f1, f2, f1.time + f2.time, false);
		    
		    itineraries.add(itinerary);
		} else {
		    Itinerary itinerary = new Itinerary(f1, null, f1.time, true);
		    itineraries.add(itinerary);
		}
	    }

	    return itineraries;
	} catch (SQLException e) {
	    throw e;
	}
    }
    
    /**
     * TODO
     */
    private Flight getFlight(int fid) throws SQLException {
	try {
	    getFlightStatement.clearParameters();
	    getFlightStatement.setInt(1, fid);
	    ResultSet rs = getFlightStatement.executeQuery();
	    
	    if (rs.next()) {
		int dayOfMonth = rs.getInt("day_of_month");
		String carrierId = rs.getString("carrier_id");
		String flightNum = rs.getString("flight_num");
		String originCity = rs.getString("origin_city");
		String destCity = rs.getString("dest_city");
		int time = rs.getInt("actual_time");
		int capacity = rs.getInt("capacity");
		int price = rs.getInt("price");
		return new Flight(fid, dayOfMonth, carrierId, flightNum, originCity, destCity, time, capacity, price);	
	    } else {
		return null;
	    }
	} catch (SQLException e) {
	    throw e;
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
	for (int i = 0; i < 3; i++) {
	    try {
		if (this.user == null) {
		    return "Cannot cancel reservations, not logged in\n";
		}

		if (reservationMatchesUser(reservationId, this.user)) {
		    conn.setAutoCommit(false);
	
		    int price = getReservationPrice(reservationId);
		    int userBalance = getUserBalance(this.user);
		    int newUserBalance = userBalance + price;
		    updateUserBalance(this.user, newUserBalance);
		    setReservationCancellation(reservationId, true);
	    
		    conn.commit();
		    conn.setAutoCommit(true);
		
		    return "Canceled reservation " + reservationId + "\n";
		}
	    } catch (SQLException e) {
		e.printStackTrace();
		try {
		    if (isDeadLock(e)) {
			conn.rollback();
		    }
		    conn.setAutoCommit(true);
		} catch (SQLException e2) {
		    return "Failed to cancel reservation " + reservationId + "\n";
		}
	    } finally {
		checkDanglingTransaction();
	    }
	}
	return "Failed to cancel reservation " + reservationId + "\n";
    }

    
    /**
     * TODO 
     */
    private void setReservationCancellation(int reservationId, boolean state) throws SQLException {
	setReservationCancellationStatement.clearParameters();
	setReservationCancellationStatement.setBoolean(1, state);
	setReservationCancellationStatement.setInt(2, reservationId);
	setReservationCancellationStatement.executeUpdate();
    }

 
    /**
     * TODO
     */
    private boolean reservationMatchesUser(int reservationId, String username) throws SQLException {
	reservationMatchesUserStatement.clearParameters();
	reservationMatchesUserStatement.setInt(1, reservationId);
	reservationMatchesUserStatement.setString(2, username);
	ResultSet rs = reservationMatchesUserStatement.executeQuery();

	boolean result;
	if (rs.next()) {
	    result = true;
	} else {
	    result = false;
	}

	rs.close();
	return result;
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
	public boolean canceled;

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
	    this.canceled = false;
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
