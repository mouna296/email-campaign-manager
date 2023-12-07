package com.untitled.ecm.dtos;

import lombok.Data;

import java.sql.Date;

// one to one mapping with db
@Data
public class MailStat {
    private Date date;
    private String recipient;
    private int sent_mail_count;
}
