declare
   cursor c is select object_name , object_type from user_objects
   where object_type in ('TABLE', 'CLUSTER', 'MATERIALIZED VIEW', 'SEQUENCE')
   and object_name not like 'BIN$%'
   and object_name not like 'SYS_IOT%'
   order by object_type desc;
begin
   for r in c loop
      execute immediate 'drop ' || r.object_type || ' "' || r.object_name || '" ';
   end loop ;
   execute immediate 'purge recyclebin';
end;
/