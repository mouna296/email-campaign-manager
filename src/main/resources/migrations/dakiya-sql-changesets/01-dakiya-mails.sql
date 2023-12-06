--liquibase formatted sql

--changeset siddhant:dakiya-mails-setup runAlways=false runInTransaction=true failOnError=true
CREATE TABLE dakiya_mails_contents_types
(
  mime_type VARCHAR(100) PRIMARY KEY NOT NULL
);
CREATE UNIQUE INDEX dakiya_mails_contents_types_uindex
  ON dakiya_mails_contents_types (mime_type);
INSERT INTO dakiya_mails_contents_types (mime_type) VALUES ('text');
INSERT INTO dakiya_mails_contents_types (mime_type) VALUES ('html');

CREATE TABLE dakiya_campaigns_mails
(
  subject        TEXT                                              NOT NULL,
  content        TEXT                                              NOT NULL,
  content_type   VARCHAR(100) DEFAULT 'html' :: CHARACTER VARYING NOT NULL,
  from_name      VARCHAR(100)                                      NOT NULL,
  from_email     VARCHAR(100)                                      NOT NULL,
  reply_to_email VARCHAR(100)                                      NOT NULL,
  reply_to_name  VARCHAR(100)                                      NOT NULL,
  id             INTEGER PRIMARY KEY                               NOT NULL,
  creator        VARCHAR(100)                                      NOT NULL
);

CREATE SEQUENCE dakiya_campaigns_mails_id_seq
INCREMENT 1
MINVALUE 1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

ALTER SEQUENCE dakiya_campaigns_mails_id_seq OWNED BY dakiya_campaigns_mails.id;
ALTER TABLE ONLY dakiya_campaigns_mails
  ALTER COLUMN id SET DEFAULT nextval('dakiya_campaigns_mails_id_seq' :: REGCLASS);
ALTER TABLE dakiya_campaigns_mails
  ADD FOREIGN KEY (content_type) REFERENCES dakiya_mails_contents_types (mime_type) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT;
