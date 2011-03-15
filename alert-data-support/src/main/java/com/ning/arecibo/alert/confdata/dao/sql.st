group sql;

nextId() ::= <<
    select alert_config_sequence.nextval from dual
>>

select_all(table) ::= <<
    select * from <table>
>>

select_by_id(table) ::= <<
    select * from <table>
    where id = :id
>>

select_by_column(table,column) ::= <<
    select * from <table>
    where <column> = :columnVal
>>

select_by_column_is_null(table,column) ::= <<
    select * from <table>
    where <column> is null
>>

select_by_column_range(table,column) ::= <<
    select * from <table>
    where <column> between :columnVal1 and :columnVal2
>>

select_by_secondary_double_column_range(table1,column1,table2,column2,column2a,column2b) ::= <<
    select * from <table1> where <column1> in
    (select <column2> from <table2> where <column2a> between :columnVal2a1 and :columnVal2a2 or <column2b> between :columnVal2b1 and :columnVal2b2)
>>

delete_by_id(table) ::= <<
    delete from <table>
    where id = :id
>>

