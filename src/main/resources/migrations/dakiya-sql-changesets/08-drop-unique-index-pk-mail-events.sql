--liquibase formatted sql

--changeset siddhant:2017-06-18T05:54:10+00:00 runAlways=false runInTransaction=true failOnError=true

DROP INDEX IF EXISTS dakiya_mails_events_pk_uindex RESTRICT;
