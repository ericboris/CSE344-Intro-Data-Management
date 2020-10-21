/*
2. Find all origin cities that only serve flights shorter than 3 hours. You can assume that flights with NULL actual_time are not 3 hours or more.
Name the output column ​city​ and sort them in ascending order alphabetically. List each city only once in the result.

The number of rows your query returns:
	109
How long the query took:
	1s
The first 20 rows of the result:
	origin_city
	Aberdeen SD
	Abilene TX
	Alpena MI
	Ashland WV
	Augusta GA
	Barrow AK
	Beaumont/Port Arthur TX
	Bemidji MN
	Bethel AK
	Binghamton NY
	Brainerd MN
	Bristol/Johnson City/Kingsport TN
	Butte MT
	Carlsbad CA
	Casper WY
	Cedar City UT
	Chico CA
	College Station/Bryan TX
	Columbia MO
	Columbus GA
*/

SELECT DISTINCT 
       f1.origin_city
  FROM flights AS f1,
       (SELECT DISTINCT 
		       origin_city, 
			   MAX(actual_time) AS max_time
          FROM flights
         GROUP BY origin_city) AS f2
 WHERE f1.origin_city = f2.origin_city 
   AND f2.max_time < (3 * 60)
 ORDER BY f1.origin_city ASC;

