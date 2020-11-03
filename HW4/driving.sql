CREATE TABLE InsuranceCo (
    name varchar(50),
    phone int,
    PRIMARY KEY (name)
);

CREATE TABLE InsurancePlan (
    maxLiability float,
    insurer varchar(50),
    licensePlate varchar(7),
    FOREIGN KEY (insurer) REFERENCES InsuranceCo (name),
    FOREIGN KEY (licensePlate) REFERENCES Vehicle (licensePlate), 
    PRIMARY KEY (licensePlate)
);

CREATE TABLE Vehicle (
    licensePlate varchar(7),
    year int,
    ownedBy int,
    insuredBy varchar(50),
    PRIMARY KEY (licensePlate),
    FOREIGN KEY (ownedBy) REFERENCES Person (ssn),
    FOREIGN KEY (insuredBy) REFERENCES InsuranceCo (name),
);

CREATE TABLE Person (
    ssn int,
    name varchar(50),
    PRIMARY KEY (ssn)
);

CREATE TABLE Driver (
    driverId int,
    ssn int,
    PRIMARY KEY (driverId),
    FOREIGN KEY (ssn) REFERENCES Person (ssn),
);

CREATE TABLE NonProfessionalDriver (
    driverId int,
    FOREIGN KEY (driverId) REFERENCES Driver (driverId),
    PRIMARY KEY (driverId)
);

CREATE TABLE ProfessionalDriver (
    medicalHistory varchar(50),
    driverId int,
    FOREIGN KEY (driverId) REFERENCES Driver (driverId),
    PRIMARY KEY (driverid)
);

CREATE TABLE Drives (
    driverId int,
    make varchar(50),
    FOREIGN KEY (driverId) REFERENCES NonProfessionalDriver (driverId),
    FOREIGN KEY (make) REFERENCES Car (make)
);

CREATE TABLE Car (
    make varchar(50),
    licensePlate varchar(7),
    FOREIGN KEY (licensePlate) REFERENCES Vehicle (licensePlate),
    PRIMARY KEY (licensePlate)
);

CREATE TABLE Truck (
    capacity int,
    licensePlate varchar(7),
    operatedBy int,
    FOREIGN KEY (licensePlate) REFERENCES Vehicle (licensePlate),
    FOREIGN KEY (operatedBy) REFERENCES ProfessionalDriver (driverId),
    PRIMARY KEY (licensePlate)
);

/* b.

I've created a relation called InsurancePlan to represent "insures". I chose this representation
because it allows us to connect the idea of maxLiability to a particular InsuranceCo (insurer)
and to a particular Vehicle (licensePlate without relying on either InsuranceCo or Vehicle to
maintain an attribute for maxLiability, since in this case, neither is appropriate for 
maintaining such an attribute. Further, this allows us to uniquely map an insurance plan onto a
vehicle. 

 */

/* c.

Since the relationship between cars and nonprofessional drivers is many to many, I've represented
that relationship by creating a table called Drives that maps nonprofessional drivers onto cars
and cars to nonprofessional drivers. Since the relationship between trucks and professional drivers
is one to many I represented that relationship by giving each truck an operated by attribute that,
since each truck is unique, means that each truck has a single driver. The different relationships
between cars and trucks and their driver dictated their representations.

 */

