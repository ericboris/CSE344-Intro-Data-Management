-- (10 points) Find the names of all airlines that ever flew more than 1000 flights in one day (i.e., a specific day/month, but not any 24-hour period). Return only the names of the airlines. Do not return any duplicates (i.e., airlines with the exact same name).
-- Name the output column name.
-- [Output relation cardinality: 12 rows]

-- Indicate the number of rows in the query result:
-- 12

SELECT DISTINCT C.name AS name
  FROM Carriers AS C
	   JOIN Flights AS F 
	   ON F.carrier_id = C.cid
 GROUP BY C.cid, 
	   F.month_id, 
	   F.day_of_month, 
	   F.carrier_id
HAVING COUNT(F.carrier_id) > 1000;