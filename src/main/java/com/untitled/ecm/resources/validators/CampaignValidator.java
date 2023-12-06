package com.untitled.ecm.resources.validators;

import com.untitled.ecm.core.DakiyaRuntimeSettings;
import com.untitled.ecm.dao.external.RedshiftDao;
import lombok.Getter;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;

public class CampaignValidator {
    private final static String schemaPath = "/schemas/campaign_schema.json";
    private final DakiyaRuntimeSettings dakiyaRuntimeSettings;
    private org.json.JSONObject toBeValidated;
    @Getter
    private ArrayList<String> errorList = new ArrayList<>();
    private boolean validated = false;
    private RedshiftDao redshiftDao;
    private InputStream unvalidatedInputStream;

    public CampaignValidator(InputStream inputStream, RedshiftDao redshiftDao, DakiyaRuntimeSettings dakiyaRuntimeSettings) {
        if (inputStream == null || redshiftDao == null || dakiyaRuntimeSettings == null) {
            throw new InstantiationError("null value provided");
        }
        this.redshiftDao = redshiftDao;
        this.unvalidatedInputStream = inputStream;
        this.dakiyaRuntimeSettings = dakiyaRuntimeSettings;
    }


    public boolean validate() {
        InputStream inputStream = null;
        org.json.JSONObject rawSchema;
        try {
            this.toBeValidated = new org.json.JSONObject(new JSONTokener(this.unvalidatedInputStream));
            inputStream = this.getClass().getResourceAsStream(schemaPath);
            rawSchema = new org.json.JSONObject(new JSONTokener(inputStream));
        } catch (Exception e) {
            errorList.add("Invalid json. Causing: " + e.getMessage());
            return false;
        }

        try {
            SchemaLoader schemaLoader = SchemaLoader.builder()
                    .schemaJson(rawSchema)
                    .addFormatValidator("joda-period", new JodaPeriodFormatValidator())
                    .addFormatValidator("joda-date-time", new JodaDateTimeFormatValidator())
                    .addFormatValidator("mail-content-type", new MailContentTypeValidator())
                    .addFormatValidator("metabase-sql", new MetabaseSQLValidator(this.redshiftDao))
                    .addFormatValidator("dakiya-email", new DakiyaEmailValidator(dakiyaRuntimeSettings))
                    .addFormatValidator("sendgrid-domain", new SendgridDomainValidator(dakiyaRuntimeSettings))
                    .addFormatValidator("mail-type", new MailTypeValidator())
                    .build();
            Schema schema = schemaLoader.load().build();
            schema.validate(toBeValidated);
            TimeDiffValidator timeDiffValidator = new TimeDiffValidator(toBeValidated.getString("start_at"),
                    toBeValidated.getString("end_at"), toBeValidated.getString("repeat_period"));
            Optional<String> timeDiffError = timeDiffValidator.validate();
            if (timeDiffError.isPresent()) {
                errorList.add(timeDiffError.get());
                return false;
            }
            String sendGridDomain = toBeValidated.getString("sendgrid_domain");
            String fromEmail = toBeValidated.getJSONObject("mail").getJSONObject("from").getString("email");
            if (!fromEmail.endsWith(sendGridDomain)) {
                errorList.add("sendgrid domain and email domain must match. Sendgrid domain: " + sendGridDomain + ", from email: " + fromEmail);
                return false;
            }
            this.validated = true;
            return true;
        } catch (ValidationException e) {
            errorList.add(e.getErrorMessage());
            addValidationErrorsToErrorList(e);
            return false;
        }
    }

    private void addValidationErrorsToErrorList(ValidationException e) {
        for (ValidationException validationException : e.getCausingExceptions()) {
            if (validationException.getCausingExceptions().size() > 0) {
                addValidationErrorsToErrorList(validationException);
            } else {
                errorList.add(validationException.getMessage());
            }
        }
    }

    public JSONObject getValidatedJSONObject() {
        if (validated) {
            return this.toBeValidated;
        }
        return null;
    }

}
