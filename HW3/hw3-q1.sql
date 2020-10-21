/*
The number of rows your query returns:
	334
How long the query took:
	2s
The first 20 rows of the result:
	origin_city,dest_city,time
	Aberdeen SD,Minneapolis MN,106
	Abilene TX,Dallas/Fort Worth TX,111
	Adak Island AK,Anchorage AK,471
	Aguadilla PR,New York NY,368
	Akron OH,Atlanta GA,408
	Albany GA,Atlanta GA,243
	Albany NY,Atlanta GA,390
	Albuquerque NM,Houston TX,492
	Alexandria LA,Atlanta GA,391
	Allentown/Bethlehem/Easton PA,Atlanta GA,456
	Alpena MI,Detroit MI,80
	Amarillo TX,Houston TX,390
	Anchorage AK,Barrow AK,490
	Appleton WI,Atlanta GA,405
	Arcata/Eureka CA,San Francisco CA,476
	Asheville NC,Chicago IL,279
	Ashland WV,Cincinnati OH,84
	Aspen CO,Los Angeles CA,304
	Atlanta GA,Honolulu HI,649
	Atlantic City NJ,Fort Lauderdale FL,212
*/

SELECT DISTINCT 
	   f1.origin_city AS origin_city, 
	   f1.dest_city AS dest_city,
	   f1.actual_time AS time
  FROM flights AS f1,
       (SELECT origin_city, 
		       MAX(actual_time) AS max_time
          FROM Flights
         GROUP BY origin_city) AS f2
 WHERE f1.origin_city = f2.origin_city 
   AND f1.actual_time = f2.max_time
 ORDER BY f1.origin_city ASC, 
       f1.dest_city ASC;

