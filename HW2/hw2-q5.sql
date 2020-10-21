-- (10 points) Find all airlines that had more than 0.5 percent of their flights out of Seattle be canceled. Return the name of the airline and the percentage of canceled flight out of Seattle. Order the results by the percentage of canceled flights in ascending order.
-- Name the output columns name and percent, in that order.
-- [Output relation cardinality: 6 rows]

-- Indicate the number of rows in the query result:
-- 6

SELECT C.name AS name,
	   (TOTAL(F.canceled) / Count(F.canceled)) * 100 AS percent
  FROM Carriers AS C
	   JOIN Flights AS F 
	   ON F.carrier_id = C.cid
 WHERE F.origin_city = "Seattle WA"
 GROUP BY C.cid, C.name
HAVING percent > 0.5
 ORDER BY percent ASC;