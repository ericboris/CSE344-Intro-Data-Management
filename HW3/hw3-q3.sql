/*
3. For each origin city, find the percentage of departing flights shorter than 3 hours. For this question, treat flights with NULL actual_time values as longer than 3 hours. 
Name the output columns ​origin_city​ and ​percentage​. Order by percentage value, ascending. Be careful to handle cities without any flights shorter than 3 hours. We will accept either 0 or NULL as the result for those cities. Report percentages as percentages not decimals (e.g., report 75.25 rather than 0.7525). 
[Output relation cardinality: 327]

The number of rows your query returns:
	327
How long the query took:
	7s
The first 20 rows of the result:
	origin_city,percentage
	Guam TT,0.0000000000000
	Pago Pago TT,0.0000000000000
	Aguadilla PR,28.6800000000000
	Anchorage AK,31.6600000000000
	San Juan PR,33.5400000000000
	Charlotte Amalie VI,39.2700000000000
	Ponce PR,40.3200000000000
	Fairbanks AK,49.5400000000000
	Kahului HI,53.3400000000000
	Honolulu HI,54.5300000000000
	San Francisco CA,55.2200000000000
	Los Angeles CA,55.4100000000000
	Seattle WA,57.4100000000000
	New York NY,60.5300000000000
	Long Beach CA,61.7200000000000
	Kona HI,62.9500000000000
	Newark NJ,63.3700000000000
	Plattsburgh NY,64.0000000000000
	Las Vegas NV,64.4700000000000
	Christiansted VI,64.6700000000000
*/

SELECT DISTINCT 
       f2.origin_city AS origin_city,
       CASE WHEN f1.p IS null 
            THEN 0 
            ELSE ROUND(f1.p * 100.00 / f2.p, 2)
        END AS percentage
  FROM (SELECT origin_city, 
               COUNT(actual_time) AS p
          FROM flights
         WHERE actual_time > 0 
           AND actual_time < (3 * 60)
         GROUP BY origin_city) AS f1
  FULL JOIN 
       (SELECT origin_city, 
               COUNT(actual_time) AS p
          FROM flights
         GROUP BY origin_city) AS f2 
    ON f1.origin_city = f2.origin_city
 ORDER BY percentage, origin_city