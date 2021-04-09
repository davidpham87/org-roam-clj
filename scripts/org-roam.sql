.tables
.headers on
.mode columns


select * from files_clj
limit 20;


select * from files
limit 20;


select * from links
limit 20;

select * from refs
limit 5;

select * from titles;

select "source", "dest", type, properties from links;
