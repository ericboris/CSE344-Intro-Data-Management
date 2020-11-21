CREATE TABLE Users (
	username varchar(20),		-- case insensitive username
	salt varbinary(20),		-- cryptographic salt for computing the hash
	hash varbinary(20),		-- hashed password
	balance int,			-- current account balance 
	PRIMARY KEY (username)
);

CREATE TABLE Reservations (
	id int,				-- unique reservation id
	username varchar(20),    	-- username that booked this reservation
	price int,			-- the price of this reservation
	paid int, 			-- boolean: 1 is paid, 0 is unpaid
	cancelled int, 			-- boolean: 1 is paid, 0 is unpaid
	fid1 int,			-- id of first flight in this reservation
	fid2 int,			-- id of second flight in this reservation (optional)
	PRIMARY KEY (id),
	FOREIGN KEY (fid1) REFERENCES Flights (fid),
	FOREIGN KEY (fid2) REFERENCES Flights (fid)
);
