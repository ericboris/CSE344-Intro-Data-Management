/*
3. For each origin city, find the percentage of departing flights shorter than 3 hours. For this question, treat flights with NULL actual_time values as longer than 3 hours. 
Name the output columns ​origin_city​ and ​percentage​. Order by percentage value, ascending. Be careful to handle cities without any flights shorter than 3 hours. We will accept either 0 or NULL as the result for those cities. Report percentages as percentages not decimals (e.g., report 75.25 rather than 0.7525). 
[Output relation cardinality: 327]

The number of rows your query returns:
	
How long the query took:
	
The first 20 rows of the result:

*/

select distinct f1.origin_city, (f2.time / count(actual_time)) as percent
from flights as f1,
(select distinct origin_city, count(actual_time) as time
from flights
where actual_time > 0 AND actual_time < (3 * 60)
group by origin_city) as f2
where f1.origin_city = f2.origin_city
group by f1.origin_city
