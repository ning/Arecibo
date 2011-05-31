group sql;

getHosts() ::= <<
    select host_name, id from hosts;
>>

getSampleKinds() ::= <<
    select sample_kind, id from sample_kinds;
>>

getSampleKindTimelinesForHostIds(host_ids) ::= <<
    select
        t.host_id,
        t.sample_kind_id,
        t.timeline_times_id,
        t.start_time,
        t.end_time,
        t.count,
        st.sample_bytes
    from
        sample_timeline st,
        timeline_interval t
    where
            t.start_time \>= :start_time
        and t.start_time \<= :end_time
        and st.sample_kind_id = :sample_kind_id
        and t.id = st.timeline_times_id
        and t.host_id in (<host_ids:{ host_id | <host_id> }; separator=",">
>>

getAllSampleKindTimelines() ::= <<
    select
        t.host_id,
        t.sample_kind_id,
        t.timeline_times_id,
        t.start_time,
        t.end_time,
        t.count,
        st.sample_bytes
    from
        sample_timeline st,
        timeline_times t
    where
            t.start_time \>= :start_time
        and t.start_time \<= :end_time
        and st.sample_kind_id = :sample_kind_id
        and t.id = st.timeline_times_id
        and t.host_id in (<host_ids:{ host_id | <host_id> }; separator=",">
>>


getTimelineTimesForHosts(hostNames) ::= <<
    select
        h.host,
        t.timeline_time_id,
        t.start_time,
        t.end_time,
        t.count as count,
  h.host
    from
        timeline_times t,
        host h
    where
            t.start_time \>= :start_time
        and t.start_time \<= :end_time
        and t.host_id = h.id
        and h.host_name in (<hosts:{ h | '<h>' }; separator=",">
>>


