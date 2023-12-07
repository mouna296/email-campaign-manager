--liquibase formatted sql

--changeset siddhant:2018-04-30T14:01:20+00:00 runAlways=false runInTransaction=true failOnError=true

ALTER TABLE dakiya_campaigns
  ADD chunk_count int DEFAULT 1 NOT NULL;
ALTER TABLE dakiya_campaigns_archives
  ADD chunk_count int DEFAULT 1 NOT NULL;

-- default Integer max_value/2 i.e 2^30
ALTER TABLE dakiya_campaigns
  ADD mails_per_chunk int DEFAULT 1073741824 NOT NULL;
ALTER TABLE dakiya_campaigns_archives
  ADD mails_per_chunk int DEFAULT 1073741824 NOT NULL;

ALTER TABLE dakiya_campaigns
  ADD delay_per_chunk_in_minutes int DEFAULT 1 NOT NULL;
ALTER TABLE dakiya_campaigns_archives
  ADD delay_per_chunk_in_minutes int DEFAULT 1 NOT NULL;
