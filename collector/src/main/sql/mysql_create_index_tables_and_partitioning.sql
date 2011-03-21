CREATE TABLE HOSTS (
	ID          INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	HOST        VARCHAR(256) NOT NULL,
	DEP_TYPE    VARCHAR(256),
	DEP_PATH    VARCHAR(256),
	UPDATED_DT   TIMESTAMP,
	STATUS      CHAR(1),
	CONSTRAINT HOSTS_Q UNIQUE (HOST)
) ENGINE=InnoDB
;

CREATE TABLE CORE_TYPES (
	ID          INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	DEP_TYPE    VARCHAR(256)
) ENGINE=InnoDB
;

CREATE INDEX CORE_TYPES_IDX ON CORE_TYPES ( DEP_TYPE ) ;

CREATE TABLE DEP_PATHS (
	ID          INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	DEP_PATH    VARCHAR(256)
) ENGINE=InnoDB
;

CREATE INDEX DEP_PATHS_IDX ON DEP_PATHS ( DEP_PATH ) ;

CREATE TABLE EVENT_TYPES (
	EVENT_TYPE_ID   INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	EVENT_TYPE      VARCHAR(256) NOT NULL,
	ENTRY_MODE      CHAR(1),
	ENTRY_PART_1    VARCHAR(2000),
	ENTRY_PART_2    VARCHAR(2000),
	ENTRY           VARCHAR(1)
) ENGINE=InnoDB
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
) ENGINE=InnoDB
;

DELIMITER //
create procedure split_and_sweep
(
   p_table_name varchar(100),
   p_partition_name varchar(100),
   p_ts timestamp,
   p_keep int
)
begin
   declare tmp_table_name varchar(100);
   declare tmp_partition_name varchar(100);
   declare tmp_keep int;

   declare l_split_ts varchar(128);
   declare l_partition_id varchar(256);

   declare v_notfound BOOL default FALSE;
   declare c cursor for
      select tablename, partitionname, to_keep from (
        select rownum rn, t.* from  (
          select table_name, partition_name from user_tab_partitions
          where partition_name != partitionname and table_name = tablename
          order by table_name, substr(partition_name, 3) desc
        ) t
      ) x
      where rn > to_keep;
   declare continue handler for not found set v_notfound := TRUE;

   select to_char(trunc(p_ts, 'MI'), 'YYYY-MM-DD hh24:mi:ss') into l_split_ts from dual;
   select to_char(trunc(p_ts, 'MI'), 'YYMMDDhh24mi') into l_partition_id from dual;


   open c;
   cursor_loop: loop
      fetch c into tmp_table_name, tmp_partition_name, tmp_keep;
      if v_not_found then leave cursor_loop; end if;
      set @alter_stmt = concat('alter table ', tmp_table_name, ' drop partition ', tmp_partition_name);
      prepare alter_stmt from @alter_stmt;
      execute alter_stmt;
      deallocate prepare alter_stmt;
   end loop;
   close c;

   set @alter_stmt = concat('alter table ', p_table_name, ' split partition ', p_partition_name,
   ' at ( timestamp ''', l_split_ts, ''' ) ', ' into ( partition p_', l_partition_id, ' , partition PART_CURR )');
   prepare alter_stmt from @alter_stmt;
   execute alter_stmt;
   deallocate prepare alter_stmt;
end;
//
