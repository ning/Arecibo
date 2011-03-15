ALTER TABLE threshold_config MODIFY monitored_event_type VARCHAR2(64);
ALTER TABLE threshold_config MODIFY monitored_attribute_type VARCHAR2(64);
ALTER TABLE threshold_qualifying_attr MODIFY attribute_type VARCHAR2(64);
ALTER TABLE threshold_qualifying_attr MODIFY attribute_value VARCHAR2(64);
ALTER TABLE threshold_context_attr MODIFY attribute_type VARCHAR2(64);
ALTER TABLE alert_incident_log MODIFY context_identifier VARCHAR2(128);
ALTER TABLE alert_incident_log MODIFY threshold_config_id NUMBER NULL;

