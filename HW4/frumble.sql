/* 1. */

CREATE TABLE Sales (
    name varchar(6),
    discount varchar(4),
    month varchar(3),
    price int
);

.mode tabs

.import data.txt Sales

/* 2. */

-- TODO Write better queries for the FDs
/*
Functional Dependencies
n->p
m->d
*/

/* 3. */

/*
Decompose into BCNF
R(n,d,m,p)
 
n->p
{n}+={n,p}
R1(n,p), R2(n,d,m)
 
m->d
{m}+={d,m}
R1(n,p), R3(n,m), R4(m,d)
*/

CREATE TABLE nameprice (
    name varchar(6),
    price int,
    FOREIGN KEY (name) REFERENCES Sales (name),
    FOREIGN KEY (price) REFERENCES Sales (price),
    PRIMARY KEY (name)
);

CREATE TABLE namemonth (
    name varchar(6),
    month varchar(3),
    FOREIGN KEY (name) REFERENCES Sales (name),
    FOREIGN KEY (month) REFERENCES Sales (month)
);

CREATE TABLE monthdiscount (
    month varchar(3),
    discount varchar(4),
    FOREIGN KEY (month) REFERENCES Sales (month),
    FOREIGN KEY (discount) REFERENCES Sales (discount),
    PRIMARY KEY (month)
);
