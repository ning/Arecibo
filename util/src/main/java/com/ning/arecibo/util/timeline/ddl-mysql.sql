
create table hosts (
   host_id int not null auto_increment primary key,
   host_name varchar(32) not null,
   created_dt int not null,
   key idx_host(host),
   key idx_created_dt_host(created_dt, host, id)
)
ENGINE=InnoDB DEFAULT CHARSET=latin1;

create table sample_kinds (
   sample_kind_id int not null auto_increment primary key,
   sample_kind varchar(64) not null,
   key idx_sample_kind (sample_kind)
)
ENGINE=InnoDB DEFAULT CHARSET=latin1;

create table timeline_times (
   timeline_times_id bigint not null auto_increment primary key,
   host_id int not null,
   start_time int not null,
   end_time int not null,
   count int not null,
   timeline_times mediumblob not null,
   key idx_host_id_start_time(host_id, start_time),
   key idx_host_id_end_time(host_id, end_time)
)
ENGINE=InnoDB DEFAULT CHARSET=latin1;

create table timeline_chunk (
   sample_timeline_id bigint not null auto_increment primary key,
   sample_kind_id int not null,
   timeline_times_id bigint not null,
   sample_bytes mediumblob not null,
   key idx_timeline_interval (host_id, timeline_interval_id)
)
ENGINE=InnoDB DEFAULT CHARSET=latin1;
