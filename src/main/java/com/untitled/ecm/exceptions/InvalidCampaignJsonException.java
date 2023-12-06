package com.untitled.ecm.exceptions;

import lombok.Getter;

import java.util.List;

public class InvalidCampaignJsonException extends RuntimeException {
    @Getter
    private final List<String> errors;

    public InvalidCampaignJsonException(List<String> errors) {
        this.errors = errors;
    }
}
