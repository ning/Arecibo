group sql;

host_query(event, fields, reduction, win, every) ::= <<
    select
        <fields: { field | avg(<field>) as <field>, }>
        <fields: { field | min(<field>) as min_<field>, }>
        <fields: { field | max(<field>) as max_<field>, }>
        count(*) as datapoints,
        max(timestamp) as timestamp,
        hostName,
        sourceUUID,
        deployedConfigSubPath,
        deployedType,
		'host' as aggType,
		'<reduction>' as reduction
    from <event>.win:time(<win>)
    group by sourceUUID, hostName, deployedConfigSubPath, deployedType
    output snapshot every <every>
>>

path_query(event, fields, reduction, win, every) ::= <<
    select
        <fields: { field | avg(<field>) as <field>, }>
        <fields: { field | min(min_<field>) as min_<field>, }>
        <fields: { field | max(max_<field>) as max_<field>, }>
        max(timestamp) as timestamp,
        avg(datapoints) as datapoints,
        deployedConfigSubPath,
        deployedType,
        count(*) as numHosts,
		'path' as aggType,
		'<reduction>' as reduction
    from <event>(datapoints is not null and datapoints > 0).win:time(<win>).std:unique(hostName)
    group by deployedConfigSubPath, deployedType
    output snapshot every <every>
>>

type_query(event, fields, reduction, win, every) ::= <<
    select
        <fields: { field | avg(<field>) as <field>, }>
        <fields: { field | min(min_<field>) as min_<field>, }>
        <fields: { field | max(max_<field>) as max_<field>, }>
        max(timestamp) as timestamp,
        avg(datapoints) as datapoints,
        deployedType,
        count(*) as numHosts,
		'type' as aggType,
		'<reduction>' as reduction
    from <event>(datapoints is not null and datapoints > 0).win:time(<win>).std:unique(hostName)
    group by deployedType
    output snapshot every <every>
>>

host_query_X(event, fields, reduction, win, every) ::= <<
    select
        <fields: { field | avg(<field>) as <field>, }>
        <fields: { field | min(<field>) as min_<field>, }>
        <fields: { field | max(<field>) as max_<field>, }>
        max(timestamp) as timestamp,
        avg(datapoints) as datapoints,
        hostName,
        sourceUUID,
        deployedConfigSubPath,
        deployedType,
		'host' as aggType,
		'<reduction>' as reduction
    from <event>(datapoints is not null and datapoints > 0).win:time(<win>)
    group by sourceUUID, hostName, deployedConfigSubPath, deployedType
    output snapshot every <every>
>>

path_query_X(event, fields, reduction, win, every) ::= <<
    select
        <fields: { field | avg(<field>) as <field>, }>
        <fields: { field | min(min_<field>) as min_<field>, }>
        <fields: { field | max(max_<field>) as max_<field>, }>
        max(timestamp) as timestamp,
        avg(datapoints) as datapoints,
        avg(numHosts) as numHosts,
        deployedConfigSubPath,
        deployedType,
		'path' as aggType,
		'<reduction>' as reduction
    from <event>(datapoints is not null and datapoints > 0).win:time(<win>)
    group by deployedConfigSubPath, deployedType
    output snapshot every <every>
>>

type_query_X(event, fields, reduction, win, every) ::= <<
    select
        <fields: { field | avg(<field>) as <field>, }>
        <fields: { field | min(min_<field>) as min_<field>, }>
        <fields: { field | max(max_<field>) as max_<field>, }>
        max(timestamp) as timestamp,
        avg(datapoints) as datapoints,
        avg(numHosts) as numHosts,
        deployedType,
		'type' as aggType,
		'<reduction>' as reduction
    from <event>(datapoints is not null and datapoints > 0).win:time(<win>)
    group by deployedType
    output snapshot every <every>
>>


