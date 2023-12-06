package com.untitled.ecm.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@JsonPropertyOrder({
        "mail_type",
        "subject",
        "content",
        "content_type",
        "from",
        "reply_to"
})
@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class DakV2 {
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
    DakEmail from;
    /**
     * (Required)
     */
    @JsonProperty("reply_to")
    @Valid
    @NotNull
    DakEmail replyTo;
    /**
     * (Required)
     */
    @JsonProperty("creator")
    @Valid
    @NotNull
    String creator;
}
