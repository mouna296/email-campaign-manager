--liquibase formatted sql

--changeset siddhant:2017-07-19T16:27:13+05:30 runAlways=false runInTransaction=true failOnError=true

ALTER TABLE dakiya_campaigns
  ADD approved_by VARCHAR(255) DEFAULT NULL  NULL;
ALTER TABLE dakiya_campaigns
  ADD approved_at TIMESTAMP DEFAULT NULL NULL;

ALTER TABLE dakiya_campaigns_archives
  ADD approved_by VARCHAR(255) DEFAULT NULL  NULL;
ALTER TABLE dakiya_campaigns_archives
  ADD approved_at TIMESTAMP DEFAULT NULL NULL;
ALTER TABLE dakiya_campaigns_archives
  ADD archived_by VARCHAR(255) DEFAULT NULL NULL;
ALTER TABLE dakiya_campaigns_archives
  ADD archived_at TIMESTAMP DEFAULT NULL NULL;

UPDATE dakiya_campaigns_archives
SET archived_by = last_modified_by
WHERE archived_by IS NULL;
UPDATE dakiya_campaigns_archives
SET archived_at = last_modified_time;

UPDATE dakiya_campaigns
SET approved_by = last_modified_by
WHERE approved_by IS NULL AND state = 'approved';
UPDATE dakiya_campaigns
SET approved_at = last_modified_time
WHERE state = 'approved';

UPDATE dakiya_campaigns_archives
SET approved_by = last_modified_by
WHERE approved_by IS NULL;
UPDATE dakiya_campaigns_archives
SET approved_at = last_modified_time;

