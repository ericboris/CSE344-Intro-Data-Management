-- (10 points) Find the day of the week with the longest average arrival delay. Return the name of the day and the average delay.
-- Name the output columns day_of_week and delay, in that order. (Hint: consider using LIMIT. Look up what it does!)
-- [Output relation cardinality: 1 row]

-- Indicate the number of rows in the query result:
-- 1

SELECT D.day_of_week AS day_of_week, 
	   AVG(F.arrival_delay) AS delay
  FROM Flights AS F
  	   JOIN Weekdays AS D 
	   ON D.did = F.day_of_week_id
 GROUP BY F.day_of_week_id
 ORDER BY delay DESC
 LIMIT 1;