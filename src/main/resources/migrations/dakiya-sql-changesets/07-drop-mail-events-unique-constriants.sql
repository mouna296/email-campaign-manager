--liquibase formatted sql

--changeset siddhant:2017-06-16T08:38:22+00:00 runAlways=false runInTransaction=true failOnError=true

ALTER TABLE dakiya_mails_events
  DROP CONSTRAINT dakiya_mails_events_pk;
CREATE INDEX dakiya_mails_events_trigger_time_index
  ON dakiya_mails_events (trigger_time DESC);
CREATE INDEX dakiya_mails_events_campaign_id_index
  ON dakiya_mails_events (campaign_id);
