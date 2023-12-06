--liquibase formatted sql

--changeset siddhant:dakiya-dynamic-email runAlways=false runInTransaction=true failOnError=true

CREATE TABLE public.dakiya_mail_types (
  mail_type CHARACTER VARYING(100) PRIMARY KEY NOT NULL
);

INSERT INTO public.dakiya_mail_types (mail_type) VALUES ('static');
INSERT INTO public.dakiya_mail_types (mail_type) VALUES ('dynamic');

ALTER TABLE public.dakiya_campaigns_mails
  ADD mail_type VARCHAR(100) DEFAULT 'static' NOT NULL;
ALTER TABLE public.dakiya_campaigns_mails
  ADD CONSTRAINT dakiya_campaigns_mail_type_fk
FOREIGN KEY (mail_type) REFERENCES dakiya_mail_types (mail_type) ON DELETE RESTRICT ON UPDATE CASCADE;
