group sql;

event_type_id()         ::= << (select event_type_id from event_types where event_type = :event_type ) >>
host_id()               ::= << (select id from hosts where host = :host ) >>
type_id()               ::= << (select id from core_types where dep_type = :type ) >>
path_id()               ::= << (select id from dep_paths where dep_path = :path ) >>
time_clause(between)    ::= << ts <if(between)> between :from and :to <else> \>= :from <endif> >>

host_event_values(between,reductionTag) ::= <<
    select * from host_events<reductionTag>
    where <time_clause(between)>
    and event_type_id = <event_type_id()>
    and host_id = <host_id()>
>>

type_event_values(between,reductionTag) ::= <<
    select * from type_events<reductionTag>
    where <time_clause(between)>
    and event_type_id = <event_type_id()>
    and type_id = <type_id()>
>>

path_event_values(between,reductionTag) ::= <<
    select * from path_events<reductionTag>
    where <time_clause(between)>
    and event_type_id = <event_type_id()>
    and type_id = <type_id()>
    and path_id = <path_id()>
>>
