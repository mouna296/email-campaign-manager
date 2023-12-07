
package com.untitled.ecm.testcommons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "mail_type",
        "subject",
        "content",
        "content_type",
        "from",
        "reply_to"
})
@Value
@Builder
public class TestMail {

    /**
     * (Required)
     */
    @JsonProperty("mail_type")
    @NotNull
    String mailType;
    /**
     * (Required)
     */
    @JsonProperty("subject")
    @Size(min = 3, max = 500)
    @NotNull
    String subject;
    /**
     * (Required)
     */
    @JsonProperty("content")
    @Size(min = 10)
    @NotNull
    String content;
    /**
     * (Required)
     */
    @JsonProperty("content_type")
    @NotNull
    String contentType = "html";
    /**
     * (Required)
     */
    @JsonProperty("from")
    @Valid
    @NotNull
    EmailAddress from;
    /**
     * (Required)
     */
    @JsonProperty("reply_to")
    @Valid
    @NotNull
    EmailAddress replyTo;

}
