-- PRAGMA foreign_keys = ON;

CREATE TABLE Carriers (
	cid varchar(7), 
	name varchar(83),
	PRIMARY KEY (cid)  
);

CREATE TABLE Months (
	mid int, 
	month varchar(9),
	PRIMARY KEY (mid)
);

CREATE TABLE Weekdays (
	did int, 
	day_of_week varchar(9),
	PRIMARY KEY (did)
);

CREATE TABLE Flights (
	fid int, 
	month_id int,			-- 1-12
	day_of_month int,		-- 1-31 
	day_of_week_id int,		-- 1-7, 1 = Monday, 2 = Tuesday, etc
	carrier_id varchar(7), 
	flight_num int,
	origin_city varchar(34), 
	origin_state varchar(47), 
	dest_city varchar(34), 
	dest_state varchar(46), 
	departure_delay int,		-- in mins
	taxi_out int,			-- in mins
	arrival_delay int, 		-- in mins
	canceled int, 			-- 1 means canceled
	actual_time int,		-- in mins
	distance int,			-- in miles
	capacity int, 
	price int,			-- in $      
	PRIMARY KEY (fid),
	FOREIGN KEY (carrier_id) REFERENCES Carriers (cid),
	FOREIGN KEY (month_id) REFERENCES Months (mid),
	FOREIGN KEY (day_of_week_id) REFERENCES Weekdays (did)
);

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

.mode csv

.import flight-dataset/carriers.csv Carriers
.import flight-dataset/months.csv Months
.import flight-dataset/weekdays.csv Weekdays
.import flight-dataset/flights-small.csv Flights

