package com.untitled.ecm.services;

import com.untitled.ecm.constants.CampaignStates;
import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.core.DakiyaUser;
import com.untitled.ecm.core.DakiyaUtils;
import com.untitled.ecm.dao.CampaignDAO;
import com.untitled.ecm.dtos.ArchivedCampaign;
import com.untitled.ecm.dtos.ArchivedCampaignWithMail;
import com.untitled.ecm.dtos.Campaign;
import com.untitled.ecm.dtos.CampaignOptionalParams;
import com.untitled.ecm.dtos.http.DeliveryBreakdown;
import com.untitled.ecm.dtos.http.SuccessFailureBreakdown;
import com.untitled.ecm.exceptions.InvalidCampaignJsonException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CampaignService {
    private final MailService mailService;
    private final CampaignDAO campaignDAO;
    private final String dakiyaInstanceType;

    public CampaignService(final CampaignDAO campaignDAO,
                           final MailService mailService,
                           final String dakiyaInstanceType) {
        if (campaignDAO == null || mailService == null) {
            throw new InstantiationError("null value provided");
        }
        this.campaignDAO = campaignDAO;
        this.mailService = mailService;
        this.dakiyaInstanceType = dakiyaInstanceType;
    }

    public List<Campaign> getAllCampaigns() {
        List<Campaign> campaignList = campaignDAO.findAll();
        if (campaignList == null) {
            return new ArrayList<Campaign>();
        }
        assignMail2Campaign(campaignList.iterator());
        return campaignList;
    }

    public List<ArchivedCampaignWithMail> getAllArchivedCampaigns() {
        List<ArchivedCampaign> archivedCampaigns = campaignDAO.findArchivedCampaigns();
        return assignMail2Campaign(archivedCampaigns);
    }

    public Campaign getCampaignById(int id) {
        Campaign campaign;

        campaign = campaignDAO.findByID(id);

        if (campaign == null) {
            throw new NotFoundException("no such campaign exists");
        }
        assignMail2Campaign(campaign);

        return campaign;
    }

    public Campaign createNewCampaign(JSONObject newCampaignJSONObject, DakiyaUser dakiyaUser) {
        JSONObject newMailJSONObject = newCampaignJSONObject.getJSONObject("mail");

        final int mailIDinDB = mailService.saveMailinDB(newMailJSONObject, dakiyaUser.getEmail());

        int campaignId = this.saveCampaignInDB(newCampaignJSONObject, mailIDinDB, dakiyaUser);

        if (campaignId < 0) {
            throw new InternalServerErrorException("internal db error, could not save this campaign in db");
        }

        final String state = CampaignStates.getDefaultStateByRole(dakiyaUser.getRoles());

        if (state.equals(CampaignStates.APPROVED)) {
            changeCampaignStateToApproved(campaignId, dakiyaUser);
        }

        Campaign campaign;
        try {
            campaign = campaignDAO.findByIdAndCreator(campaignId, dakiyaUser.getEmail());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new InternalServerErrorException("db sanity check failed, could not get this campaign from db");
        }

        if (campaign == null) {
            throw new InternalServerErrorException("db sanity check failed, did not find this campaign in db");
        }

        this.assignMail2Campaign(campaign);

        return campaign;
    }

    public Campaign updateCampaign(Campaign oldCampaign, JSONObject newCampaignJSONObject, DakiyaUser dakiyaUser) {
        JSONObject newMailJSONObject = newCampaignJSONObject.getJSONObject("mail");
        int mailIDinDB = mailService.saveMailinDB(newMailJSONObject, dakiyaUser.getEmail());

        int rowsModified = this.updateCampaignInDB(oldCampaign, newCampaignJSONObject, mailIDinDB, dakiyaUser);

        if (rowsModified != 1) {
            throw new InternalServerErrorException("internal db error, could not update campaign");
        }

        final String state = CampaignStates.getDefaultStateByRole(dakiyaUser.getRoles());

        if (state.equals(CampaignStates.APPROVED)) {
            changeCampaignStateToApproved(oldCampaign.getId(), dakiyaUser);
        }

        Campaign newCampaign;
        try {
            newCampaign = campaignDAO.findByIdAndLastModifier(oldCampaign.getId(), dakiyaUser.getEmail());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new InternalServerErrorException("db sanity check failed, could not get this new campaign from db");
        }

        if (newCampaign == null) {
            throw new InternalServerErrorException("db sanity check failed, did not find this new campaign in db");
        }

        this.assignMail2Campaign(newCampaign);

        return newCampaign;
    }

    public void changeCampaignStateToApproved(int id, DakiyaUser dakiyaUser) {
        try {
            int rowsModified = campaignDAO.changeCampaignStateToApproved(id,
                    dakiyaUser.getEmail(),
                    new Timestamp(DateTime.now().withZone(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA)).getMillis()));
            if (rowsModified != 1) {
                log.error("expected rows modified 1, found " + Integer.toString(rowsModified));
                throw new InternalServerErrorException("internal db error, unexpected number of rows modified");
            }
        } catch (Exception e) {
            log.error("internal db error. Causing " + e.getMessage());
            throw new InternalServerErrorException("internal db error, could not approve campaign");
        }
    }

    public List<Integer> getAllCampaignIdsByCategory(String category) {
        final List<Integer> filteredCampaignIdsByCategory = campaignDAO.getCampaignsByCategory(category);
        if (filteredCampaignIdsByCategory == null) {
            return new ArrayList<Integer>();
        }
        return filteredCampaignIdsByCategory;
    }


    public DeliveryBreakdown getDeliveryBreakdown(int campaignID) {
        getCampaignById(campaignID);
        return campaignDAO.getCampaignDeliveryBreakdown(campaignID);
    }

    public SuccessFailureBreakdown getSuccessFailureBreakdown(int campaignID) {
        getCampaignById(campaignID);
        return campaignDAO.getSuccessFailureBreakdown(campaignID);

    }

    private void assignMail2Campaign(Iterator<Campaign> campaignIterator) {
        Campaign campaign;
        while (campaignIterator.hasNext()) {
            campaign = campaignIterator.next();
            campaign.setMail(mailService.getMailById(campaign.getMailIDinDB()));
        }
    }

    private List<ArchivedCampaignWithMail> assignMail2Campaign(final List<ArchivedCampaign> archivedCampaigns) {
        if (CollectionUtils.isEmpty(archivedCampaigns))
            return Collections.emptyList();

        Map<Integer, Integer> idVersionMap = Maps.newHashMap();

        for (ArchivedCampaign archivedCampaign : archivedCampaigns) {
            if (!idVersionMap.containsKey(archivedCampaign.getId())) {
                idVersionMap.put(archivedCampaign.getId(), archivedCampaign.getVersion());
                continue;
            }

            if (idVersionMap.get(archivedCampaign.getId()) < archivedCampaign.getVersion())
                idVersionMap.put(archivedCampaign.getId(), archivedCampaign.getVersion());
        }

        return archivedCampaigns.stream()
                .filter(archivedCampaign -> archivedCampaign.getVersion() == idVersionMap.get(archivedCampaign.getId()))
                .map(archivedCampaign -> new ArchivedCampaignWithMail(archivedCampaign, mailService.getMailById(archivedCampaign.getMail_dbid())))
                .collect(Collectors.toList());
    }

    private void assignMail2Campaign(Campaign campaign) {
        if (campaign == null) {
            return;
        }
        campaign.setMail(mailService.getMailById(campaign.getMailIDinDB()));
    }


    private int saveCampaignInDB(JSONObject campaign, int mailIDinDB, DakiyaUser dakiyaUser) {
        String sql = campaign.getString("sql");
        int version = 0;
        String title = campaign.getString("title");
        String about = campaign.getString("about");
        Timestamp createdOn = new Timestamp(DateTime.now().withZone(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA)).getMillis());
        String state = CampaignStates.NOT_APPROVED;
        String campaignCreator = dakiyaUser.getEmail();
        Timestamp lastModifiedTime = createdOn;
        String lastModifiedBy = dakiyaUser.getEmail();
        DateTime startDateTime = DakiyaUtils.getStartDateTime(campaign.getString("start_at"));
        Timestamp startAt = new Timestamp(startDateTime.getMillis());
        DateTime endDateTime = DateTime.parse(campaign.getString("end_at")).withZone(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA));
        Timestamp endAt = new Timestamp(endDateTime.getMillis());
        String repeatPeriod = campaign.getString("repeat_period");
        String sendgridDomain = campaign.getString("sendgrid_domain");

        CampaignOptionalParams campaignOptionalParams = this.getCampaignOptionalParams(campaign);
        int perUserMailLimit = campaignOptionalParams.getPerUserMailLimit();
        int repeatThreshold = campaignOptionalParams.getCampaignRepeatThreshold();
        String category = campaignOptionalParams.getCampaignCategory();
        int chunkCount = campaignOptionalParams.getChunkCount();
        int mailsPerChunk = campaignOptionalParams.getMailsPerChunk();
        int delayPerChunkInMinutes = campaignOptionalParams.getDelayPerChunkInMinutes();

        try {
            return campaignDAO.saveCampaign(sql, version, title, about, createdOn, state, mailIDinDB,
                    campaignCreator, lastModifiedTime, lastModifiedBy, startAt, endAt, repeatPeriod,
                    repeatThreshold, perUserMailLimit, sendgridDomain, category, dakiyaInstanceType,
                    chunkCount, mailsPerChunk, delayPerChunkInMinutes, 0);
        } catch (Exception e) {
            log.error(e.getMessage());
            return -1;
        }
    }

    private int updateCampaignInDB(Campaign oldCampaign, JSONObject newCampaign, int mailIDinDB, DakiyaUser dakiyaUser) {
        int id = oldCampaign.getId();
        String sql = newCampaign.getString("sql");
        int version = oldCampaign.getVersion() + 1;
        String title = newCampaign.getString("title");
        String about = newCampaign.getString("about");
        Timestamp createdOn = new Timestamp(DateTime.parse(oldCampaign.getCreatedOn()).getMillis());
        String state = CampaignStates.NOT_APPROVED;
        String campaignCreator = oldCampaign.getCampaignCreator();
        Timestamp lastModifiedTime = new Timestamp(DateTime.now().withZone(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA)).getMillis());
        String lastModifiedBy = dakiyaUser.getEmail();
        DateTime startDateTime = DakiyaUtils.getStartDateTime(newCampaign.getString("start_at"));
        Timestamp startAt = new Timestamp(startDateTime.getMillis());
        DateTime endDateTime = DateTime.parse(newCampaign.getString("end_at")).withZone(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA));
        Timestamp endAt = new Timestamp(endDateTime.getMillis());
        String repeatPeriod = newCampaign.getString("repeat_period");
        String sendgridDomain = newCampaign.getString("sendgrid_domain");

        CampaignOptionalParams campaignOptionalParams = this.getCampaignOptionalParams(newCampaign);
        int perUserMailLimit = campaignOptionalParams.getPerUserMailLimit();
        int repeatThreshold = campaignOptionalParams.getCampaignRepeatThreshold();
        String category = campaignOptionalParams.getCampaignCategory();
        int chunkCount = campaignOptionalParams.getChunkCount();
        int mailsPerChunk = campaignOptionalParams.getMailsPerChunk();
        int delayPerChunkInMinutes = campaignOptionalParams.getDelayPerChunkInMinutes();


        try {
            return campaignDAO.updateCampaign(id, sql, version, title, about, createdOn, state, mailIDinDB,
                    campaignCreator, lastModifiedTime, lastModifiedBy, startAt, endAt,
                    repeatPeriod, repeatThreshold, perUserMailLimit, sendgridDomain, category, dakiyaInstanceType,
                    chunkCount, mailsPerChunk, delayPerChunkInMinutes);
        } catch (Exception e) {
            log.error(e.getMessage());
            return -1;
        }
    }

    private CampaignOptionalParams getCampaignOptionalParams(JSONObject campaign) {
        int perUserMailLimit;
        int repeatThreshold;
        String category;
        int chunkCount;
        int mailsPerChunk;
        int delayPerChunkInMinutes;
        try {
            perUserMailLimit = campaign.getInt("mail_limit");
            if (perUserMailLimit < 1) {
                log.warn("mail limit of less than one provided, setting it as 1");
                perUserMailLimit = 1;
            }
        } catch (JSONException e) {
            log.warn(e.getMessage() + " reverting to default value of 2");
            perUserMailLimit = 2;

        }
        try {
            repeatThreshold = campaign.getInt("repeat_threshold");
        } catch (JSONException e) {
            log.warn(e.getMessage() + " reverting to default value of -1");
            repeatThreshold = -1;
        }

        try {
            category = campaign.getString("category");
        } catch (JSONException e) {
            category = "default";
        }

        boolean chunkCountProvided = false;
        try {
            chunkCount = campaign.getInt("chunk_count");
            if (chunkCount < 1 || chunkCount > 200) {
                throw new InvalidCampaignJsonException(Lists.newArrayList("chunk_count should be between 1 and 200"));
            }
            chunkCountProvided = true;
        } catch (JSONException e) {
            chunkCount = 1;
        }

        try {
            mailsPerChunk = campaign.getInt("mails_per_chunk");
            int maxMails = Integer.MAX_VALUE / 2;
            if (mailsPerChunk < 1 || mailsPerChunk > maxMails) {
                throw new InvalidCampaignJsonException(Lists.newArrayList("mails_per_chunk should be between 1 and " + maxMails));
            }
        } catch (JSONException e) {
            if (chunkCountProvided) {
                throw new InvalidCampaignJsonException(Lists.newArrayList("mails_per_chunk must be provided if chunk_count is set"));
            }
            mailsPerChunk = Integer.MAX_VALUE / 2;
        }

        try {
            delayPerChunkInMinutes = campaign.getInt("delay_per_chunk_in_minutes");
            if (delayPerChunkInMinutes < 1 || delayPerChunkInMinutes > 2880) {
                throw new InvalidCampaignJsonException(Lists.newArrayList("delay_per_chunk_in_minutes should be between 1 and 2880"));
            }
        } catch (JSONException e) {
            if (chunkCountProvided) {
                throw new InvalidCampaignJsonException(Lists.newArrayList("delay_per_chunk_in_minutes must be provided if chunk_count is set."));
            }
            delayPerChunkInMinutes = 1;
        }

        return CampaignOptionalParams.builder()
                .perUserMailLimit(perUserMailLimit)
                .campaignRepeatThreshold(repeatThreshold)
                .campaignCategory(category)
                .chunkCount(chunkCount)
                .mailsPerChunk(mailsPerChunk)
                .delayPerChunkInMinutes(delayPerChunkInMinutes)
                .build();
    }


    public boolean archiveCampaign(Campaign campaign, DakiyaUser dakiyaUser) {
        try {
            ArchivedCampaign campaign2bArchived = new ArchivedCampaign(campaign, dakiyaUser);
            campaignDAO.archiveCampaign(campaign2bArchived);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    public void deleteCampaignById(int id) {
        int rowsModified = campaignDAO.deleteCampaignById(id);
        if (rowsModified != 1) {
            log.error("expected rows modified 1, found " + Integer.toString(rowsModified));
            throw new InternalServerErrorException("internal db error, unexpected number of rows modified");
        }
    }
}
