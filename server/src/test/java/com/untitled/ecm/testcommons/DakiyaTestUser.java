package com.untitled.ecm.testcommons;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class DakiyaTestUser {
    @NonNull
    String role;
    @NonNull
    String email;
    @NonNull
    String password;
    String firstName = "dakiya";
    String lastName = "test-user";
}
