package com.untitled.ecm.resources.validators;

import org.everit.json.schema.FormatValidator;
import org.joda.time.DateTime;

import java.util.Optional;

public class JodaDateTimeFormatValidator implements FormatValidator {

    @Override
    public Optional<String> validate(final String dateTime) {

        try {
            DateTime.parse(dateTime);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e.getMessage() + " Refer https://en.wikipedia.org/wiki/ISO_8601 for details");
        }
    }
}
