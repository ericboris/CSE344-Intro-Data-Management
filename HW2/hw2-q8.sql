-- (10 points) Compute the total departure delay of each airline across all flights. Some departure delays may be negative (indicating an early departure); they should reduce the total, so you don't need to handle them specially. Name the output columns name and delay, in that order. 
-- [Output relation cardinality: 22 rows]

-- Indicate the number of rows in the query result:
-- 22

SELECT C.name AS name, 
	   SUM(F.departure_delay) AS delay
  FROM Carriers AS C
       JOIN Flights AS F 
	   ON F.carrier_id = C.cid
 GROUP BY C.name;