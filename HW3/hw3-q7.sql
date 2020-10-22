/* PROMPT
7. List the names of carriers that operate flights from Seattle to San Francisco, CA. Return each carrier's name only once. Do not use a nested query to answer this question.

Name the output column ​carrier​. Order the output ascending by carrier.
*/

SELECT DISTINCT
       c.name AS carrier
  FROM carriers AS c 
  JOIN flights AS f 
    ON f.carrier_id = c.cid
 WHERE f.origin_city = 'Seattle WA' 
   AND f.dest_city = 'San Francisco CA'
 GROUP BY c.name
 ORDER BY c.name;

/* RESULT
The number of rows the query returns:
	4
How long the query took:
	2s
The first 20 rows of the result:
	carrier
	Alaska Airlines Inc.
	SkyWest Airlines Inc.
	United Air Lines Inc.
	Virgin America
*/