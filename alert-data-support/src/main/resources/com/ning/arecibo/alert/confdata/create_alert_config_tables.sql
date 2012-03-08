create table person (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, first_name varchar(32)
, last_name varchar(32)
, is_group_alias char(1) default '0' not null
, unique index person_unq (label)
) engine = innodb;
create trigger person_create_timestamp before insert on `person`
for each row set new.create_timestamp = now();

create table messaging_description (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, message_type varchar(32) not null
, message_text varchar(4000)
, unique index messaging_desc_unq (label)
) engine = innodb;
create trigger messaging_description_create_timestamp before insert on `messaging_description`
for each row set new.create_timestamp = now();

create table managing_key (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, managing_key varchar(32) not null
, action varchar(32) not null
, activated_indefinitely char(1) default '0' not null
, activated_until_ts timestamp
, auto_activate_tod_start_ms integer
, auto_activate_tod_end_ms integer
, auto_activate_dow_start integer
, auto_activate_dow_end integer
, unique index managing_key_unq (label)
) engine = innodb;
create trigger managing_key_create_timestamp before insert on `managing_key`
for each row set new.create_timestamp = now();

create table notif_group (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, enabled char(1) default '0' not null
, unique index notif_group_unq (label)
) engine = innodb;
create trigger notif_group_create_timestamp before insert on `notif_group`
for each row set new.create_timestamp = now();

create table level_config (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, color varchar(32) not null
, default_notif_group_id integer
, unique index level_config_unq (label)
, index notif_group_fklc_idx (default_notif_group_id)
, foreign key (default_notif_group_id) references notif_group(id) on delete set null
) engine = innodb;
create trigger level_config_create_timestamp before insert on `level_config`
for each row set new.create_timestamp = now();

create table alerting_config (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, parent_config_id integer
, level_config_id integer
, status varchar(32)
, type varchar(32)
, enabled char(1)
, notif_repeat_mode varchar(32)
, notif_repeat_interval_ms integer
, notif_on_recovery char(1)
, unique index alerting_config_unq (label)
, index parent_config_fkac_idx (parent_config_id)
, foreign key (parent_config_id) references alerting_config(id) on delete set null
, index level_config_fkac_idx (level_config_id)
, foreign key (level_config_id) references level_config(id) on delete set null
) engine = innodb;
create trigger alerting_config_create_timestamp before insert on `alerting_config`
for each row set new.create_timestamp = now();

create table threshold_config (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, alerting_config_id integer
, monitored_event_type varchar(64)
, monitored_attribute_type varchar(64)
, clearing_interval_ms integer
, min_threshold_value integer
, max_threshold_value integer
, min_threshold_samples integer
, max_sample_window_ms integer
, unique index threshold_config_unq (label)
, index alerting_config_fktc_idx (alerting_config_id)
, foreign key (alerting_config_id) references alerting_config(id) on delete set null
) engine = innodb;
create trigger threshold_config_create_timestamp before insert on `threshold_config`
for each row set new.create_timestamp = now();

create table threshold_qualifying_attr (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, threshold_config_id integer not null
, attribute_type varchar(64) not null
, attribute_value varchar(64) not null
, unique index threshold_qual_attr_unq (label)
, index threshold_config_fktqa_idx (threshold_config_id)
, foreign key (threshold_config_id) references threshold_config(id) on delete cascade
) engine = innodb;
create trigger threshold_qualifying_attr_create_timestamp before insert on `threshold_qualifying_attr`
for each row set new.create_timestamp = now();

create table threshold_context_attr (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, threshold_config_id integer not null
, attribute_type varchar(64) not null
, unique index threshold_context_attr_unq (label)
, index threshold_config_fktca_idx (threshold_config_id)
, foreign key (threshold_config_id) references threshold_config(id) on delete cascade
) engine = innodb;
create trigger threshold_context_attr_create_timestamp before insert on `threshold_context_attr`
for each row set new.create_timestamp = now();

create table managing_key_mapping (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, alerting_config_id integer not null
, managing_key_id integer not null
, unique index managing_key_mapping_unq (label)
, index alerting_config_fkmkm_idx (alerting_config_id)
, foreign key (alerting_config_id) references alerting_config(id) on delete cascade
, index managing_key_fkmkm_idx (managing_key_id)
, foreign key (managing_key_id) references managing_key(id) on delete cascade
) engine = innodb;
create trigger managing_key_mapping_create_timestamp before insert on `managing_key_mapping`
for each row set new.create_timestamp = now();

create table notif_config (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, notif_type varchar(32) not null
, address varchar(255) not null
, person_id integer not null
, unique index notif_config_unq (label)
, unique index notif_config_unq2 (address)
, index person_fkanc_idx (person_id)
, foreign key (person_id) references person(id) on delete cascade
) engine = innodb;
create trigger notif_config_create_timestamp before insert on `notif_config`
for each row set new.create_timestamp = now();

create table notif_mapping (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, notif_group_id integer not null
, notif_config_id integer not null
, unique index notif_mapping_unq (label)
, index notif_group_fkanm (notif_group_id)
, foreign key (notif_group_id) references notif_group(id) on delete cascade
, index notif_config_fkanm (notif_config_id)
, foreign key (notif_config_id) references notif_config(id) on delete cascade
) engine = innodb;
create trigger notif_mapping_create_timestamp before insert on `notif_mapping`
for each row set new.create_timestamp = now();

create table notif_group_mapping (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, notif_group_id integer not null
, alerting_config_id integer not null
, unique index notif_group_mapping_unq (label)
, index notif_group_fkagm (notif_group_id)
, foreign key (notif_group_id) references notif_group(id) on delete cascade
, index alerting_config_fkagm (alerting_config_id)
, foreign key (alerting_config_id) references alerting_config(id) on delete cascade
) engine = innodb;
create trigger notif_group_mapping_create_timestamp before insert on `notif_group_mapping`
for each row set new.create_timestamp = now();

create table alert_incident_log (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, threshold_config_id integer
, context_identifier varchar(128) not null
, start_time timestamp
, clear_time timestamp
, initial_alert_event_value integer
, short_description varchar(255)
, unique index alert_incident_log_unq (label)
, index threshold_config_fkail (threshold_config_id)
, foreign key (threshold_config_id) references threshold_config(id) on delete set null
) engine = innodb;
create trigger alert_incident_log_create_timestamp before insert on `alert_incident_log`
for each row set new.create_timestamp = now();

create table acknowledgement_log (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, alert_incident_id integer not null
, person_id integer not null
, ack_time timestamp
, ack_comment varchar(255)
, unique index acknowledgement_log_unq (label)
, index alert_incident_fkal (alert_incident_id)
, foreign key (alert_incident_id) references alert_incident_log(id) on delete cascade
, index person_fkal (person_id)
, foreign key (person_id) references person(id) on delete cascade
) engine = innodb;
create trigger acknowledgement_log_create_timestamp before insert on `acknowledgement_log`
for each row set new.create_timestamp = now();

create table notif_log (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, alert_incident_id integer not null
, notif_config_id integer not null
, notif_time timestamp
, unique index notif_log_unq (label)
, index alert_incident_fknl (alert_incident_id)
, foreign key (alert_incident_id) references alert_incident_log(id) on delete cascade
, index notif_config_fkanl (notif_config_id)
, foreign key (notif_config_id) references notif_config(id) on delete cascade
) engine = innodb;
create trigger notif_log_create_timestamp before insert on `notif_log`
for each row set new.create_timestamp = now();

create table managing_key_log (
  id integer not null auto_increment primary key
, label varchar(128) not null
, create_timestamp timestamp default 0
, update_timestamp timestamp default current_timestamp on update current_timestamp
, managing_key_id integer not null
, action varchar(32)
, start_time timestamp
, end_time timestamp
, unique key managing_key_log_unq (label)
, index managing_key_fkmkl (managing_key_id)
, foreign key (managing_key_id) references managing_key(id) on delete cascade
) engine = innodb;
create trigger managing_key_log_create_timestamp before insert on `managing_key_log`
for each row set new.create_timestamp = now();