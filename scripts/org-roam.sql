.tables
.headers on

select * from files
limit 10;

select * from links
limit 10;

select * from refs
limit 5;

select * from titles;

select "from", "to", type, properties from links;
