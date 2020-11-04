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

/*
Functional Dependencies

Process for finding FDs:
1. Enumerate all possible FDs in order of smallest determinants to greatest, i.e.:
    {n->d, n->m, n->p, ... nd->m, nd->p, nd->mp, ... ndm->p, nmp->d, ndp->m}

2. Write a query to determine if a relation is an FD:
    SELECT X FROM Sales GROUP BY X HAVING COUNT(DISTINCT Y) > 1;

    Where X is a set of determinants and Y is a set of dependents.

    For any X, Y pair 
	if the query returns no rows then X->Y 
	else X, Y is not a FD.

3. Run the query for each possible FD in order. After each query, remove from the 
set of possible FDs any X, Y pair that is no longer a possible FD and remove from the
set of possible FDs any X, Y pair that must logically be a trivial FD. This process 
returns:
    {n->p, m->d}

    From these we infer the complete set of FDs of Sales to be:

    {n->p, m->d, nd->p, nm->dp, mp->d, ndm->p}

    However, we're only concerned with the former set. 


The queries that find FDs.

    Find the n->p FD. 
    SELECT name FROM Sales GROUP BY name HAVING COUNT(DISTINCT price) > 1;

    Find the m->d FD.
    SELECT month FROM Sales GROUP BY month HAVING COUNT(DISTINCT discount) > 1;

A query that doesn't find a FD.
    
    Fail to find the d->n FD.
    SELECT discount FROM Sales GROUP BY discount HAVING COUNT(DISTINCT name) > 1;
*/

/* 3. */

/*
Given relation R(n,d,m,p) and FDs {n->p, m->d}, decompose R into BCNF: 

Select FD n->p
Find the closure of n to be {n}+={n,p}
{n,p} violates BCNF, split R into R1(n,p), R2(n,d,m)

Result: R1(n,p), R2(n,d,m)

Select FD m->d
Find the closure of m to be {m}+={d,m}
{d,m} violates BCNF, split R2 into R3(n,m), R4(m,d)

Result: R1(n,p), R3(n,m), R4(m,d)

No more FDs.
R has been decomposed into BCNF.

Result: R1(n,p), R3(n,m), R4(m,d)
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

/* 4. */

INSERT INTO nameprice (name, price)
SELECT distinct name, price
FROM Sales;

INSERT INTO namemonth (name, month)
SELECT distinct name, month
FROM Sales;

INSERT INTO monthdiscount (month, discount)
SELECT distinct month, discount
FROM Sales;
