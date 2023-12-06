--liquibase formatted sql

--changeset siddhant:dakiya-campaigns-setup runAlways=false runInTransaction=true failOnError=true
CREATE TABLE dakiya_campaigns_states
(
  state VARCHAR(100) PRIMARY KEY NOT NULL
);
CREATE UNIQUE INDEX campaigns_states_uindex
  ON dakiya_campaigns_states (state);
INSERT INTO dakiya_campaigns_states (state) VALUES ('archived');
INSERT INTO dakiya_campaigns_states (state) VALUES ('approved');
INSERT INTO dakiya_campaigns_states (state) VALUES ('not_approved');

CREATE TABLE dakiya_campaigns
(
  id                 INTEGER PRIMARY KEY                                                  NOT NULL,
  sql                TEXT                                                                 NOT NULL,
  about              TEXT                                                                 NOT NULL,
  title              VARCHAR(100)                                                         NOT NULL,
  created_on         TIMESTAMP WITHOUT TIME ZONE                                          NOT NULL,
  start_at           TIMESTAMP WITHOUT TIME ZONE                                          NOT NULL,
  last_modified_time TIMESTAMP WITHOUT TIME ZONE DEFAULT now()                            NOT NULL,
  last_modified_by   VARCHAR(100)                                                         NOT NULL,
  campaign_creator   VARCHAR(100)                                                         NOT NULL,
  version            INTEGER DEFAULT '0' :: INTEGER                                       NOT NULL,
  state              VARCHAR(100) DEFAULT 'not_approved' :: CHARACTER VARYING             NOT NULL,
  mail_dbid          INTEGER                                                              NOT NULL,
  end_at             TIMESTAMP WITHOUT TIME ZONE                                          NOT NULL,
  repeat_period      VARCHAR(100)                                                         NOT NULL,
  repeat_threshold   INTEGER                                                              NOT NULL,
  mail_limit         INTEGER DEFAULT '2' :: INTEGER                                       NOT NULL
);

CREATE SEQUENCE campaigns_id_seq
INCREMENT 1
MINVALUE 1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

ALTER SEQUENCE campaigns_id_seq OWNED BY dakiya_campaigns.id;
ALTER TABLE ONLY dakiya_campaigns
  ALTER COLUMN id SET DEFAULT nextval('campaigns_id_seq' :: REGCLASS);
ALTER TABLE dakiya_campaigns
  ADD FOREIGN KEY (campaign_creator) REFERENCES dakiya_users (email) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT;
ALTER TABLE dakiya_campaigns
  ADD FOREIGN KEY (state) REFERENCES dakiya_campaigns_states (state) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT;
ALTER TABLE dakiya_campaigns
  ADD FOREIGN KEY (mail_dbid) REFERENCES dakiya_campaigns_mails (id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT;

CREATE TABLE dakiya_campaigns_archives
(
  id                 INTEGER                                                                     NOT NULL,
  sql                TEXT                                                                        NOT NULL,
  version            INTEGER                                                                     NOT NULL,
  about              TEXT                                                                        NOT NULL,
  title              VARCHAR(100)                                                                NOT NULL,
  created_on         TIMESTAMP WITHOUT TIME ZONE                                                 NOT NULL,
  start_at           TIMESTAMP WITHOUT TIME ZONE                                                 NOT NULL,
  state              VARCHAR(100) DEFAULT 'archived' :: CHARACTER VARYING                        NOT NULL,
  last_modified_time TIMESTAMP WITHOUT TIME ZONE DEFAULT now()                                   NOT NULL,
  last_modified_by   VARCHAR(100)                                                                NOT NULL,
  campaign_creator   VARCHAR(100)                                                                NOT NULL,
  mail_dbid          INTEGER                                                                     NOT NULL,
  end_at             TIMESTAMP WITHOUT TIME ZONE                                                 NOT NULL,
  repeat_period      VARCHAR(100)                                                                NOT NULL,
  repeat_threshold   INTEGER                                                                     NOT NULL,
  mail_limit         INTEGER DEFAULT '2' :: INTEGER                                              NOT NULL
);
