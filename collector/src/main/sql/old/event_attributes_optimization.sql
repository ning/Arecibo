create table key_def
(
	key varchar2(256),
	id integer,
	primary key ( key )
)
organization index
nologging
;

create index key_id on key_def ( id ) ;