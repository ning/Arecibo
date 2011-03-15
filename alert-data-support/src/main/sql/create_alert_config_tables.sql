-- these are ordered in creation dependency order
-- I stuck to the principle of having an index
-- on all foreign keys (prevent locking problems, apparently)


CREATE SEQUENCE alert_config_sequence;


CREATE TABLE person (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
first_name VARCHAR2(32),
last_name VARCHAR2(32),
is_group_alias CHAR(1) DEFAULT '0' NOT NULL,
-- primary key
CONSTRAINT person_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT person_unq UNIQUE (label)
);



CREATE TABLE messaging_description (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
message_type VARCHAR2(32) NOT NULL,
message_text VARCHAR2(4000),
-- primary key
CONSTRAINT messaging_desc_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT messaging_desc_unq UNIQUE (label)
);



CREATE TABLE managing_key (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
key VARCHAR2(32) NOT NULL,
action VARCHAR2(32) NOT NULL,
activated_indefinitely CHAR(1) DEFAULT '0' NOT NULL,
activated_until_ts TIMESTAMP,
auto_activate_tod_start_ms NUMBER,
auto_activate_tod_end_ms NUMBER,
auto_activate_dow_start NUMBER,
auto_activate_dow_end NUMBER,
-- primary key
CONSTRAINT managing_key_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT managing_key_unq UNIQUE (label)
);



CREATE TABLE notif_group (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
enabled CHAR(1) DEFAULT '0' NOT NULL,
-- primary key
CONSTRAINT notif_group_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT notif_group_unq UNIQUE (label)
);



CREATE TABLE level_config (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
color VARCHAR2(32) NOT NULL,
default_notif_group_id NUMBER,
-- primary key
CONSTRAINT level_config_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT level_config_unq UNIQUE (label),
-- foreign keys
CONSTRAINT notif_group_id_fklc FOREIGN KEY (default_notif_group_id)
    REFERENCES notif_group (id) ON DELETE SET NULL
);
-- indexes for foreign keys
CREATE INDEX notif_group_fklc_idx
    ON level_config(default_notif_group_id);



CREATE TABLE alerting_config (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
parent_config_id NUMBER,
level_config_id NUMBER,
status VARCHAR2(32),
type VARCHAR2(32),
enabled CHAR(1),
notif_repeat_mode VARCHAR2(32),
notif_repeat_interval_ms NUMBER,
notif_on_recovery CHAR(1),
-- primary key
CONSTRAINT alerting_config_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT alerting_config_unq UNIQUE (label),
-- foreign keys
CONSTRAINT parent_config_fkac FOREIGN KEY (parent_config_id)
    REFERENCES alerting_config (id) ON DELETE SET NULL,
CONSTRAINT level_config_fkac FOREIGN KEY (level_config_id)
    REFERENCES level_config (id) ON DELETE SET NULL
);
-- indexes for foreign keys
CREATE INDEX parent_config_fkac_idx
    ON alerting_config(parent_config_id);
CREATE INDEX level_config_fkac_idx
    ON alerting_config(level_config_id);



CREATE TABLE threshold_config (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
alerting_config_id NUMBER,
monitored_event_type VARCHAR2(64),
monitored_attribute_type VARCHAR2(64),
clearing_interval_ms NUMBER,
min_threshold_value NUMBER,
max_threshold_value NUMBER,
min_threshold_samples NUMBER,
max_sample_window_ms NUMBER,
-- primary key
CONSTRAINT threshold_config_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT threshold_config_unq UNIQUE (label),
-- foreign keys
CONSTRAINT alerting_config_fktc FOREIGN KEY (alerting_config_id)
    REFERENCES alerting_config (id) ON DELETE SET NULL
);
-- indexes for foreign keys
CREATE INDEX alerting_config_fktc_idx
    ON threshold_config(alerting_config_id);



CREATE TABLE threshold_qualifying_attr (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
threshold_config_id NUMBER NOT NULL,
attribute_type VARCHAR2(64) NOT NULL,
attribute_value VARCHAR2(64) NOT NULL,
-- primary key
CONSTRAINT threshold_qual_attr_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT threshold_qual_attr_unq UNIQUE (label),
-- foreign keys
CONSTRAINT threshold_config_fktqa FOREIGN KEY (threshold_config_id)
    REFERENCES threshold_config (id) ON DELETE CASCADE
);
-- indexes for foreign keys
CREATE INDEX threshold_config_fktqa_idx
    ON threshold_qualifying_attr(threshold_config_id);



CREATE TABLE threshold_context_attr (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
threshold_config_id NUMBER NOT NULL,
attribute_type VARCHAR2(64) NOT NULL,
-- primary key
CONSTRAINT threshold_context_attr_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT threshold_context_attr_unq UNIQUE (label),
-- foreign keys
CONSTRAINT threshold_config_fktca FOREIGN KEY (threshold_config_id)
    REFERENCES threshold_config (id) ON DELETE CASCADE
);
-- indexes for foreign keys
CREATE INDEX threshold_config_fktca_idx
   ON threshold_context_attr(threshold_config_id);



CREATE TABLE managing_key_mapping (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
alerting_config_id NUMBER NOT NULL,
managing_key_id NUMBER NOT NULL,
-- primary key
CONSTRAINT managing_key_mapping_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT managing_key_mapping_unq UNIQUE (label),
-- foreign keys
CONSTRAINT alerting_config_fkmkm FOREIGN KEY (alerting_config_id)
    REFERENCES alerting_config (id) ON DELETE CASCADE,
CONSTRAINT managing_key_fkmkm FOREIGN KEY (managing_key_id)
    REFERENCES managing_key (id) ON DELETE CASCADE
);
-- indexes for foreign keys
CREATE INDEX alerting_config_fkmkm_idx
    ON managing_key_mapping(alerting_config_id);
CREATE INDEX managing_key_fkmkm_idx
    ON managing_key_mapping(managing_key_id);



CREATE TABLE notif_config (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
notif_type VARCHAR2(32) NOT NULL,
address VARCHAR2(255) NOT NULL,
person_id NUMBER NOT NULL,
-- primary key
CONSTRAINT notif_config_pk PRIMARY KEY (id),
-- unique label, address
CONSTRAINT notif_config_unq UNIQUE (label),
CONSTRAINT notif_config_unq2 UNIQUE (address),
-- foreign keys
CONSTRAINT person_fkanc FOREIGN KEY (person_id)
    REFERENCES person (id) ON DELETE CASCADE
);
-- indexes for foreign keys
CREATE INDEX person_fkanc_idx
    ON notif_config(person_id);



CREATE TABLE notif_mapping (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
notif_group_id NUMBER NOT NULL,
notif_config_id NUMBER NOT NULL,
-- primary key
CONSTRAINT notif_mapping_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT notif_mapping_unq UNIQUE (label),
-- foreign keys
CONSTRAINT notif_group_fkanm FOREIGN KEY (notif_group_id)
    REFERENCES notif_group (id) ON DELETE CASCADE,
CONSTRAINT notif_config_fkanm FOREIGN KEY (notif_config_id)
    REFERENCES notif_config (id) ON DELETE CASCADE
);
-- indexes for foreign keys
CREATE INDEX notif_group_fkanm_idx
    ON notif_mapping(notif_group_id);
CREATE INDEX notif_config_fkanm_idx
    ON notif_mapping(notif_config_id);



CREATE TABLE notif_group_mapping (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
notif_group_id NUMBER NOT NULL,
alerting_config_id NUMBER NOT NULL,
-- primary key
CONSTRAINT notif_group_mapping_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT notif_group_mapping_unq UNIQUE (label),
-- foreign keys
CONSTRAINT notif_group_fkagm FOREIGN KEY (notif_group_id)
    REFERENCES notif_group (id) ON DELETE CASCADE,
CONSTRAINT alerting_config_fkagm FOREIGN KEY (alerting_config_id)
    REFERENCES alerting_config (id) ON DELETE CASCADE
);
-- indexes for foreign keys
CREATE INDEX notif_group_fkagm_idx
    ON notif_group_mapping(notif_group_id);
CREATE INDEX alerting_config_fkagm_idx
    ON notif_group_mapping(alerting_config_id);




CREATE TABLE alert_incident_log (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
threshold_config_id NUMBER,
context_identifier VARCHAR(128) NOT NULL,
start_time TIMESTAMP,
clear_time TIMESTAMP,
initial_alert_event_value NUMBER,
short_description VARCHAR(255),
-- primary key
CONSTRAINT alert_incident_log_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT alert_incident_log_unq UNIQUE (label),
-- foreign keys
CONSTRAINT threshold_config_fkail FOREIGN KEY (threshold_config_id)
    REFERENCES threshold_config (id) ON DELETE SET NULL
);
-- indexes for foreign keys
CREATE INDEX alert_incident_fkail_idx
    ON alert_incident_log(threshold_config_id);
-- index to find incidents by start/clear time
CREATE INDEX alert_incident_st_idx
    ON alert_incident_log(start_time);
CREATE INDEX alert_incident_ct_idx
    ON alert_incident_log(clear_time);



CREATE TABLE acknowledgement_log (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
alert_incident_id NUMBER NOT NULL,
person_id NUMBER NOT NULL,
ack_time TIMESTAMP,
ack_comment VARCHAR2(255),
-- primary key
CONSTRAINT acknowledgement_log_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT acknowledgement_log_unq UNIQUE (label),
-- foreign keys
CONSTRAINT alert_incident_fkal FOREIGN KEY (alert_incident_id)
    REFERENCES alert_incident_log (id) ON DELETE CASCADE,
CONSTRAINT person_fkal FOREIGN KEY (person_id)
    REFERENCES person (id) ON DELETE SET NULL
);
-- indexes for foreign keys
CREATE INDEX alert_incident_fkal_idx
    ON acknowledgement_log(alert_incident_id);
CREATE INDEX person_fkal_idx
    ON acknowledgement_log(person_id);



CREATE TABLE notif_log (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
alert_incident_id NUMBER NOT NULL,
notif_config_id NUMBER NOT NULL,
notif_time TIMESTAMP,
-- primary key
CONSTRAINT notif_log_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT notif_log_unq UNIQUE (label),
-- foreign keys
CONSTRAINT alert_incident_fknl FOREIGN KEY (alert_incident_id)
    REFERENCES alert_incident_log (id) ON DELETE CASCADE,
CONSTRAINT notif_config_fkanl FOREIGN KEY (notif_config_id)
    REFERENCES notif_config (id) ON DELETE SET NULL
);
-- indexes for foreign keys
CREATE INDEX alert_incident_fknl_idx
    ON notif_log(alert_incident_id);
CREATE INDEX notif_config_fkanl_idx
    ON notif_log(notif_config_id);



CREATE TABLE managing_key_log (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
managing_key_id NUMBER NOT NULL,
action VARCHAR2(32),
start_time TIMESTAMP,
end_time TIMESTAMP,
-- primary key
CONSTRAINT managing_key_log_pk PRIMARY KEY (id),
-- unique label
CONSTRAINT managing_key_log_unq UNIQUE (label),
-- foreign keys
CONSTRAINT managing_key_fkmkl FOREIGN KEY (managing_key_id)
    REFERENCES managing_key (id) ON DELETE SET NULL
);
-- indexes for foreign keys
CREATE INDEX managing_key_fkmkl_idx
    ON managing_key_log(managing_key_id);

