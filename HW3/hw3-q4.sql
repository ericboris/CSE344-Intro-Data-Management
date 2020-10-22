/*
4. List all cities that can be reached from Seattle through one stop (i.e., with any two flights that go through an intermediate city) but ​cannot​ be reached through a direct flight. ​Do not include Seattle as one of these destinations (even though you could get back with two flights)​.
Name the output column ​city​. Order the output ascending by city. 

The number of rows your query returns:
	256
How long the query took:
	4s
The first 20 rows of the result:
	city
	Aberdeen SD
	Abilene TX
	Adak Island AK
	Aguadilla PR
	Akron OH
	Albany GA
	Albany NY
	Alexandria LA
	Allentown/Bethlehem/Easton PA
	Alpena MI
	Amarillo TX
	Appleton WI
	Arcata/Eureka CA
	Asheville NC
	Ashland WV
	Aspen CO
	Atlantic City NJ
	Augusta GA
	Bakersfield CA
	Bangor ME
*/

SELECT DISTINCT 
       f1.dest_city AS city
  FROM flights AS f1,
       (SELECT DISTINCT 
		       dest_city
          FROM flights
         WHERE origin_city = 'Seattle WA') AS f2
 WHERE f1.origin_city = f2.dest_city 
   AND f1.dest_city <> 'Seattle WA'

EXCEPT

SELECT DISTINCT 
       f1.dest_city
  FROM flights as f1
 WHERE f1.origin_city = 'Seattle WA'
 ORDER BY f1.dest_city ASC;