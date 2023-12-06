--liquibase formatted sql

--changeset siddhant:dakiya-analytics-setup01 runAlways=false runInTransaction=true failOnError=true
CREATE TABLE dakiya_mails_events
(
  trigger_time     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  campaign_id      INTEGER                     NOT NULL,
  campaign_version INTEGER                     NOT NULL,
  recipient        VARCHAR(100)                NOT NULL,
  mail_sent        BOOLEAN,
  remark           TEXT,
  CONSTRAINT dakiya_mails_events_pk PRIMARY KEY (trigger_time, campaign_version, campaign_id, recipient)
);
CREATE UNIQUE INDEX dakiya_mails_events_pk_uindex
  ON dakiya_mails_events (trigger_time, campaign_version, campaign_id, recipient);

CREATE TABLE dakiya_mails_stats
(
  date            DATE         NOT NULL,
  recipient       VARCHAR(100) NOT NULL,
  sent_mail_count INTEGER      NOT NULL,
  CONSTRAINT dakiya_mails_stats_pk PRIMARY KEY (date, recipient)
);
CREATE UNIQUE INDEX dakiya_mails_stats_pk_uindex
  ON dakiya_mails_stats (date, recipient);

--changeset codebuff:dakiya-analytics-setup02 runAlways=false runInTransaction=true failOnError=true
CREATE TABLE dakiya_campaigns_events
(
  campaign_id    INTEGER                     NOT NULL,
  trigger_time   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  version        INTEGER                     NOT NULL,
  mails_filtered INTEGER                     NOT NULL,
  expected_mails INTEGER                     NOT NULL,
  mails_sent     INTEGER                     NOT NULL,
  remark         TEXT                        NOT NULL,
  CONSTRAINT dakiya_campaigns_events_pk PRIMARY KEY (campaign_id, trigger_time, version)
);
CREATE UNIQUE INDEX dakiya_campaigns_events_pk_uindex
  ON dakiya_campaigns_events (campaign_id, trigger_time, version);
