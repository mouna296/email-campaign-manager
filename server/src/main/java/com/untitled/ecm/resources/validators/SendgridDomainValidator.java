package com.untitled.ecm.resources.validators;

import com.untitled.ecm.core.DakiyaRuntimeSettings;
import org.everit.json.schema.FormatValidator;

import java.util.ArrayList;
import java.util.Optional;

public class SendgridDomainValidator implements FormatValidator {
    private DakiyaRuntimeSettings dakiyaRuntimeSettings;

    public SendgridDomainValidator(DakiyaRuntimeSettings dakiyaRuntimeSettings) {
        this.dakiyaRuntimeSettings = dakiyaRuntimeSettings;
    }

    @Override
    public Optional<String> validate(String domain) {
        if (dakiyaRuntimeSettings == null) {
            return Optional.of("could not validate sendgrid domain, internal server error");
        }
        ArrayList<String> sendgridDomains = this.dakiyaRuntimeSettings.getAllSendGridDomains();
        if (sendgridDomains.contains(domain.toLowerCase())) {
            return Optional.empty();
        } else {
            return Optional.of("Invalid sendgrid domain: " + domain + ". Allowed sendgrid domains are: " + String.join(",", sendgridDomains));
        }
    }
}
