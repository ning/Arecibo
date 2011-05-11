group sql;

getSampleKindTimelinesForHosts(hosts) ::= <<
    select
        t.start_time,
        t.end_time, 
        t.count,
        st.sample_bytes
    from
        sample_timeline st,
        timeline_intervals t
    where 
            t.start_time \>= :start_time
        and t.start_time \<= :end_time
        and t.host_id = h.id
        and st.sample_kind_id = :sample_kind_id
        and t.id = st.timeline_times_id
        and h.host in (<hosts:{ h | '<h>' }; separator=",">

>>


getTimelineTimesForHosts(hostNames) ::= <<
    select 
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
        and h.host in (<hosts:{ h | '<h>' }; separator=",">
        
        

