create table hosts (
  host_id integer not null auto_increment primary key
, host_name varchar(256) not null
, created_dt integer not null
, unique index host_name_unq (host_name)
, index created_dt_host_id_dx (created_dt, host_id)
) engine = innodb default charset = latin1;

create table sample_kinds (
  sample_kind_id integer not null auto_increment primary key
, sample_kind varchar(256) not null
, unique index sample_kind_unq (sample_kind)
) engine = innodb default charset = latin1;

create table timeline_times (
  timeline_times_id bigint not null auto_increment primary key
, host_id integer not null
, event_category varchar(256)
, start_time integer not null
, end_time integer not null
, count integer not null
, in_row_times varbinary(400) default null
, blob_times mediumblob default null
, not_valid tinyint default 0
, aggregation_level tinyint default 0
, index host_id_start_time_end_time_idx (host_id, start_time, end_time)
, index valid_agg_host_start_time (not_valid, aggregation_level, host_id, start_time)
) engine = innodb default charset = latin1;

create table timeline_chunks (
  sample_timeline_id bigint not null auto_increment primary key
, host_id integer not null
, sample_kind_id integer not null
, sample_count integer not null
, timeline_times_id bigint not null
, start_time integer not null
, in_row_samples varbinary(400) default null
, blob_samples mediumblob default null
, unique index host_id_timeline_times_sample_kind_idx (host_id, timeline_times_id, sample_kind_id)
) engine = innodb default charset = latin1;
