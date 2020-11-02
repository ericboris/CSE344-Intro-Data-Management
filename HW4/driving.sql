CREATE TABLE InsuranceCo (
    name varchar(50),
    phone int,
    PRIMARY KEY (name)
);

CREATE TABLE InsurancePlan (
    maxLiability float,
    FOREIGN KEY (insurer) REFERENCES InsuranceCo (name),
    FOREIGN KEY (licensePlate) REFERENCES Vehicle (licensePlate)  
);

CREATE TABLE Vehicle (
    licensePlate varchar(7),
    year int,
    PRIMARY KEY (licensePlate)
    FOREIGN KEY (ownedByy) REFERENCES Person (ssn)
    FOREIGN KEY (insuredBy) REFERENCES InsuranceCo (name)
);

CREATE TABLE Person (
    ssn int,
    name varchar(50)
    PRIMARY KEY (ssn)
);

CREATE TABLE Driver (
    driverId int,
    PRIMARY KEY (driverId)
    FOREIGN KEY (drivenBy) REFERENCES NonProfessionalDriver (ssn)
);

CREATE TABLE Car (
    make varchar(50)
    FOREIGN KEY (licensePlate) REFERENCES Vehicle (licensePlate)
);

CREATE TABLE Truck (
    capacity int
    FOREIGN KEY (licensePlate) REFERENCES Vehicle (licensePlate)
    FOREIGN KEY (operatedBy) REFERENCES ProfessionalDriver (driverId)
);

CREATE TABLE NonProfessionalDriver (
    FOREIGN KEY (driverId) REFERENCES Driver (driverId)
);

CREATE TABLE ProfessionalDriver (
    medicalHistory varchar(50) 
    FOREIGN KEY (driverId) REFERENCES Driver (driverId)
);

/* b. */

/* c. */
