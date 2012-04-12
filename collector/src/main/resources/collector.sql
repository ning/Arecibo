create table hosts (
  host_id integer not null auto_increment primary key
, host_name varchar(256) not null
, created_dt integer not null
, unique index host_name_unq (host_name)
, index created_dt_host_id_dx (created_dt, host_id)
) engine = innodb default charset = latin1;

create table event_categories (
  event_category_id integer not null auto_increment primary key
, event_category varchar(256) not null
, unique index event_category_unq (event_category)
) engine = innodb default charset = latin1;

create table sample_kinds (
  sample_kind_id integer not null auto_increment primary key
, event_category_id integer not null
, sample_kind varchar(256) not null
, unique index sample_kind_unq (event_category_id, sample_kind)
) engine = innodb default charset = latin1;

create table timeline_chunks (
  chunk_id bigint not null auto_increment primary key
, host_id integer not null
, sample_kind_id integer not null
, sample_count integer not null
, start_time integer not null
, end_time integer not null
, not_valid tinyint default 0
, aggregation_level tinyint default 0
, in_row_samples varbinary(400) default null
, blob_samples mediumblob default null
, unique index host_id_timeline_chunk_sample_kind_idx (host_id, sample_kind_id, start_time, aggregation_level)
, index valid_agg_host_start_time (not_valid, aggregation_level, host_id, sample_kind_id, start_time)
) engine = innodb default charset = latin1;

create table last_start_times (
  time_inserted int not null primary key
, start_times mediumtext not null
) engine = innodb default charset = latin1;

insert ignore into timeline_chunks(chunk_id, host_id, sample_kind_id, sample_count, start_time, end_time, in_row_samples, blob_samples)
                           values (0, 0, 0, 0, 0, 0, null, null);
