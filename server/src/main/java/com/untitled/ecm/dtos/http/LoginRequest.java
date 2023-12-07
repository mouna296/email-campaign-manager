package com.untitled.ecm.dtos.http;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotEmpty
    String email;

    @NotEmpty
    String password;
}
