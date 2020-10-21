/* Q1.1 */

CREATE TABLE Books (
	isbn varchar(17),
	title varchar(100), 
	author varchar(100),
	genre varchar(100),
	publisher varchar(100),
	PRIMARY KEY (isbn)	
);

CREATE TABLE Members (
	name varchar(100),
	phone int,
	PRIMARY KEY (name, phone)
);

CREATE TABLE Lending (
	isbn varchar(17),
	name varchar(100),
	phone int,
	checkout varchar(10),
	returned varchar(10),
	PRIMARY KEY (isbn, name, phone, checkout),
	FOREIGN KEY (isbn) REFERENCES Books (isbn),
	FOREIGN KEY (name) REFERENCES Members (name),
	FOREIGN KEY (phone) REFERENCES Members (phone)
);

/* Q1.2 */

SELECT L.name AS name,
       B.title AS title
  FROM Lending AS L
	   JOIN Books AS B 
	   ON B.isbn = L.isbn
 WHERE L.returned IS NULL
 ORDER BY L.name ASC, B.title ASC;
 
/* Q1.3 */
 
SELECT DISTINCT B.title
  FROM Lending AS L
       JOIN Books AS B
       ON B.isbn = L.isbn
	   
	   JOIN Members AS M
	   ON M.name = L.name
 GROUP BY L.name, L.isbn
HAVING COUNT(*) > 1;

/* Q1.4 */
 
SELECT DISTINCT M.phone AS phone
  FROM Members AS M
  
EXCEPT
 
SELECT DISTINCT M.phone AS phone
  FROM Lending AS L
       JOIN Members AS M
       ON M.name = L.name
	   
	   JOIN Books AS B
	   ON B.isbn = L.isbn
 WHERE B.genre = "Romance";

 /* Q1.5 */
 
SELECT B.publisher 
  FROM Books AS B 
 GROUP BY B.isbn 

EXCEPT 

SELECT B.publisher 
  FROM Books AS B 
       JOIN Lending AS L 
       ON L.isbn = B.isbn 
 GROUP BY L.isbn;

