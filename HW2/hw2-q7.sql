-- (10 points) Find the total capacity of all direct flights that fly between Seattle and San Francisco, CA on July 10th (i.e. Seattle to SF or SF to Seattle).
-- Name the output column capacity.
-- [Output relation cardinality: 1 row]

-- Indicate the number of rows in the query result:
-- 1

SELECT SUM(F.capacity) AS capacity
  FROM Flights AS F
	   JOIN Months AS M 
	   ON M.mid = F.month_id
 WHERE M.month = "July" 
       AND F.day_of_month = 10 
	   AND ((origin_city = "San Francisco CA" 
	   AND dest_city = "Seattle WA") 
	   OR (origin_city = "Seattle WA" 
	   AND dest_city = "San Francisco CA"));