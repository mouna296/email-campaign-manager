package com.untitled.ecm.resources.validators;

import org.everit.json.schema.FormatValidator;

import java.util.Optional;


public class MailContentTypeValidator implements FormatValidator {

    @Override
    public Optional<String> validate(final String contentType) {
        if (contentType.equals("html")) {
            return Optional.empty();
        } else {
            return Optional.of("invalid content type, allowed strings: \"html\"");
        }
    }
}
