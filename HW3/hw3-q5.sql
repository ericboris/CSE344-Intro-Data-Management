/* PROMPT
5. List all cities that cannot be reached from Seattle through a direct flight nor with one stop (i.e., with any two flights that go through an intermediate city). Warning: this query might take a while to execute. We will learn about how to speed this up in lecture. 

Name the output column ​city​. Order the output ascending by city.
(You can assume all cities to be the collection of all origin_city or all dest_city)
(Note: Do not worry if this query takes a while to execute. We are mostly concerned with the results)
*/

/* COMMENT
Get all the reachable destination cities and
remove from those any destinations that are 
reachable from Seattle via 1 or 2 flights.

We get that latter set of flights thus:
Lines 48-51 get all the destinations reachable in one flight from Seattle.
Lines 38-45 get all the destinations reachable in two flights from Seattle.
The enclosing query, lines 37-52, joins the results of the two inner queries into a single column of unique cities reachable in one or two flights from Seattle.
*/

SELECT DISTINCT
       f3.dest_city AS city
  FROM flights AS f3

EXCEPT 

SELECT DISTINCT 
       f3.dest_city
  FROM (SELECT DISTINCT 
               f1.dest_city as dest_city
          FROM flights AS f1,
               (SELECT DISTINCT
                       dest_city
                  FROM flights
                 WHERE origin_city = 'Seattle WA') AS f2
         WHERE f1.origin_city = f2.dest_city) as f3
  FULL JOIN 
       (SELECT DISTINCT  
               dest_city
          FROM flights
         WHERE origin_city = 'Seattle WA') AS f4 
    ON f4.dest_city = f3.dest_city
 ORDER BY f3.dest_city ASC
 
/* RESULT
The number of rows the query returns:
	3
How long the query took:
	4s
The first 20 rows of the result:
	city
	Devils Lake ND
	Hattiesburg/Laurel MS
	St. Augustine FL
 */