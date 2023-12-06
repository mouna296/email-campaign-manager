package com.untitled.ecm.resources.validators;

import com.untitled.ecm.constants.DakiyaStrings;
import org.everit.json.schema.FormatValidator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.Optional;

public class JodaPeriodFormatValidator implements FormatValidator {

    @Override
    public Optional<String> validate(final String periodString) {

        try {
            Period.parse(periodString);
        } catch (Exception e) {
            return Optional.of(e.getMessage() + " Refer https://en.wikipedia.org/wiki/ISO_8601#Durations for details");
        }

        // currently all campaigns must execute maximum once per 24 hours.
        Period period = Period.parse(periodString);
        Duration duration = period.toDurationFrom(DateTime.now().withZone(DateTimeZone.forID(DakiyaStrings.DAKIYA_TIMEZONE_INDIA)));
        if (duration.getStandardMinutes() < 30) {
            return Optional.of("Provide a value of repeat_period which is greater than 30 minutes");
        } else {
            return Optional.empty();
        }

    }

}
