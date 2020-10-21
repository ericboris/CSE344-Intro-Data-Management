-- (10 points) Find the maximum price of tickets between Seattle and New York, NY (i.e. Seattle to NY or NY to Seattle). Show the maximum price for each airline separately.
-- Name the output columns carrier and max_price, in that order.
-- [Output relation cardinality: 3 rows]

-- Indicate the number of rows in the query result:
-- 3

SELECT C.name AS name,
	   MAX(price) AS max_price
  FROM Carriers AS C
	   JOIN Flights AS F 
	   ON F.carrier_id = C.cid
 WHERE F.origin_city = "Seattle WA" 
       AND F.dest_city = "New York NY" 
	   OR F.origin_city = "New York NY" 
	   AND F.dest_city = "Seattle WA"
 GROUP BY C.cid, C.name;
	