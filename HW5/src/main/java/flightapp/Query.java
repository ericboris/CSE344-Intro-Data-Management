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
    private PreparedStatement checkFlightCapacityStatement;
    private static final String CHECK_FLIGHT_CAPACITY = ""
	+ "SELECT capacity"
	+ "  FROM Flights"
	+ " WHERE fid = ?";

    // For check dangling
    private PreparedStatement tranCountStatement;
    private static final String TRANCOUNT_SQL = ""
	+ "SELECT @@TRANCOUNT AS tran_count";

    // Used to clear the Users table.
    private PreparedStatement clearUsersStatement;
    private static final String CLEAR_USERS = ""
	+ "DELETE FROM Users;";

    // Used to clear the Reservations table.
    private PreparedStatement clearReservationsStatement;
    private static final String CLEAR_RESERVATIONS = ""
	+ "DELETE FROM Reservations;";

    // Used to clear the BookedSeats table.
    private PreparedStatement clearBookedSeatsStatement;
    private static final String CLEAR_BOOKED_SEATS = ""
	+ "DELETE FROM BookedSeats;";

    // Used to check if username already exists
    private PreparedStatement isUsernameTakenStatement;
    private static final String CHECK_USERNAME_EXISTS = ""
	+ "SELECT COUNT(*) as count"
	+ "  FROM Users"
	+ " WHERE username = ?;";

    // Used to add a new user to the Users table.
    private PreparedStatement addUserStatement;
    private static final String INSERT_USER_DATA = ""
	+ "INSERT INTO Users VALUES (?, ?, ?, ?);";

    // Used to get the top n direct flights from src to dst on a given day in July 2015. 
    private PreparedStatement directFlightStatement;
    private static final String DIRECT_FLIGHT = ""
	+ "SELECT top(?)"
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

    // Used to get the top one-hop flights from src to dst on a given day in July 2015.
    private PreparedStatement oneHopFlightStatement;
    private static final String ONE_HOP_FLIGHT = ""
	+ "SELECT TOP(?)"
	+ "       f1.fid AS f1_fid,"  
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

    // Add a new Reservation to the Reservations table.
    private PreparedStatement addReservationStatement;
    private static final String ADD_RESERVATION = ""
	+ "INSERT INTO Reservations"
	+ " VALUES (?, ?, ?, ?, ?, ?, ?);";  

    // Add a new BookedSeat entry.
    private PreparedStatement addBookedSeatStatement;
    private static final String ADD_BOOKED_SEAT = ""
	+ "INSERT INTO BookedSeats"
	+ " VALUES (?, ?);";

    // Get Reservation ID from the Reservations table.
    private PreparedStatement getReservationIdStatement;
    private static final String GET_RESERVATION_ID = ""
	+ "SELECT id AS id"
	+ "  FROM Reservations"
	+ " WHERE username = ?"
	+ "   AND price = ?"
	+ "   AND paid = ?"
	+ "   AND canceled = ?"
	+ "   AND fid1 = ?;";

    // Get a user's reservations on a given day 
    private PreparedStatement doesUserHaveReservationStatement;
    private static final String GET_USER_OPEN_RESERVATIONS_ON_DAY = ""
	+ "SELECT COUNT(*) AS count"
	+ "  FROM Flights AS f,"
	+ "       (SELECT fid1"
	+ "          FROM Reservations"
	+ "         WHERE username = ?) AS r"
	+ " WHERE r.fid1 = f.fid"
	+ "   AND f.day_of_month = ?;"; 

    // Get the number of booked seats on a given flight.
    private PreparedStatement getBookedSeatsStatement;
    private static final String GET_FLIGHTS_BOOKED_SEATS = ""
	+ "SELECT seats"
	+ "  FROM BookedSeats"
	+ " WHERE fid = ?;";

    // Increment the number of booked seats on a given flight.
    private PreparedStatement incrementBookedSeatsStatement;
    private static final String INCREMENT_FLIGHTS_BOOKED_SEATS = ""
	+ "UPDATE BookedSeats"
	+ "   SET seats = seats + 1"
	+ " WHERE fid = ?;";

    // Return whether the given username booked the given reservation.
    private PreparedStatement userHasUnpaidReservationStatement;
    private static final String DID_USER_BOOK_RESERVATION = ""
	+ "SELECT COUNT(*) AS count"
	+ "  FROM Reservations"
	+ " WHERE id = ?"
	+ "   AND username = ?" 
	+ "   AND paid = 0"
	+ "   AND canceled = 0;";

    // Update the attribute for the given reservationId with the given value.
    private PreparedStatement setReservationPaidStatement;
    private static final String UPDATE_RESERVATION_PAID = ""
	+ "UPDATE Reservations"
	+ "   SET paid = ?"
	+ " WHERE id = ?"; 

    // Return the attribute for the given username.
    private PreparedStatement getUserBalanceStatement;
    private static final String GET_USER_BALANCE = ""
	+ "SELECT balance"
	+ "  FROM Users"
	+ " WHERE username = ?;";

    // Update the attribute for the given username with the given value.
    private PreparedStatement setUserBalanceStatement;
    private static final String UPDATE_USER_BALANCE = ""
	+ "UPDATE Users"
	+ "   SET balance = ?"
	+ " WHERE username = ?"; 

    // Get reservation cost. 
    private PreparedStatement getReservationPriceStatement;
    private static final String GET_RESERVATION_PRICE = ""
	+ "SELECT price"
	+ "  FROM Reservations"
	+ " WHERE id = ?;";

    // Get the given user's reservations.
    private PreparedStatement getOpenReservationsStatement;
    private static final String GET_USER_OPEN_RESERVATIONS = ""
	+ "SELECT id, price, paid, canceled, fid1, fid2"
	+ "  FROM Reservations"
	+ " WHERE username = ?"
	+ "   AND canceled = 0;";

    // Get the given user's salt.
    private PreparedStatement getUserSaltStatement;
    private static final String GET_USER_SALT = ""
	+ "SELECT salt"
	+ "  FROM Users"
	+ " WHERE username = ?;";

    // Get the given user's hash.
    private PreparedStatement getUserHashStatement;
    private static final String GET_USER_HASH = ""
	+ "SELECT hash"
	+ "  FROM Users"
	+ " WHERE username = ?;";

    // Get the size of the reservations table.
    private PreparedStatement getReservationsSizeStatement;
    private static final String GET_RESERVATIONS_SIZE = ""
	+ "SELECT count(*) AS count"
	+ "  FROM reservations;";

    // Get a flight's information via fid.
    private PreparedStatement getFlightStatement;
    private static final String GET_FLIGHT = ""
	+ "SELECT day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price"
	+ "  FROM Flights"
	+ " WHERE fid = ?;";

    // Change a reservation's ancellation attribute status.
    private PreparedStatement setReservationCanceledStatement;
    private static final String SET_RESERVATION_CANCELLATION = ""
	+ "UPDATE Reservations"
	+ "   SET canceled = ?"
	+ " WHERE id = ?;";

    // Returns results there's a user in the Reservations table with the given reservation Id. 
    private PreparedStatement reservationMatchesUserStatement;
    private static final String RESERVATION_MATCHES_USER = ""
	+ "SELECT COUNT(*) AS count"
	+ "  FROM Reservations"
	+ " WHERE id = ?"
	+ "   AND username = ?"
	+ "   AND canceled = 0;";
    
    // Return the reservationId's canceled attribute.
    private PreparedStatement getReservationCanceledStatement;
    private static final String GET_RESERVATION_CANCELED = ""
	+ "SELECT canceled"
	+ "  FROM Reservations"
	+ " WHERE id = ?;";

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
		: openConnectionFromCredential(serverURL, dbName, adminName, password); prepareStatements(); } 
    /**

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
	// Flights statements
	checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);
	directFlightStatement = conn.prepareStatement(DIRECT_FLIGHT);
	oneHopFlightStatement = conn.prepareStatement(ONE_HOP_FLIGHT);
	getFlightStatement = conn.prepareStatement(GET_FLIGHT);

	// Users Statements
	clearUsersStatement = conn.prepareStatement(CLEAR_USERS);
	isUsernameTakenStatement = conn.prepareStatement(CHECK_USERNAME_EXISTS);
	addUserStatement = conn.prepareStatement(INSERT_USER_DATA);
	getUserBalanceStatement = conn.prepareStatement(GET_USER_BALANCE);
	setUserBalanceStatement = conn.prepareStatement(UPDATE_USER_BALANCE);
	getUserSaltStatement = conn.prepareStatement(GET_USER_SALT);
	getUserHashStatement = conn.prepareStatement(GET_USER_HASH);

	// Reservations Statements
	clearReservationsStatement = conn.prepareStatement(CLEAR_RESERVATIONS);
	addReservationStatement = conn.prepareStatement(ADD_RESERVATION);
	getReservationIdStatement = conn.prepareStatement(GET_RESERVATION_ID);
	userHasUnpaidReservationStatement = conn.prepareStatement(DID_USER_BOOK_RESERVATION);
	setReservationPaidStatement = conn.prepareStatement(UPDATE_RESERVATION_PAID);
	getReservationPriceStatement = conn.prepareStatement(GET_RESERVATION_PRICE);
	getOpenReservationsStatement = conn.prepareStatement(GET_USER_OPEN_RESERVATIONS);
	getReservationsSizeStatement = conn.prepareStatement(GET_RESERVATIONS_SIZE);
	setReservationCanceledStatement = conn.prepareStatement(SET_RESERVATION_CANCELLATION);
	reservationMatchesUserStatement = conn.prepareStatement(RESERVATION_MATCHES_USER);
	getReservationCanceledStatement = conn.prepareStatement(GET_RESERVATION_CANCELED);

	// BookedSeats Statements
	clearBookedSeatsStatement = conn.prepareStatement(CLEAR_BOOKED_SEATS);
	getBookedSeatsStatement = conn.prepareStatement(GET_FLIGHTS_BOOKED_SEATS);
	incrementBookedSeatsStatement = conn.prepareStatement(INCREMENT_FLIGHTS_BOOKED_SEATS);
	addBookedSeatStatement = conn.prepareStatement(ADD_BOOKED_SEAT);

	// Misc.	
	tranCountStatement = conn.prepareStatement(TRANCOUNT_SQL);
	doesUserHaveReservationStatement = conn.prepareStatement(GET_USER_OPEN_RESERVATIONS_ON_DAY);
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
		return "User already logged in\n";
	    }

	    // Check whether the given username exists.
	    boolean usernameExists = isUsernameTaken(username);
	    if (!usernameExists) {
		return "Login failed\n";
	    }

	    // Check whether the given username and password combination are valid.

	    // Get the salt and hash of the stored username.
	    byte[] storedSalt = getUserSalt(username);
	    byte[] storedHash = getUserHash(username);
	    if (storedSalt == null || storedHash == null) {
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

	    return "Login failed\n";

	} catch (SQLException e) {
	    e.printStackTrace();
	    return "Login failed\n";
	} finally {
	    checkDanglingTransaction();
	}
    }

    /**
     * Return the user's salt. 
     */
    public byte[] getUserSalt(String username) throws SQLException {
	getUserSaltStatement.clearParameters();
	getUserSaltStatement.setString(1, username.toLowerCase());
	ResultSet rs = getUserSaltStatement.executeQuery();
	rs.next();
	byte[] result = rs.getBytes("salt");
	rs.close();
	return result;
    }

    /**
     * Return the user's hash. 
     */
    public byte[] getUserHash(String username) throws SQLException {
	getUserHashStatement.clearParameters();
	getUserHashStatement.setString(1, username.toLowerCase());
	ResultSet rs = getUserHashStatement.executeQuery();
	rs.next();
	byte[] result = rs.getBytes("hash");
	rs.close();
	return result; 
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
     * Return true if the given username is already in the Users table and false otherwise. 
     */
    private boolean isUsernameTaken(String username) throws SQLException {
	isUsernameTakenStatement.clearParameters();
	isUsernameTakenStatement.setString(1, username.toLowerCase());
	ResultSet rs = isUsernameTakenStatement.executeQuery();
	rs.next();
	boolean result = rs.getInt("count") == 1;
	rs.close();
	return result;
    }

    /**
     * Add a new user to the Users table. 
     */
    private void addUser(String username, byte[] salt, byte[] hash, int amount) throws SQLException {
	addUserStatement.clearParameters();
	addUserStatement.setString(1, username.toLowerCase());
	addUserStatement.setBytes(2, salt);
	addUserStatement.setBytes(3, hash);
	addUserStatement.setInt(4, amount);
	addUserStatement.executeUpdate();
    }

    /**
     * Generate a random cryptographic salt. 
     */
    private byte[] getSalt() {
	SecureRandom random = new SecureRandom();
	byte[] salt = new byte[16];
	random.nextBytes(salt);
	return salt;
    }

    /**
     * Generate a hashed password. 
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
     * Return a string buffer from the given itinerary. 
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
     * Return a list of direct flights up to the given maximum number. 
     */
    private List<Itinerary> getDirectFlights(List<Itinerary> itineraries, int max, String origin, String destination, int day) throws SQLException {
	directFlightStatement.clearParameters();
	directFlightStatement.setInt(1, max);
	directFlightStatement.setString(2, origin);
	directFlightStatement.setString(3, destination);
	directFlightStatement.setInt(4, day);
	ResultSet rs = directFlightStatement.executeQuery();

	while (rs.next()) {	
	    int f1fid = rs.getInt("fid");
	    int f1dayOfMonth = rs.getInt("day_of_month");
	    String f1carrierId = rs.getString("carrier_id");
	    String f1flightNum = rs.getString("flight_num");
	    String f1originCity = rs.getString("origin_city");
	    String f1destCity = rs.getString("dest_city");
	    int f1time = rs.getInt("actual_time");
	    int f1capacity = rs.getInt("capacity");
	    int f1price = rs.getInt("price");

	    Flight flight1 = new Flight(f1fid, f1dayOfMonth, f1carrierId, f1flightNum, f1originCity, f1destCity, f1time, f1capacity, f1price);
	    Itinerary itinerary = new Itinerary(flight1, null, f1time, true);
	    itineraries.add(itinerary);
	}

	rs.close();
	return itineraries;
    }

    /**
     * Return a list of indirect flights up to the given maximum number.
     */
    private List<Itinerary> getIndirectFlights(List<Itinerary> itineraries, int max, String origin, String destination, int day) throws SQLException {
	oneHopFlightStatement.clearParameters();
	oneHopFlightStatement.setInt(1, max);
	oneHopFlightStatement.setString(2, origin);
	oneHopFlightStatement.setString(3, destination);
	oneHopFlightStatement.setInt(4, day);
	ResultSet rs = oneHopFlightStatement.executeQuery();

	while (rs.next()) {
	    // Flight 1 info.
	    int f1fid = rs.getInt("f1_fid");
	    int f1dayOfMonth = rs.getInt("f1_day_of_month");
	    String f1carrierId = rs.getString("f1_carrier_id");
	    String f1flightNum = rs.getString("f1_flight_num");
	    String f1originCity = rs.getString("f1_origin_city");
	    String f1destCity = rs.getString("f1_dest_city");
	    int f1time = rs.getInt("f1_actual_time");
	    int f1capacity = rs.getInt("f1_capacity");
	    int f1price = rs.getInt("f1_price");

	    // Flight 2 info.
	    int f2fid = rs.getInt("f2_fid");
	    int f2dayOfMonth = rs.getInt("f2_day_of_month");
	    String f2carrierId = rs.getString("f2_carrier_id");
	    String f2flightNum = rs.getString("f2_flight_num");
	    String f2originCity = rs.getString("f2_origin_city");
	    String f2destCity = rs.getString("f2_dest_city");
	    int f2time = rs.getInt("f2_actual_time");
	    int f2capacity = rs.getInt("f2_capacity");
	    int f2price = rs.getInt("f2_price");

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

	rs.close();
	return itineraries;
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
		if (this.user == null) {
		    return "Cannot book reservations, not logged in\n";
		}

		if (this.itineraries.size() == 0 || this.itineraries.size() <= itineraryId) {
		    return "No such itinerary " + itineraryId + "\n";
		}

		Itinerary itinerary = itineraries.get(itineraryId);

		boolean userAlreadyHasReservation = doesUserHaveReservation(this.user, itinerary.flight1.dayOfMonth);
		if (userAlreadyHasReservation) {
		    return "You cannot book two flights in the same day\n";
		}

		conn.setAutoCommit(false);

		boolean flightsHaveAvailableSeats = doesItineraryHaveSeats(itinerary);
		if (!flightsHaveAvailableSeats) {
		    conn.setAutoCommit(true);
		    return "Booking failed\n";
		}

		addReservation(this.user, itinerary); 
		incrementBookedSeats(itinerary);
		int reservationId = getReservationId(this.user, itinerary);

		conn.commit();
		conn.setAutoCommit(true);

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
     * Return the number of reservations in the Reservations table. 
     */
    private int getReservationsSize() throws SQLException {
	ResultSet rs = getReservationsSizeStatement.executeQuery();
	rs.next();
	int result = rs.getInt("count");
	rs.close();
	return result;
    }

    /**
     * Return true if the flights in the given itinerary have available seats and false otherwise. 
     */
    private boolean doesItineraryHaveSeats(Itinerary itinerary) throws SQLException {
	int f1Capacity = itinerary.flight1.capacity;
	int fid1 = itinerary.flight1.fid;
	int f1BookedSeats = getBookedSeats(fid1);
	int f1AvailableSeats = f1Capacity - f1BookedSeats;

	if (f1AvailableSeats <= 0) {
	    return false;
	}

	// If the itinerary has a second flight, check that there are available seats on flight2.
	if (!itinerary.isDirectFlight) {
	    int f2Capacity = itinerary.flight2.capacity;
	    int fid2 = itinerary.flight2.fid;
	    int f2BookedSeats = getBookedSeats(fid2);
	    int f2AvailableSeats = f2Capacity - f2BookedSeats;

	    if (f2AvailableSeats <= 0) {
		return false;
	    }
	}

	return true;
    }

    /**
     * Return the reservationId given the reservation's booking information. 
     */
    private int getReservationId(String username, Itinerary itinerary) throws SQLException {
	int paid = (itinerary.paid) ? 1 : 0;
	int canceled = (itinerary.canceled) ? 1 : 0;
    
	getReservationIdStatement.clearParameters();
	getReservationIdStatement.setString(1, username);
	getReservationIdStatement.setInt(2, itinerary.price);
	getReservationIdStatement.setInt(3, paid);
	getReservationIdStatement.setInt(4, canceled);
	getReservationIdStatement.setInt(5, itinerary.flight1.fid);
	ResultSet rs = getReservationIdStatement.executeQuery();
	rs.next();
	int result = rs.getInt("id");
	rs.close();
	return result;  
    }

    /**
     * Add a new BookedSeat flight entry.
     */
    private void addBookedSeat(int fid) throws SQLException {
	addBookedSeatStatement.clearParameters();
	addBookedSeatStatement.setInt(1, fid);
	addBookedSeatStatement.setInt(2, 0);
	addBookedSeatStatement.executeUpdate();
    }

    /**
     * Increment the number of booked seats on the itinerary's given flights by one. 
     */
    private void incrementBookedSeats(Itinerary itinerary) throws SQLException {
	int fid1 = itinerary.flight1.fid;	
	if (getBookedSeats(fid1) == 0) {
	    addBookedSeat(fid1);
	}
	incrementBookedSeatsStatement.clearParameters();
	incrementBookedSeatsStatement.setInt(1, fid1);
	incrementBookedSeatsStatement.executeUpdate();

	if (!itinerary.isDirectFlight) {
	    int fid2 = itinerary.flight2.fid;
	    if (getBookedSeats(fid2) == 0) {
		addBookedSeat(fid2);
	    }
	    incrementBookedSeatsStatement.clearParameters();
	    incrementBookedSeatsStatement.setInt(1, fid2);
	    incrementBookedSeatsStatement.executeUpdate();
	}
    }

    /**
     * Add a new reservation to the Reservations table. 
     */
    private void addReservation(String username, Itinerary itinerary) throws SQLException {
	addReservationStatement.clearParameters();	

	int size = getReservationsSize();	
	int paid = (itinerary.paid) ? 1 : 0;
	int canceled = (itinerary.canceled) ? 1 : 0;

	addReservationStatement.setInt(1, size + 1); 
	addReservationStatement.setString(2, username);
	addReservationStatement.setInt(3, itinerary.price);
	addReservationStatement.setInt(4, paid);
	addReservationStatement.setInt(5, canceled);
	addReservationStatement.setInt(6, itinerary.flight1.fid);

	// Handle the case where there's no flight2.
	if (itinerary.isDirectFlight) {
	    addReservationStatement.setNull(7, Types.INTEGER);
	} else {
	    addReservationStatement.setInt(7, itinerary.flight2.fid);
	}

	addReservationStatement.executeUpdate();
    }

    /**
     * Get the number of booked seats on the given flight. 
     */
    private int getBookedSeats(int fid) throws SQLException {
	getBookedSeatsStatement.clearParameters();
	getBookedSeatsStatement.setInt(1, fid);
	ResultSet rs = getBookedSeatsStatement.executeQuery();
	int result;
	if(rs.next()) {
	    result = rs.getInt("seats");
	} else {
	    result = 0;
	}
	rs.close();
	return result; 
    }

    /**
     * Return true if the given user has a flight booked on the given day and false otherwise. 
     */
    private boolean doesUserHaveReservation(String username, int itineraryDay) throws SQLException {
	doesUserHaveReservationStatement.clearParameters();
	doesUserHaveReservationStatement.setString(1, this.user);
	doesUserHaveReservationStatement.setInt(2, itineraryDay);
	ResultSet rs = doesUserHaveReservationStatement.executeQuery();
	rs.next();
	boolean result = rs.getInt("count") == 1;
	rs.close();
	return result;
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
		if (this.user == null) {
		    return "Cannot pay, not logged in\n";
		}

		// The current user must be the one that booked this reservation.
		if (!userHasUnpaidReservation(reservationId, this.user)) {
		    return "Cannot find unpaid reservation " + reservationId + " under user: " + this.user + "\n";
		}  

		int reservationPrice = getReservationPrice(reservationId);
		int userBalance = getUserBalance(this.user);
		int newUserBalance = userBalance - reservationPrice;

		if (newUserBalance < 0) {
		    return "User has only " + userBalance + " in account but itinerary costs " + reservationPrice + "\n";
		}

		conn.setAutoCommit(false);


		setUserBalance(this.user, newUserBalance);
		setReservationPaid(reservationId, 1);

		conn.commit();
		conn.setAutoCommit(true);

		return "Paid reservation: " + reservationId + " remaining balance: " + newUserBalance + "\n";
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
	return "Failed to pay for reservation " + reservationId + "\n";
    }

    /**
     * Return true if the user hasn't paid the reservation and false otherwise.
     */
    private boolean userHasUnpaidReservation(int reservationId, String username) throws SQLException {
	userHasUnpaidReservationStatement.clearParameters();
	userHasUnpaidReservationStatement.setInt(1, reservationId);
	userHasUnpaidReservationStatement.setString(2, username);
	ResultSet rs = userHasUnpaidReservationStatement.executeQuery();	
	rs.next();
	boolean result = rs.getInt("count") == 1;
	rs.close();
	return result;
    }

    /**
     * Return the reservation's price.
     */
    private int getReservationPrice(int reservationId) throws SQLException {
	getReservationPriceStatement.setInt(1, reservationId);
	ResultSet rs = getReservationPriceStatement.executeQuery();
	rs.next();
	int result = rs.getInt("price");
	rs.close();
	return result;
    } 

    /**
     * Return the user's account balance.
     */
    private int getUserBalance(String username) throws SQLException {
	getUserBalanceStatement.setString(1, username);
	ResultSet rs = getUserBalanceStatement.executeQuery();
	rs.next();
	int result = rs.getInt("balance");
	rs.close();
	return result;
    }

    /**
     * Set the user's account balance to the given amount.
     */
    private void setUserBalance(String username, int amount) throws SQLException {
	setUserBalanceStatement.clearParameters();
	setUserBalanceStatement.setInt(1, amount);
	setUserBalanceStatement.setString(2, username);
	setUserBalanceStatement.executeUpdate();
    }

    /**
     * Set the reservation's paid attribute to the given state:
     * true if 1 and false if 0.
     */
    private void setReservationPaid(int reservationId, int state) throws SQLException {
	setReservationPaidStatement.clearParameters();
	setReservationPaidStatement.setInt(1, state);
	setReservationPaidStatement.setInt(2, reservationId);
	setReservationPaidStatement.executeUpdate();
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
		if (this.user == null) {
		    return "Cannot view reservations, not logged in\n";
		}

		List<Itinerary> reservations = getOpenReservations(this.user);
		if (reservations.size() == 0) {
		    return "No reservations found\n";
		}

		conn.setAutoCommit(false);

		StringBuffer sb = reservationStringBuffer(reservations);

		if (sb.length() == 0) {
		    conn.setAutoCommit(true);
		    return "No flights match your selection\n";
		}		    

		conn.commit();
		conn.setAutoCommit(true);

		return sb.toString();
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
	return "Failed to retrieve reservations\n";
    }

    /**
     * Return a string buffer of the given reservations.
     */
    private StringBuffer reservationStringBuffer(List<Itinerary> reservations) {
	StringBuffer sb = new StringBuffer();	

	int n = reservations.size();
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

    /**
     * Return a list of the open reservations held by the user. 
     */
    private List<Itinerary> getOpenReservations(String username) throws SQLException {
	getOpenReservationsStatement.clearParameters();
	getOpenReservationsStatement.setString(1, username.toLowerCase());
	ResultSet rs = getOpenReservationsStatement.executeQuery();	

	List<Itinerary> itineraries = new ArrayList<>();
	while (rs.next()) {
	    int id = rs.getInt("id");
	    int price = rs.getInt("price");
	    int paid = rs.getInt("paid");
	    int fid1 = rs.getInt("fid1");
	    int fid2 = rs.getInt("fid2");	

	    Flight f1 = getFlight(fid1);

	    boolean isDirectFlight = fid2 == 0;
	    Flight f2 = isDirectFlight ? null : getFlight(fid2);

	    Itinerary itinerary = new Itinerary(f1, f2, f1.time, isDirectFlight);
	    itineraries.add(itinerary);
	}

	rs.close();
	return itineraries;
    }

    /**
     * Return the flight's full information. 
     */
    private Flight getFlight(int fid) throws SQLException {
	getFlightStatement.clearParameters();
	getFlightStatement.setInt(1, fid);
	ResultSet rs = getFlightStatement.executeQuery();

	rs.next();

	int dayOfMonth = rs.getInt("day_of_month");
	String carrierId = rs.getString("carrier_id");
	String flightNum = rs.getString("flight_num");
	String originCity = rs.getString("origin_city");
	String destCity = rs.getString("dest_city");
	int time = rs.getInt("actual_time");
	int capacity = rs.getInt("capacity");
	int price = rs.getInt("price");

	Flight f = new Flight(fid, dayOfMonth, carrierId, flightNum, originCity, destCity, time, capacity, price);	

	rs.close();
	return f;
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
		    int canceled = 1;
		    setUserBalance(this.user, newUserBalance);
		    setReservationCanceled(reservationId, canceled);

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
		    e2.printStackTrace();
		}
	    } finally {
		checkDanglingTransaction();
	    }
	}
	return "Failed to cancel reservation " + reservationId + "\n";
    }

    /**
     * Get the reservation's canceled attribute.
     */
    private int getReservationCanceled(int reservationId) throws SQLException {
	getReservationCanceledStatement.clearParameters();
	getReservationCanceledStatement.setInt(1, reservationId);
	ResultSet rs = getReservationCanceledStatement.executeQuery();
	rs.next();
	int result = rs.getInt("canceled");
	rs.close();
	return result;
    }

    /**
     * Set the reservation's canceled attribute to the given state:
     * true if 1 and false if 0.
     */
    private void setReservationCanceled(int reservationId, int state) throws SQLException {
	setReservationCanceledStatement.clearParameters();
	setReservationCanceledStatement.setInt(1, state);
	setReservationCanceledStatement.setInt(2, reservationId);
	setReservationCanceledStatement.executeUpdate();
    }


    /**
     * Return true if the user has reserved the reservation and false otherwise. 
     */
    private boolean reservationMatchesUser(int reservationId, String username) throws SQLException {
	reservationMatchesUserStatement.clearParameters();
	reservationMatchesUserStatement.setInt(1, reservationId);
	reservationMatchesUserStatement.setString(2, username);
	ResultSet rs = reservationMatchesUserStatement.executeQuery();
	rs.next();
	boolean result = rs.getInt("count") > 0;
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
