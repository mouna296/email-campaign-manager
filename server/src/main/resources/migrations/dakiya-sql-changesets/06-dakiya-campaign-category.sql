--liquibase formatted sql

--changeset siddhant:2017-05-05T20:55:54+06:00 runAlways=false runInTransaction=true failOnError=true

ALTER TABLE dakiya_campaigns
  ADD category VARCHAR(100) DEFAULT 'default' NOT NULL;
ALTER TABLE dakiya_campaigns_archives
  ADD category VARCHAR(100) DEFAULT 'default' NOT NULL;

