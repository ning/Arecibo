drop table last_path_events;
create table last_path_events
(
	PATH_ID         INTEGER NOT NULL,
	EVENT_TYPE_ID   INTEGER NOT NULL,
	TS TIMESTAMP    NOT NULL,
	ENTRY_MODE      CHAR(1),
	ENTRY_PART_1    RAW(2000),
	ENTRY_PART_2    RAW(2000),
	ENTRY           BLOB,
  primary key ( PATH_ID , EVENT_TYPE_ID )
)
organization index
compress 1
including TS
overflow
nologging
;

create or replace trigger path_events_trigger
after insert on path_events
for each row
begin

  update last_path_events
    set TS = :new.TS ,
        ENTRY_MODE = :new.ENTRY_MODE,
        ENTRY_PART_1 = :new.ENTRY_PART_1,
        ENTRY_PART_2 = :new.ENTRY_PART_2,
        ENTRY = :new.ENTRY
    where PATH_ID = :new.PATH_ID and EVENT_TYPE_ID = :new.EVENT_TYPE_ID ;

  if SQL%ROWCOUNT = 0 then
    insert into last_path_events (TS, event_type_id, PATH_ID, entry_mode, entry_part_1, entry_part_2, entry)
    values ( :new.ts , :new.event_type_id , :new.PATH_ID , :new.entry_mode, :new.entry_part_1, :new.entry_part_1, :new.entry);
  end if ;

end;
/

show errors for path_events_trigger;