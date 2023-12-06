
package com.untitled.ecm.testcommons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "email"
})
@Value(staticConstructor = "of")
public class EmailAddress {

    /**
     * (Required)
     */
    @JsonProperty("name")
    @NotNull
    String name;
    /**
     * (Required)
     */
    @JsonProperty("email")
    @Size(min = 3)
    @NotNull
    String email;

}
