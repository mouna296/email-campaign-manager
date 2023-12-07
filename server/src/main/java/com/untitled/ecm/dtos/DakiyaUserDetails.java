package com.untitled.ecm.dtos;


import lombok.Data;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.sql.Timestamp;

@Data
public class DakiyaUserDetails {
    private String firstName;
    private String lastName;
    private DateTime createdOn;
    private String role;
    private String email;

    public String getCreatedOn() {
        return createdOn.toString();
    }

    public void setCreatedOn(Timestamp createdOnTimestamp) {
        this.createdOn = new DateTime(createdOnTimestamp)
                .withZone(DateTimeZone.forID("Asia/Kolkata"));
    }
}
