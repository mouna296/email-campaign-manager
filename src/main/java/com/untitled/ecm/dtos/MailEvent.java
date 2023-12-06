package com.untitled.ecm.dtos;

import lombok.Data;

import java.sql.Timestamp;

// one to one mapping with db
@Data
public class MailEvent {
    private Timestamp trigger_time;
    private int campaign_id;
    private int campaign_version;
    private String recipient;
    private boolean mail_sent;
    private String remark;
}
