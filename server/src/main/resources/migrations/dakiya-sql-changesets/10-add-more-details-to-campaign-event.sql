--liquibase formatted sql

--changeset siddhant:2017-09-04T14:01:20+00:00 runAlways=false runInTransaction=true failOnError=true
ALTER TABLE dakiya_campaigns
  ADD dakiya_instance_type VARCHAR(255) DEFAULT 'transactional' NOT NULL;
ALTER TABLE dakiya_campaigns_archives
  ADD dakiya_instance_type VARCHAR(255) DEFAULT 'transactional' NOT NULL;
ALTER TABLE dakiya_campaigns_events
  ADD dakiya_instance_type VARCHAR(255) DEFAULT 'transactional' NOT NULL;
ALTER TABLE dakiya_campaigns_events
  ADD campaign_last_modified_by VARCHAR(255) DEFAULT 'transactional' NOT NULL;


