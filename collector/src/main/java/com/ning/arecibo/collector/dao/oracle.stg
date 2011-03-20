group sql;

nextId() ::= <<
	select generic_seq.nextval from dual
>>

getHosts() ::= <<
   select * from hosts
>>

getHostByName() ::= <<
   select * from hosts where host = :host
>>

insertHost() ::= <<
    insert into hosts ( id, host, dep_type, dep_path, updated_dt, status )
    values ( :id, :host, :dep_type, :dep_path, systimestamp, 'U' )
>>

updateHost() ::= <<
    update hosts set
        host = :host,
        dep_type = :dep_type,
        dep_path = :dep_path,
        updated_dt = systimestamp,
        status = 'U'
    where id = :id
>>


getTypes() ::= <<
   select * from core_types
>>

getPaths() ::= <<
   select * from dep_paths
>>

getEventTypes() ::= <<
   select * from event_types
>>

getTypeByName() ::= <<
   select * from core_types where dep_type = :dep_type 
>>

getPathByName() ::= <<
   select * from dep_paths where dep_path = :dep_path
>>

getEventTypeByName() ::= <<
   select * from event_types where event_type = :event_type
>>


insertPath() ::= <<
   insert into dep_paths ( id, dep_path ) values ( :id , :dep_path )
>>

insertType() ::= <<
   insert into core_types ( id, dep_type ) values ( :id , :dep_type )
>>

insertEventType() ::= <<
   insert into event_types ( event_type, event_type_id ) values (:event_type, :event_type_id)
>>

insert_host_event(host_event_table) ::= <<
	INSERT /*+ APPEND */ INTO <host_event_table> (
		ts, event_type_id, host_id, entry_mode, entry_part_1, entry_part_2, entry)
	VALUES (
		:ts, :event_type_id, :host_id, :entry_mode, :entry_part_1, :entry_part_2, :entry)
>>

insert_path_event(path_event_table) ::= <<
	INSERT /*+ APPEND */ INTO <path_event_table> (
		ts, event_type_id, path_id, type_id, entry_mode, entry_part_1, entry_part_2, entry)
	VALUES (
		:ts, :event_type_id, :path_id, :type_id, :entry_mode, :entry_part_1, :entry_part_2, :entry)
>>

insert_type_event(type_event_table) ::= <<
	INSERT /*+ APPEND */ INTO <type_event_table> (
		ts, event_type_id, type_id, entry_mode, entry_part_1, entry_part_2, entry)
	VALUES (
		:ts, :event_type_id, :type_id, :entry_mode, :entry_part_1, :entry_part_2, :entry)
>>

insert_generic_event(generic_event_table) ::= <<
	INSERT /*+ APPEND */ INTO <generic_event_table> (
		ts, event_type_id, entry_mode, entry_part_1, entry_part_2, entry)
	VALUES (
		:ts, :event_type_id, :entry_mode, :entry_part_1, :entry_part_2, :entry)
>>

split_and_sweep() ::= <<
BEGIN
	split_and_sweep (
	   p_table_name => :table_name,
	   p_ts => :ts,
	   p_keep => :keep
	);
END;
>>

table_space_stats() ::= <<
select
   fs.freespace                                "freeAllocatedMB",
   df.totalspace                               "totalAllocatedMB",
   (df.totalspace - fs.freespace)              "usedMB"
from
   (select
      tablespace_name,
      round(sum(bytes) / 1048576) TotalSpace
   from
      dba_data_files
   group by
      tablespace_name
   ) df,
   (select
      tablespace_name,
      round(sum(bytes) / 1048576) FreeSpace
   from
      dba_free_space
   group by
      tablespace_name
   ) fs
where df.tablespace_name = fs.tablespace_name
  and fs.tablespace_name = :tablespace
>>