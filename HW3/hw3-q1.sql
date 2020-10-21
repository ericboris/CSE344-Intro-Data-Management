/* 
The SQL query that once executed returns the expected result.
    ● A comment that indicated the number of rows your query returns
    ● A comment that indicates how long the query took, and
    ● A comment that contains the first 20 rows of the result (if the result has fewer than 20 rows, output all of them).
    ○ You can simply copy and paste the first rows into the comment. 
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