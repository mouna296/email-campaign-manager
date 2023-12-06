package com.untitled.ecm.dtos;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class CampaignEvent {
    private int campaign_id;
    private Timestamp trigger_time;
    private int version;
    private int mails_filtered;
    private int expected_mails;
    private int mails_sent;
    private String remark;
    private String campaign_last_modified_by;
    private String dakiya_instance_type;
    private int chunk_number;
    private int total_chunk_count;

    public CampaignEvent() {
        this.mails_filtered = 0;
        this.expected_mails = 0;
        this.mails_sent = 0;
    }
}
