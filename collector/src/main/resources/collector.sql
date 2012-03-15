create table hosts (
  host_id integer not null auto_increment primary key
, host_name varchar(256) not null
, created_dt integer not null
, unique index host_name_unq (host_name)
, index created_dt_host_id_dx (created_dt, host_id)
) engine = innodb default charset = latin1;

create table sample_kinds (
  sample_kind_id integer not null auto_increment primary key
, sample_kind varchar(64) not null
, unique index sample_kind_unq (sample_kind)
) engine = innodb default charset = latin1;

create table timeline_times (
  timeline_times_id bigint not null auto_increment primary key
, host_id integer not null
, start_time integer not null
, end_time integer not null
, count integer not null
, times mediumblob not null
, unique index host_id_start_time_end_time_idx (host_id, start_time, end_time)
) engine = innodb default charset = latin1;

create table timeline_chunks (
  sample_timeline_id bigint not null auto_increment primary key
, host_id integer not null
, sample_kind_id integer not null
, sample_count integer not null
, timeline_times_id bigint not null
, sample_bytes mediumblob not null
, unique index host_id_sample_timeline_id_sample_kind_id_idx (host_id, sample_timeline_id, sample_kind_id)
) engine = innodb default charset = latin1;