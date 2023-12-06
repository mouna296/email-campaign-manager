package com.untitled.ecm.resources.validators;

import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.core.DakiyaUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import java.util.Optional;

public class TimeDiffValidator {
    DateTime startAt;
    DateTime endAT;
    Period period;

    public TimeDiffValidator(String ISO86041_startAt, String ISO86041_endAt, String ISO86041_period) {
        this.startAt = DakiyaUtils.getStartDateTime(ISO86041_startAt);
        this.endAT = DateTime.parse(ISO86041_endAt).withZone(DateTimeZone.forID(DakiyaStrings.DAKIYA_TIMEZONE_INDIA));
        this.period = Period.parse(ISO86041_period);
    }

    public Optional<String> validate() {
        if (this.startAt.isAfter(endAT)) {
            return Optional.of("start time must be before end time. Perceived start time "
                    + startAt.toString() + " and end time " + endAT.toString());
        } else {
            if (this.startAt.plus(this.period).isAfter(endAT)) {
                return Optional.of("start time + repeat period must be before or equal to end time. Perceived start time "
                        + startAt.toString() + " and end time " + endAT.toString() + " and repeat period " + this.period.toString());
            } else {
                return Optional.empty();
            }
        }
    }

}
