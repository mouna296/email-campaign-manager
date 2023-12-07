package com.untitled.ecm.dtos.http;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CreateUserRequest {
    /**
     * valid roles {@link com.untitled.ecm.constants.Roles}
     */
    // for now everyone will be Roles.Campaign_Manager
    @NotNull
    @Size(min = 3)
    String email;
    @NotNull
    @Size(min = 6)
    String password;
    @NotNull
    @Size(min = 1)
    String firstName;
    @NotNull
    @Size(min = 1)
    String lastName;
}
