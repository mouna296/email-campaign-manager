package com.untitled.ecm.constants;

public final class DakiyaStrings {

    public static final String DAKIYA_NAME = "dakiya";
    public static final String SENDGRID = "sendgrid";

    // assuming most of email recipients are in India, however in db everything is in unix timestamp
    public static final String DAKIYA_TIMEZONE_INDIA = "Asia/Kolkata";


    //mailer
    public static final String DAKIYA_IS_TEST_MAIL = "DakiyaIsThisTestEmail";

    // dakiya runtime settings
    public static final String CREATING_CAMPAIGNS_ALLOWED = "creating-campaigns-allowed";
    public static final String SENDING_EMAILS_ALLOWED = "sending-emails-allowed";
    public static final String MAX_EMAILS_ALLOWED_PER_RECIPIENT_PER_DAY = "max-emails-allowed-per-recipient-per-day";
    public static final String sendgridAPIKeyPrefix = "sendgrid-apikey";
    public static final String sendgridAPIKeyDelimiter = "-d-";
    public static final String DEFAULT_SENDGRID_API_KEY_DOMAIN_KEY = DakiyaStrings.sendgridAPIKeyPrefix + DakiyaStrings.sendgridAPIKeyDelimiter + "default";
    public static final String DAKIYA_ENVIRONMENT_TYPE_KEY = "environment-type";
    public static final String DAKIYA_INSTANCE_TYPE = "DakiyaInstanceType";


    // scheduler
    public static final String DAKIYA_SCHEDULER = "dakiyaScheduler";
    public static final String DAKIYA_SCHEDULER_CAMPAIGN_JOBS_GROUP = DakiyaStrings.DAKIYA_SCHEDULER + "CampaignJobsGroup";
    public static final String DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN = "JobCampaign";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_ID = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "ID";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_VERSION = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "Version";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_SENDGRID_DOMAIN = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "SendgridDomain";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_MAIL_ID = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "MailID";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_MAIL_SUBJECT = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "MailSubject";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_START_TIME = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "StartTime";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_END_TIME = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "EndTime";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_PREVIOUS_FIRE_TIME = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "PreviousFireTime";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_NEXT_FIRE_TIME = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "NextFireTime";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_MAY_FIRE_AGAIN = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "MayFireAgain";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_TRIGGER_TIME = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "TriggerTime";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_CATEGORY = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "Category";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_TOTAL_CHUNK_COUNT = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "TotalChunkCount";
    public static final String DAKIYA_SCHEDULER_JOB_CAMPAIGN_CHUNK_NUMBER = DakiyaStrings.DAKIYA_SCHEDULER + DAKIYA_SCHEDULER_JOB_TYPE_CAMPAIGN + "ChunkNumber";
    public static final String DAKIYA_JOB_NO_OF_TRIGGERS = "noOfTriggers";
    // Background Task Manager
    public static final String DAKIYA_BACKGROUND_TASK_MANAGER = "dakiyaBackgroundTaskManager";


    // metabase
    public static final String METABASE_STATUS_OK = "metabase is up and running";
    public static final String NON_SELECT_COMMAND_PRESENT_IN_SQl = "only select command allowed in sql query";
    public static final String SELECT_COMMAND_NOT_PRESENT_IN_SQl = "select command not found in sql query";

    // todo change class name from DakiyaString to something more appropriate
    public static final int METABASE_MAX_PREVIEW_COUNT = 100;
    public static final int MIN_PASS_LENGHT = 10;

    public static String USER_ROLE_HEADER = "X-User-Role";
    public static String USER_EMAIL_HEADER = "X-User-Email";

    private DakiyaStrings() {

    }


}
