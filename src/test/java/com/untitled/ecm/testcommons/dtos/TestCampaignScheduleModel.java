package com.untitled.ecm.testcommons.dtos;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.joda.time.DateTime;
import org.joda.time.Period;

import javax.validation.constraints.Min;

@Value
@Builder(toBuilder = true)
public class TestCampaignScheduleModel {
    @NonNull
    DateTime startAt;
    @NonNull
    DateTime endAt;
    @NonNull
    Period repeatPeriod;
    @Min(-1)
    int repeatThreshold;
    @Min(1)
    int mailLimit;
}
