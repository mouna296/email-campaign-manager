package com.untitled.ecm.resources.validators;

import com.untitled.ecm.core.DakiyaRuntimeSettings;
import org.apache.commons.lang3.StringUtils;
import org.everit.json.schema.FormatValidator;

import java.util.ArrayList;
import java.util.Optional;

public class DakiyaEmailValidator implements FormatValidator {
    private DakiyaRuntimeSettings dakiyaRuntimeSettings;

    public DakiyaEmailValidator(DakiyaRuntimeSettings dakiyaRuntimeSettings) {
        this.dakiyaRuntimeSettings = dakiyaRuntimeSettings;
    }
    @Override
    public Optional<String> validate(String dakiyaEmail) {
        if (dakiyaRuntimeSettings == null) {
            return Optional.of("could not validate sendgrid domain, internal server error");
        }

        if (StringUtils.countMatches(dakiyaEmail, "@") != 1) {
            return Optional.of("invalid email");
        }

        ArrayList<String> sendgridDomains = this.dakiyaRuntimeSettings.getAllSendGridDomains();
        //"example.com for  testing purposed
        if (dakiyaEmail.endsWith("example.com")) {
            return Optional.empty();
        }
        for (String domain : sendgridDomains) {
            if (dakiyaEmail.toLowerCase().endsWith(domain)) {
                return Optional.empty();
            }
        }
        return Optional.of("invalid email: " + dakiyaEmail + ". Allowed email domains are: " + String.join(",", sendgridDomains));
    }
}
