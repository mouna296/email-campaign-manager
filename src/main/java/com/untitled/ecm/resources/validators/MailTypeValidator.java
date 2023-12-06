package com.untitled.ecm.resources.validators;

import com.untitled.ecm.constants.MailType;
import org.everit.json.schema.FormatValidator;

import java.util.Optional;

public class MailTypeValidator implements FormatValidator {
    @Override
    public Optional<String> validate(String mailType) {
        if (mailType.equals(MailType.DYNAMIC) || mailType.equals(MailType.STATIC)) {
            return Optional.empty();
        } else {
            return Optional.of("Allowed mail types are: dynamic and static");
        }
    }
}
