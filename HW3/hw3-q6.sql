/* PROMPT
6. List the names of carriers that operate flights from Seattle to San Francisco, CA. Return each carrier's name only once. Use a nested query to answer this question.

Name the output column ​carrier​. Order the output ascending by carrier.
*/

SELECT c1.name AS carrier
  FROM carriers AS c1,
       (SELECT Distinct carrier_id
          FROM flights
         WHERE origin_city = 'Seattle WA' 
           AND dest_city = 'San Francisco CA') AS c2
 WHERE c1.cid = c2.carrier_id
 ORDER BY c1.name ASC;
 
/* RESULT
The number of rows the query returns:
	4
How long the query took:
	4s
The first 20 rows of the result:
	carrier
	Alaska Airlines Inc.
	SkyWest Airlines Inc.
	United Air Lines Inc.
	Virgin America
*/