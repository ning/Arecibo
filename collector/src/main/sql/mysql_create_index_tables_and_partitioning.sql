CREATE TABLE HOSTS (
	ID          INTEGER NOT NULL PRIMARY KEY,
	HOST        VARCHAR(256) NOT NULL,
	DEP_TYPE    VARCHAR(256),
	DEP_PATH    VARCHAR(256),
	UPDATED_DT   TIMESTAMP,
	STATUS      CHAR(1),
	CONSTRAINT HOSTS_Q UNIQUE (HOST)
)
;

CREATE TABLE CORE_TYPES (
	ID          INTEGER NOT NULL PRIMARY KEY,
	DEP_TYPE    VARCHAR(256)
)
;

CREATE INDEX CORE_TYPES_IDX ON CORE_TYPES ( DEP_TYPE ) ;

CREATE TABLE DEP_PATHS (
	ID          INTEGER NOT NULL PRIMARY KEY,
	DEP_PATH    VARCHAR(256)
)
;

CREATE INDEX DEP_PATHS_IDX ON DEP_PATHS ( DEP_PATH ) ;

CREATE TABLE EVENT_TYPES (
	EVENT_TYPE      VARCHAR(256) NOT NULL PRIMARY KEY,
	EVENT_TYPE_ID   INTEGER NOT NULL,
	ENTRY_MODE      CHAR(1),
	ENTRY_PART_1    VARCHAR(2000),
	ENTRY_PART_2    VARCHAR(2000),
	ENTRY           VARCHAR(1)
)
;

CREATE INDEX EVENT_TYPE_IDX ON EVENT_TYPES ( EVENT_TYPE_ID ) ;

CREATE TABLE GENERIC_EVENTS (
	TS TIMESTAMP NOT NULL,
	EVENT_TYPE_ID INTEGER NOT NULL,
	ENTRY_MODE CHAR(1),
	ENTRY_PART_1 VARCHAR(2000),
	ENTRY_PART_2 VARCHAR(2000),
	ENTRY CHAR(1),
    PRIMARY KEY ( EVENT_TYPE_ID, TS )
)
;

create procedure split_and_sweep
(
   p_table_name varchar2,
   p_partition_name varchar2 DEFAULT 'PART_CURR',
   p_ts timestamp,
   p_keep integer DEFAULT 8
)
as
   l_split_ts varchar(128);
   l_partition_id varchar2(256) ;
   cursor c (tablename varchar2, partitionname varchar2, to_keep integer) is
      select * from (
        select rownum rn, t.* from  (
          select table_name, partition_name from user_tab_partitions
          where partition_name != partitionname
            and table_name = tablename
          order by table_name, substr(partition_name, 3) desc
        ) t
      )
      where rn > to_keep ;
begin
   select to_char(trunc(p_ts, 'MI'), 'YYYY-MM-DD hh24:mi:ss' ) into l_split_ts from dual ;
   select to_char(trunc(p_ts, 'MI'), 'YYMMDDhh24mi' ) into l_partition_id from dual ;
   for r in c (p_table_name, p_partition_name, p_keep) loop
      execute immediate 'alter table ' || r.table_name || ' drop partition ' || r.partition_name ;
   end loop ;
   execute immediate
    'alter table ' || p_table_name || ' split partition '|| p_partition_name ||' at ( timestamp '''|| l_split_ts || ''' ) '
    || ' into ( partition p_'|| l_partition_id ||' , partition PART_CURR )' ;
end;


show errors for procedure split_and_sweep ;



