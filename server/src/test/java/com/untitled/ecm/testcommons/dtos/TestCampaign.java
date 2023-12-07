
package com.untitled.ecm.testcommons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "title",
        "about",
        "sql",
        "start_at",
        "end_at",
        "repeat_period",
        "repeat_threshold",
        "mail_limit",
        "sendgrid_domain",
        "category",
        "mail"
})
@Value
@Builder(toBuilder = true)
public class TestCampaign {

    /**
     * (Required)
     */
    @JsonProperty("title")
    @NotNull
    String title;
    /**
     * (Required)
     */
    @JsonProperty("about")
    @NotNull
    String about;
    /**
     * (Required)
     */
    @JsonProperty("sql")
    @NotNull
    String sql;
    /**
     * (Required)
     */
    @JsonProperty("start_at")
    @NotNull
    String startAt;
    /**
     * (Required)
     */
    @JsonProperty("end_at")
    @NotNull
    String endAt;
    /**
     * (Required)
     */
    @JsonProperty("repeat_period")
    @NotNull
    String repeatPeriod;
    /**
     * (Required)
     */
    @JsonProperty("repeat_threshold")
    @NotNull
    int repeatThreshold;
    @JsonProperty("mail_limit")
    int mailLimit;
    /**
     * (Required)
     */
    @JsonProperty("sendgrid_domain")
    @NotNull
    String sendgridDomain;
    @JsonProperty("category")
    String category;
    /**
     * (Required)
     */
    @JsonProperty("mail")
    @Valid
    @NotNull
    TestMail mail;

}
