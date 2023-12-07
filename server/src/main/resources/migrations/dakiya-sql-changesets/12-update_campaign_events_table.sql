--liquibase formatted sql

--changeset siddhant:2018-05-02T14:01:20+00:00 runAlways=false runInTransaction=true failOnError=true

ALTER TABLE dakiya_campaigns_events
  ADD chunk_number int NULL;
ALTER TABLE dakiya_campaigns_events
  ADD total_chunk_count int NULL;
