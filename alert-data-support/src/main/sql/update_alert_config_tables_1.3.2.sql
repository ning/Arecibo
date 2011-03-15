DROP TABLE acknowledgement_log;
DROP TABLE notif_log;
DROP TABLE alert_incident_log;


CREATE TABLE alert_incident_log (
-- common fields
id NUMBER NOT NULL,
label VARCHAR2(128) NOT NULL,
create_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
update_timestamp TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
-- instance fields
threshold_config_id NUMBER NOT NULL,
context_identifier VARCHAR(32) NOT NULL,
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

