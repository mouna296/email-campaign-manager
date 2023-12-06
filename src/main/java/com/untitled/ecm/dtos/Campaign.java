package com.untitled.ecm.dtos;


import com.untitled.ecm.constants.DakiyaStrings;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.sql.Timestamp;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class Campaign {
    private int id;
    private String sql;
    private int version;
    private String about;
    private String title;
    private DateTime createdOn;
    private String campaignCreator;
    private DateTime startAt;
    private DateTime endAt;
    private Period repeatPeriod;
    private int repeatThreshold;
    private DateTime lastModifiedTime;
    private String lastModifiedBy;
    private int mailIDinDB;
    private String state;
    private Dak mail;
    private int mailLimit;
    private String sendgridDomain;
    private String category;
    private String approvedBy;
    private DateTime approvedAt;
    private String dakiyaInstanceType;
    private Integer chunkCount;
    private Integer mailsPerChunk;
    private Integer delayPerChunkInMinutes;

    public String getCreatedOn() {
        return this.createdOn.toString();
    }

    public void setCreatedOn(Timestamp createdOnTimeStamp) {
        this.createdOn = new DateTime(createdOnTimeStamp)
                .withZone(DateTimeZone.forID(DakiyaStrings.DAKIYA_TIMEZONE_INDIA));
    }


    public String getLastModifiedTime() {
        return lastModifiedTime.toString();
    }


    public void setLastModifiedTime(Timestamp lastModifiedTimeTimetamp) {
        this.lastModifiedTime = new DateTime(lastModifiedTimeTimetamp)
                .withZone(DateTimeZone.forID(DakiyaStrings.DAKIYA_TIMEZONE_INDIA));
    }

    public String getStartAt() {
        return startAt.toString();
    }

    public void setStartAt(Timestamp startAtTimestamp) {
        this.startAt = new DateTime(startAtTimestamp)
                .withZone(DateTimeZone.forID(DakiyaStrings.DAKIYA_TIMEZONE_INDIA));
    }

    public String getEndAt() {
        return endAt.toString();
    }

    public void setEndAt(Timestamp endAtTimestamp) {
        this.endAt = new DateTime(endAtTimestamp)
                .withZone(DateTimeZone.forID(DakiyaStrings.DAKIYA_TIMEZONE_INDIA));
    }

    public long getEndAtMillis() {
        return endAt.getMillis();
    }

    public String getRepeatPeriod() {
        return repeatPeriod.toString();
    }

    public void setRepeatPeriod(String repeatDurationString) {
        this.repeatPeriod = Period.parse(repeatDurationString);
    }

    public long getRepeatPeriodMillis() {
        Duration duration = this.repeatPeriod.toDurationFrom(DateTime.now().withZone(DateTimeZone.forID(DakiyaStrings.DAKIYA_TIMEZONE_INDIA)));
        return duration.getMillis();
    }

    public String getApprovedAt() {
        if (this.approvedAt == null) {
            return null;
        }
        return this.approvedAt.toString();
    }

    public void setApprovedAt(Timestamp approvedAt) {
        if (approvedAt == null) {
            this.approvedAt = null;
            return;
        }
        this.approvedAt = new DateTime(approvedAt)
                .withZone(DateTimeZone.forID(DakiyaStrings.DAKIYA_TIMEZONE_INDIA));
    }

}
