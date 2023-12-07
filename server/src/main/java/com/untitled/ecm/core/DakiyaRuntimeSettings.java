package com.untitled.ecm.core;

import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.constants.EnvironmentType;
import com.untitled.ecm.dao.DakiyaSettingDAO;
import com.untitled.ecm.dtos.DakiyaSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;

public class DakiyaRuntimeSettings {
    private DakiyaSettingDAO dakiyaSettingDAO;
    private Logger logger;

    public DakiyaRuntimeSettings(DakiyaSettingDAO dakiyaSettingDAO) {
        if (dakiyaSettingDAO == null) {
            throw new InstantiationError("dakiyaSettingsDAO was null");
        }
        this.dakiyaSettingDAO = dakiyaSettingDAO;
        this.logger = LoggerFactory.getLogger(DakiyaRuntimeSettings.class);
    }

    private String getDefaultSendGridDomain() {
        return this.getSettingByKey(DakiyaStrings.DEFAULT_SENDGRID_API_KEY_DOMAIN_KEY, "");
    }

    public String getDefaultDomainSendGridAPIKey() {
        Optional<String> apiKey = this.getSendGridAPIKeyByDomain(this.getDefaultSendGridDomain());
        if (apiKey.isPresent()) {
            return apiKey.get();
        } else {
            return null;
        }
    }

    public Optional<String> getSendGridAPIKeyByDomain(String domain) {
        String apiKey = "";
        try {
            apiKey = this.getSettingByKey(DakiyaStrings.sendgridAPIKeyPrefix + DakiyaStrings.sendgridAPIKeyDelimiter + domain);
            if (apiKey.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(apiKey);
            }
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }


    public ArrayList<String> getAllSendGridDomains() {
        List<DakiyaSetting> dakiyaSettings = this.getAllSettings();
        ArrayList<String> sendgridDomains = new ArrayList<>();
        String key;
        for (DakiyaSetting dakiyaSetting : dakiyaSettings) {
            key = dakiyaSetting.getKey();
            if (!key.startsWith(DakiyaStrings.sendgridAPIKeyPrefix)) {
                continue;
            }
            if (key.equals(DakiyaStrings.DEFAULT_SENDGRID_API_KEY_DOMAIN_KEY)) {
                continue;
            }
            String[] temp = key.split(DakiyaStrings.sendgridAPIKeyDelimiter);
            if (temp.length != 2) {
                continue;
            }
            sendgridDomains.add(temp[1].toLowerCase());
        }

        return sendgridDomains;
    }

    public EnvironmentType getEnvType() {
        String val = this.getSettingByKey(DakiyaStrings.DAKIYA_ENVIRONMENT_TYPE_KEY, EnvironmentType.PRODUCTION.toString());
        if (val.equalsIgnoreCase(EnvironmentType.PRODUCTION.toString())) {
            return EnvironmentType.PRODUCTION;
        } else if (val.equalsIgnoreCase(EnvironmentType.TEST.toString())) {
            return EnvironmentType.TEST;
        }
        return EnvironmentType.PRODUCTION;
    }

    public boolean isCreatingCampaignAllowed() {
        return this.string2Boolean(
                this.getSettingByKey(DakiyaStrings.CREATING_CAMPAIGNS_ALLOWED, "no"));
    }

    public boolean isSendingEmailAllowed() {
        return this.string2Boolean(this.getSettingByKey(DakiyaStrings.SENDING_EMAILS_ALLOWED, "no"));
    }

    int getMaxEmailsAllowedPerRecipientPerDay() {
        return this.string2Int(this.getSettingByKey(
                DakiyaStrings.MAX_EMAILS_ALLOWED_PER_RECIPIENT_PER_DAY, "2"));
    }

    public int saveSettingsFromHashMap(HashMap<String, String> settingsHashmap) {

        ArrayList<DakiyaSetting> dakiyaSettings = new ArrayList<DakiyaSetting>();

        for (HashMap.Entry<String, String> pair : settingsHashmap.entrySet()) {
            dakiyaSettings.add(new DakiyaSetting(pair.getKey(), pair.getValue()));
        }
        int[] rows_modified_ = {0};
        int rows_modified = 0;
        try {
            rows_modified_ = this.dakiyaSettingDAO.saveMultipleSettings(dakiyaSettings.iterator());
            rows_modified = IntStream.of(rows_modified_).sum();
            logger.warn(Integer.toString(rows_modified) + " dakiya runtime settings overwritten or created from yaml file");
        } catch (Exception e) {
            logger.error("could not save/update settings in db " + e.getMessage());
        }

        if (settingsHashmap.size() != rows_modified) {
            logger.warn("not all settings were saved in db");
        }

        return rows_modified;

    }

    public List<DakiyaSetting> getAllSettings() {
        return this.dakiyaSettingDAO.getAllSettings();
    }

    public void saveSettingByKey(String key, String value) {
        int row_modified = this.dakiyaSettingDAO.saveSettingByKey(key, value);
        if (row_modified != 1) {
            throw new RuntimeException("setting with key " + key + " was not saved in db");
        }
    }

    private String getSettingByKey(String key, String defaultValue) {
        String value = this.dakiyaSettingDAO.getSettingValueByKey(key);
        if (value != null) {
            return value;
        } else {
            logger.debug("setting by key " + key + " not found");
            return defaultValue;
        }
    }

    public String getSettingByKey(String key) {
        String value = this.dakiyaSettingDAO.getSettingValueByKey(key);
        if (value != null) {
            return value;
        } else {
            logger.debug("setting by key " + key + " not found");
            throw new NoSuchElementException(key + " not found");
        }
    }


    private int string2Int(String str) {
        return Integer.parseInt(str);
    }

    private boolean string2Boolean(String str) {
        str = str.toLowerCase();
        return str.equals("yes") || str.equals("y") || str.equals("true");
    }

    private String boolean2String(boolean flag) {
        // jackson convert yes to true from yaml
        if (flag) {
            return "true";
        } else {
            return "false";
        }
    }
}
