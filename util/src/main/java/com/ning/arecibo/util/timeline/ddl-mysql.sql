
create table host (
   id int not null auto_increment,
   host varchar(32) not null,
   primary key(id),
   key idx_host(host)
)
ENGINE=InnoDB DEFAULT CHARSET=latin1;

create table sample_kind (
   id int not null auto_increment,
   sample_kind varchar(64) not null,
   primary key (id),
   key idx_sample_kind (sample_kind)
)
ENGINE=InnoDB DEFAULT CHARSET=latin1;

create table timeline_intervals (
   id bigint not null auto_increment,
   host_id int not null,
   start_time int not null,
   end_time int not null,
   count int not null,
   primary key (id),
   key idx_host_id_start_time(host_id, start_time)
)
ENGINE=InnoDB DEFAULT CHARSET=latin1;

create table sample_timeline (
   sample_kind_id int not null,
   timeline_times_id bigint not null,
   sample_bytes varbinary(64000) not null,
   primary key (sample_kind_id, timeline_times_id)
)
ENGINE=InnoDB DEFAULT CHARSET=latin1;
