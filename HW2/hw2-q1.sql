-- (10 points) List the distinct flight numbers of all flights from Seattle to Boston by Alaska Airlines Inc. on Mondays. Also notice that, in the database, the city names include the state. So Seattle appears as Seattle WA. Please use the flight_num column instead of fid. Name the output column flight_num.
-- [Hint: Output relation cardinality: 3 rows]
-- 14

-- Indicate the number of rows in the query result:
-- 3

SELECT DISTINCT F.flight_num AS flight_num
  FROM Carriers AS C
  	   JOIN Flights AS F 
	   ON F.carrier_id = C.cid
	   
       JOIN Weekdays AS D 
	   ON D.did = F.day_of_week_id
 WHERE D.day_of_week = "Monday" 
   AND C.name = "Alaska Airlines Inc." 
   AND F.origin_city = "Seattle WA" 
   AND F.dest_city = "Boston MA";