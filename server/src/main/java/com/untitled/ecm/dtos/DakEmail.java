package com.untitled.ecm.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "email"
})
public class DakEmail {
    /**
     * (Required)
     */
    @JsonProperty("email")
    @Size(min = 3)
    @NotNull
    private String email;
    /**
     * (Required)
     */
    @JsonProperty("name")
    @Nullable
    private String name;

    public DakEmail() {
    }

    public DakEmail(String email) {
        if (email == null || email.length() == 0) {
            throw new InstantiationError("email cannot be null or empty");
        }
        this.email = email;
    }

    public DakEmail(String name, String email) {
        if (email == null || email.length() == 0) {
            throw new InstantiationError("email cannot be null or empty");
        }
        this.email = email;
        this.name = name;
    }
}
