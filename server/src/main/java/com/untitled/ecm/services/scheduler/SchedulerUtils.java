package com.untitled.ecm.services.scheduler;

import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.core.DakiyaUtils;
import com.untitled.ecm.dtos.Campaign;
import com.untitled.ecm.services.scheduler.jobs.RunCampaign;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.quartz.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.untitled.ecm.core.DakiyaUtils.getISTDateTime;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.impl.matchers.GroupMatcher.groupEquals;

@Slf4j
public final class SchedulerUtils {
    public static void scheduleCampaign(@NonNull final Campaign campaign) throws SchedulerException {
        final JobDetail jobDetail = generateJobDetail(campaign);
        final Trigger trigger = generateTrigger(campaign);
        SchedulerManager.getQuartzScheduler().scheduleJob(jobDetail, trigger);
    }


    public static void scheduleCampaignChunk(@NonNull final Campaign campaign, final int chunkNumber) throws SchedulerException {
        final JobDetail jobDetail = generateJobDetail(campaign);
        long campaignTriggerStartTime = DateTime.now().plusMinutes(campaign.getDelayPerChunkInMinutes()).getMillis();
        final Identity identity = getIdentity(campaign);
        // chunk number is stored in name of trigger key which is used by RunCampaignJob
        final Trigger trigger = newTrigger()
                .withIdentity(generateTriggerKeyForCampaignChunk(campaign.getId(), chunkNumber), identity.getGroup())
                .startAt(new Date(campaignTriggerStartTime))
                .withSchedule(simpleSchedule().withMisfireHandlingInstructionFireNow())
                .forJob(jobDetail)
                .build();
        SchedulerManager.getQuartzScheduler().scheduleJob(trigger);
    }

    private static String generateTriggerKeyForCampaignChunk(int campaignId, int chunkNumber) {
        return campaignId + "_" + chunkNumber + "_" + UUID.randomUUID();
    }

    public static Integer getChunkNumber(@NonNull TriggerKey key) {
        String[] parts = key.getName().split("_");
        if (parts.length != 3) {
            return null;
        }
        return Integer.valueOf(parts[1]);
    }

    private static Trigger generateTrigger(@NonNull final Campaign campaign) {
        ScheduleBuilder<? extends Trigger> scheduleBuilder = generateScheduleBuilder(campaign);
        // this is a workaround to make sure that trigger misfire does not happen for first execution of campaign
        long campaignTriggerStartTime = DakiyaUtils.getStartDateTime(campaign.getStartAt()).getMillis();
        final Identity identity = getIdentity(campaign);
        return newTrigger()
                .withIdentity(identity.getName(), identity.getGroup())
                // also includes time of day and timezone independent
                .startAt(new Date(campaignTriggerStartTime))
                .withSchedule(scheduleBuilder)
                .endAt(new Date(campaign.getEndAtMillis()))
                .build();
    }

    private static ScheduleBuilder<? extends Trigger> generateScheduleBuilder(Campaign campaign) {
        ScheduleBuilder<? extends Trigger> scheduleBuilder;

        if (campaign.getRepeatThreshold() == -1) {
            scheduleBuilder = simpleSchedule()
                    .withIntervalInMilliseconds(campaign.getRepeatPeriodMillis())
                    .withMisfireHandlingInstructionNowWithRemainingCount()
                    .repeatForever();
        } else {
            scheduleBuilder = simpleSchedule()
                    .withIntervalInMilliseconds(campaign.getRepeatPeriodMillis())
                    .withMisfireHandlingInstructionNowWithExistingCount()
                    .withRepeatCount(campaign.getRepeatThreshold());
        }
        return scheduleBuilder;
    }

    private static JobDetail generateJobDetail(@NonNull final Campaign campaign) {
        final Identity identity = getIdentity(campaign);
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.putAsString(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_ID, campaign.getId());
        jobDataMap.putAsString(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_VERSION, campaign.getVersion());
        jobDataMap.putAsString(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_MAIL_ID, campaign.getMailIDinDB());
        return newJob(RunCampaign.class)
                .withIdentity(identity.getName(), identity.getGroup())
                .setJobData(jobDataMap)
                .build();
    }

    public static void unscheduleMultipleCampaigns(@NonNull final List<Integer> campaignIds) throws SchedulerException {
        for (int id : campaignIds) {
            SchedulerUtils.unscheduleCampaign(id);
        }
    }

    public static void unscheduleCampaign(int campaignId) throws SchedulerException {
        final Identity identity = new Identity(campaignId);
        final JobDetail job = newJob(RunCampaign.class)
                .withIdentity(identity.getName(), identity.getGroup())
                .build();
        if (SchedulerManager.getQuartzScheduler().checkExists(job.getKey())) {
            SchedulerManager.getQuartzScheduler().deleteJob(job.getKey());
        } else {
            log.warn("attempted to delete a schedule which does not exist for campaign id " + identity.getName());
        }
    }

    public static void triggerRunCampaignJob(@NonNull final Campaign campaign) throws SchedulerException {
        final JobDetail jobDetails = generateJobDetail(campaign);
        if (SchedulerManager.getQuartzScheduler().checkExists(jobDetails.getKey())) {
            throw new IllegalArgumentException(jobDetails.getKey().toString() + " already exists in the db, " +
                    "cannot create a new schedule for this. " +
                    "Either first delete the schedule for this campaign(GET /unschedule-campaign/{campaign-id}) " +
                    "or update the campaign with new schedule(POST /campaigns/{campaign-id}");
        }

        final Identity identity = getIdentity(campaign);
        final Trigger trigger = newTrigger()
                .withIdentity(identity.getName(), identity.getGroup())
                .startNow()
                .build();

        SchedulerManager.getQuartzScheduler().scheduleJob(jobDetails, trigger);
    }

    public static HashMap<String, Object> getJobDetailsByCampaignID(int id) {
        final Identity identity = getIdentity(id);
        final JobKey jobKey = JobKey.jobKey(identity.getName(), identity.getGroup());
        return getJobDetails(jobKey);
    }

    public static HashMap<String, HashMap<String, Object>> getAllCampaignJobsDetails() throws SchedulerException {
        HashMap<String, HashMap<String, Object>> jobs = new HashMap<>();
        HashMap<String, Object> jobDetails;
        // may be of help later where there might be more than one group, scheduler.getJobGroupNames()
        // this url only deals with campaigns, thus explicit job group
        for (final JobKey jobKey : SchedulerManager.getQuartzScheduler()
                .getJobKeys(groupEquals(DakiyaStrings.DAKIYA_SCHEDULER_CAMPAIGN_JOBS_GROUP))) {
            jobDetails = SchedulerUtils.getJobDetails(jobKey);
            jobs.put(jobKey.toString(), jobDetails);
        }
        return jobs;
    }

    private static HashMap<String, Object> getJobDetails(JobKey jobKey) {
        HashMap<String, Object> jobDetails = new HashMap<>();
        Trigger trigger;
        try {
            if (!SchedulerManager.getQuartzScheduler().checkExists(jobKey)) {
                jobDetails.put("error", "no such campaign exist or schedule for this campaign does not exists. Campaign: " + jobKey.getName());
                return jobDetails;
            }
            // trigger key and job key are exactly same, unless trigger is for chunked execution @See services/jobs/RunCampaign.class
            trigger = SchedulerManager.getQuartzScheduler().getTrigger(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
            if (trigger == null) {
                jobDetails.put("error", "no primary trigger exist for this campaign " + jobKey.getName());
                return jobDetails;
            }
            jobDetails.putAll(SchedulerManager.getQuartzScheduler().getJobDetail(jobKey).getJobDataMap().getWrappedMap());
            // this can be greater than 1
            jobDetails.put(DakiyaStrings.DAKIYA_JOB_NO_OF_TRIGGERS,
                    SchedulerManager.getQuartzScheduler().getTriggersOfJob(jobKey).size());
            trigger = SchedulerManager.getQuartzScheduler()
                    .getTrigger(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
            jobDetails.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_START_TIME,
                    getISTDateTime(trigger.getStartTime()).toString());
            jobDetails.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_END_TIME,
                    getISTDateTime(trigger.getEndTime()).toString());
            jobDetails.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_PREVIOUS_FIRE_TIME,
                    getISTDateTime(trigger.getPreviousFireTime()).toString());
            jobDetails.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_NEXT_FIRE_TIME,
                    getISTDateTime(trigger.getNextFireTime()).toString());
            jobDetails.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_MAY_FIRE_AGAIN, trigger.mayFireAgain());
            return jobDetails;
        } catch (Exception e) {
            jobDetails.put("error", e.getMessage());
            return jobDetails;
        }
    }

    private static Identity getIdentity(final int id) {
        return new Identity(id);
    }

    private static Identity getIdentity(@NonNull final Campaign campaign) {
        return new Identity(campaign.getId());
    }

}

@Getter
class Identity {
    private final String name;
    private String group = DakiyaStrings.DAKIYA_SCHEDULER_CAMPAIGN_JOBS_GROUP;
    Identity(int id) {
        name = Integer.toString(id);
    }

}

