/*
n->p
m->d
nd->p
nm->d
nm->p
nm->dp
mp->d
ndm->p
*/

/*
R(n,d,m,p)

n->p
{n}+={n,p}
R1(n,p), R2(n,d,m)

m->d
{m}+={d,m}
R1(n,p), R3(n,m), R4(m,d)
*/

/*
n->d
select distinct name, discount
from sales
order by name, discount;
*/

/*
n->m
select distinct name,month
from sales
order by name, month;
*/

/*
n->p ISFD
*/
/*
select distinct name,price
from sales
order by name, price;
select char();
*/

/*
n->dm
n->dp
n->mp
n->dmp
*/

/*
d->n/
select distinct discount,name
from sales
order by discount,name;
*/

/*
d->m
select distinct discount,month
from sales
order by discount,month;
*/

/*
d->p
select distinct discount,price
from sales
order by discount,price;
*/

/*
d->nm
d->np
d->mp
d->nmp
*/

/*
m->n
select distinct month,name
from sales
order by month,name;
*/

/*
m->d ISFD
*/
/*
select distinct month,discount
from sales
order by month,discount;
*/

/*
m->p
select distinct month,price
from sales
order by month,price;
*/

/*
m->nd
m->np
m->dp
m->ndp
*/

/*
p->n
select distinct price,name
from sales
order by price,name;
*/

/*
p->d
select distinct price,discount
from sales
order by price,discount;
*/

/*
p->m
select distinct price,month
from sales
order by price,month;
*/

/*
p->nd
p->nm
p->dm
p->ndm
*/

/*
nd->m
select distinct name,discount,month
from sales
order by name,discount,month;
*/

/*
nd->p ISFD
*/
/*
select distinct name,discount,price
from sales
order by name,discount,price;
*/

/*
nd->mp

/*
nm->d ISFD
*/
/*
select distinct name,month,discount
from sales
order by name,month,discount;
*/

/*
nm->p ISFD
*/
/*
select distinct name,month,price
from sales
order by name,month,price;
*/

/*
nm->dp ISFD
*/
/*
select distinct name,month,discount,price
from sales
order by name,month,discount,price;
*/

/*
np->d
select distinct name,price,discount
from sales
order by name,price,discount;
*/

/*
np->m
*/
/*
select distinct name,price,month	
from sales
order by name,price,month;
*/

/*
np->dm
*/

/*
dm->n
*/
/*
select distinct discount,month,name
from sales
order by discount,month,name;
*/

/*
dm->p
*/
/*
select distinct discount,month,price
from sales
order by discount,month,price;
*/

/*
dm->np
*/

/*
dp->n
*/
/*
select distinct discount,price,name
from sales
order by discount,price,name;
*/

/*
dp->m
*/
/*
select distinct discount,price,month
from sales
order by discount,price,month;
*/

/*
dp-nm
*/

/*
mp->n
*/
/*
select distinct month,price,name
from sales
order by month,price,name;
*/

/*
mp->d ISFD
*/
/*
select distinct month,price,discount
from sales
order by month,price,discount;
*/

/*
mp->nd
*/


/*
ndm->p ISFD
*/
/*
select distinct name,discount,month,price
from sales
order by name,discount,month,price;
*/

/*
nmp->d
select distinct name,month,price,discount
from sales
order by name,month,price,discount;
*/

/*
dmp->n
select distinct discount,month,price,name
from sales
order by discount,month,price,name;
*/
