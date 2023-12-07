--liquibase formatted sql

--changeset siddhant:dakiya-users-setup runAlways=false runInTransaction=true failOnError=true
CREATE TABLE dakiya_roles
(
  role VARCHAR(100) PRIMARY KEY NOT NULL
);
CREATE UNIQUE INDEX dakiya_roles_uindex
  ON dakiya_roles (role);

CREATE TABLE dakiya_users
(
  email           VARCHAR(100) PRIMARY KEY                                 NOT NULL,
  hashed_password TEXT                                                     NOT NULL,
  role            VARCHAR(100) DEFAULT 'unauthorized' :: CHARACTER VARYING NOT NULL
);
CREATE UNIQUE INDEX dakiya_users_uindex
  ON dakiya_users (email);
ALTER TABLE dakiya_users
  ADD FOREIGN KEY (role) REFERENCES dakiya_roles (role) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT;

CREATE TABLE dakiya_users_details
(
  email      VARCHAR(100) PRIMARY KEY                  NOT NULL,
  first_name VARCHAR(100)                              NOT NULL,
  last_name  VARCHAR(100)                              NOT NULL,
  created_on TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);
ALTER TABLE dakiya_users_details
  ADD FOREIGN KEY (email) REFERENCES dakiya_users (email) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT;

INSERT INTO dakiya_roles (role) VALUES ('campaign_manager');
INSERT INTO dakiya_roles (role) VALUES ('super_user');
INSERT INTO dakiya_roles (role) VALUES ('campaign_supervisor');
INSERT INTO dakiya_roles (role) VALUES ('unauthorized');

--changeset siddhant:dakiya-settings-setup runAlways=false runInTransaction=true failOnError=true
CREATE TABLE dakiya_settings
(
  key   TEXT PRIMARY KEY NOT NULL,
  value TEXT             NOT NULL
);
CREATE UNIQUE INDEX dakiya_settings_uindex
  ON dakiya_settings (key);





