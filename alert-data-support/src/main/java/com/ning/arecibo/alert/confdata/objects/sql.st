group sql;

insert_acknowledgement_log() ::= <<
    insert into acknowledgement_log (
        id,
        label,
        alert_incident_id,
        person_id,
        ack_time,
        ack_comment)
    values (
        :id,
        :label,
        :alert_incident_id,
        :person_id,
        :ack_time,
        :ack_comment)
>>

update_acknowledgement_log() ::= <<
    update acknowledgement_log set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        alert_incident_id = :alert_incident_id,
        person_id = :person_id,
        ack_time = :ack_time
        ack_comment = :ack_comment
    where
        id = :id
>>



insert_alerting_config() ::= <<
    insert into alerting_config (
        id,
        label,
        parent_config_id,
        level_config_id,
        status,
        type,
        enabled,
        notif_repeat_mode,
        notif_repeat_interval_ms,
        notif_on_recovery)
    values (
        :id,
        :label,
        :parent_config_id,
        :level_config_id,
        :status,
        :type,
        :enabled,
        :notif_repeat_mode,
        :notif_repeat_interval_ms,
        :notif_on_recovery)
>>

update_alerting_config() ::= <<
    update alerting_config set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        parent_config_id = :parent_config_id,
        level_config_id = :level_config_id,
        status = :status,
        type = :type,
        enabled = :enabled,
        notif_repeat_mode = :notif_repeat_mode,
        notif_repeat_interval_ms = :notif_repeat_interval_ms,
        notif_on_recovery = :notif_on_recovery
    where
        id = :id
>>



insert_notif_group_mapping() ::= <<
    insert into notif_group_mapping (
        id,
        label,
        notif_group_id,
        alerting_config_id)
    values (
        :id,
        :label,
        :notif_group_id,
        :alerting_config_id)
>>

update_notif_group_mapping() ::= <<
    update notif_group_mapping set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        notif_group_id = :notif_group_id,
        alerting_config_id = :alerting_config_id
    where
        id = :id
>>



insert_notif_group() ::= <<
    insert into notif_group (
        id,
        label,
        enabled)
    values (
        :id,
        :label,
        :enabled)
>>

update_notif_group() ::= <<
    update notif_group set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        enabled = :enabled
    where
        id = :id
>>



insert_alert_incident_log() ::= <<
    insert into alert_incident_log (
        id,
        label,
        threshold_config_id,
        context_identifier,
        short_description,
        start_time,
        clear_time,
        initial_alert_event_value)
    values (
        :id,
        :label,
        :threshold_config_id,
        :context_identifier,
        :short_description,
        :start_time,
        :clear_time,
        :initial_alert_event_value)
>>

update_alert_incident_log() ::= <<
    update alert_incident_log set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        threshold_config_id = :threshold_config_id,
        context_identifier = :context_identifier,
        short_description = :short_description,
        start_time = :start_time,
        clear_time = :clear_time,
        initial_alert_event_value = :initial_alert_event_value
    where
        id = :id
>>



insert_managing_key_mapping() ::= <<
    insert into managing_key_mapping (
        id,
        label,
        alerting_config_id,
        managing_key_id)
    values (
        :id,
        :label,
        :alerting_config_id,
        :managing_key_id)
>>

update_managing_key_mapping() ::= <<
    update managing_key_mapping set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        alerting_config_id = :alerting_config_id,
        managing_key_id = :managing_key_id
    where
        id = :id
>>



insert_notif_config() ::= <<
    insert into notif_config (
        id,
        label,
        person_id,
        notif_type,
        address)
    values (
        :id,
        :label,
        :person_id,
        :notif_type,
        :address)
>>

update_notif_config() ::= <<
    update notif_config set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        person_id = :person_id,
        notif_type = :notif_type,
        address = :address
    where
        id = :id
>>



insert_notif_mapping() ::= <<
    insert into notif_mapping (
        id,
        label,
        notif_group_id,
        notif_config_id)
    values (
        :id,
        :label,
        :notif_group_id,
        :notif_config_id)
>>

update_notif_mapping() ::= <<
    update notif_mapping set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        notif_group_id = :notif_group_id,
        notif_config_id = :notif_config_id
    where
        id = :id
>>



insert_level_config() ::= <<
    insert into level_config (
        id,
        label,
        color,
        default_notif_group_id)
    values (
        :id,
        :label,
        :color,
        :default_notif_group_id)
>>

update_level_config() ::= <<
    update level_config set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        color = :color,
        default_notif_group_id = :default_notif_group_id
    where
        id = :id
>>



insert_managing_key_log() ::= <<
    insert into managing_key_log (
        id,
        label,
        managing_key_id,
        action,
        start_time,
        end_time)
    values (
        :id,
        :label,
        :managing_key_id,
        :action,
        :start_time,
        :end_time)
>>

update_managing_key_log() ::= <<
    update managing_key_log set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        managing_key_id = :managing_key_id,
        action = :action,
        start_time = :start_time,
        end_time = :end_time
    where
        id = :id
>>



insert_managing_key() ::= <<
    insert into managing_key (
        id,
        label,
        key,
        action,
        activated_indefinitely,
        activated_until_ts,
        auto_activate_tod_start_ms,
        auto_activate_tod_end_ms,
        auto_activate_dow_start,
        auto_activate_dow_end)
    values (
        :id,
        :label,
        :key,
        :action,
        :activated_indefinitely,
        :activated_until_ts,
        :auto_activate_tod_start_ms,
        :auto_activate_tod_end_ms,
        :auto_activate_dow_start,
        :auto_activate_dow_end)
>>

update_managing_key() ::= <<
    update managing_key set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        key = :key,
        action = :action,
        activated_indefinitely = :activated_indefinitely,
        activated_until_ts = :activated_until_ts,
        auto_activate_tod_start_ms = :auto_activate_tod_start_ms,
        auto_activate_tod_end_ms = :auto_activate_tod_end_ms,
        auto_activate_dow_start = :auto_activate_dow_start,
        auto_activate_dow_end = :auto_activate_dow_end
    where
        id = :id
>>



insert_messaging_description() ::= <<
    insert into messaging_description (
        id,
        label,
        message_type,
        message_text)
    values (
        :id,
        :label,
        :message_type,
        :message_text)
>>

update_messaging_description() ::= <<
    update messaging_description set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        message_type = :messaging_type,
        message_text = :message_text
    where
       id = :id
>>



insert_notif_log() ::= <<
    insert into notif_log (
        id,
        label,
        alert_incident_id,
        notif_config_id,
        notif_time)
    values (
        :id,
        :label,
        :alert_incident_id,
        :notif_config_id,
        :notif_time)
>>

update_notif_log() ::= <<
    update notif_log set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        alert_incident_id = :alert_incident_id,
        notif_config_id = :notif_config_id,
        notif_time = :notif_time
    where
        id = :id
>>



insert_person() ::= <<
    insert into person (
        id,
        label,
        first_name,
        last_name,
        is_group_alias)
    values (
        :id,
        :label,
        :first_name,
        :last_name,
        :is_group_alias)
>>

update_person() ::= <<
    update person set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        first_name = :first_name,
        last_name = :last_name,
        is_group_alias = :is_group_alias
    where
        id = :id
>>



insert_threshold_config() ::= <<
    insert into threshold_config (
        id,
        label,
        alerting_config_id,
        monitored_event_type,
        monitored_attribute_type,
        clearing_interval_ms,
        min_threshold_value,
        max_threshold_value,
        min_threshold_samples,
        max_sample_window_ms)
    values (
        :id,
        :label,
        :alerting_config_id,
        :monitored_event_type,
        :monitored_attribute_type,
        :clearing_interval_ms,
        :min_threshold_value,
        :max_threshold_value,
        :min_threshold_samples,
        :max_sample_window_ms)
>>

update_threshold_config() ::= <<
    update threshold_config set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        alerting_config_id = :alerting_config_id,
        monitored_event_type = :monitored_event_type,
        monitored_attribute_type = :monitored_attribute_type,
        clearing_interval_ms = :clearing_interval_ms,
        min_threshold_value = :min_threshold_value,
        max_threshold_value = :max_threshold_value,
        min_threshold_samples = :min_threshold_samples,
        max_sample_window_ms = :max_sample_window_ms
    where
        id = :id
>>



insert_threshold_context_attr() ::= <<
    insert into threshold_context_attr (
        id,
        label,
        threshold_config_id,
        attribute_type)
    values (
        :id,
        :label,
        :threshold_config_id,
        :attribute_type)
>>

update_threshold_context_attr() ::= <<
    update threshold_context_attr set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        threshold_config_id = :threshold_config_id,
        attribute_type = :attribute_type
    where
        id = :id
>>



insert_threshold_qualifying_attr() ::= <<
    insert into threshold_qualifying_attr (
        id,
        label,
        threshold_config_id,
        attribute_type,
        attribute_value)
    values (
        :id,
        :label,
        :threshold_config_id,
        :attribute_type,
        :attribute_value)
>>

update_threshold_qualifying_attr() ::= <<
    update threshold_qualifying_attr set
        label = :label,
        update_timestamp = SYSTIMESTAMP,
        threshold_config_id = :threshold_config_id,
        attribute_type = :attribute_type,
        attribute_value = :attribute_value
    where
        id = :id
>>